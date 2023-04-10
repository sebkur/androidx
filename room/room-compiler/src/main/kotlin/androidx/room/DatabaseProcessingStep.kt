/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XProcessingEnvConfig
import androidx.room.compiler.processing.XProcessingStep
import androidx.room.compiler.processing.XTypeElement
import androidx.room.log.RLog
import androidx.room.processor.Context
import androidx.room.processor.Context.BooleanProcessorOptions.GENERATE_KOTLIN
import androidx.room.processor.DatabaseProcessor
import androidx.room.processor.ProcessorErrors
import androidx.room.util.SchemaFileResolver
import androidx.room.vo.DaoMethod
import androidx.room.vo.Warning
import androidx.room.writer.AutoMigrationWriter
import androidx.room.writer.DaoWriter
import androidx.room.writer.DatabaseWriter
import java.io.File
import java.nio.file.Path

class DatabaseProcessingStep : XProcessingStep {

    override fun annotations(): Set<String> {
        return mutableSetOf(Database::class.qualifiedName!!)
    }

    override fun process(
        env: XProcessingEnv,
        elementsByAnnotation: Map<String, Set<XElement>>,
        isLastRound: Boolean
    ): Set<XTypeElement> {
        check(env.config == getEnvConfig(env.options)) {
            "Room Processor expected ${getEnvConfig(env.options)} " +
                "but was invoked with a different " +
                "configuration: ${env.config}"
        }
        val context = Context(env)

        val rejectedElements = mutableSetOf<XTypeElement>()
        val databases = elementsByAnnotation[Database::class.qualifiedName]
            ?.filterIsInstance<XTypeElement>()
            ?.mapNotNull { annotatedElement ->
                if (isLastRound && !annotatedElement.validate()) {
                    context.reportMissingTypeReference(annotatedElement.qualifiedName)
                    return@mapNotNull null
                }
                val (database, logs) = context.collectLogs { subContext ->
                    DatabaseProcessor(
                        subContext,
                        annotatedElement
                    ).process()
                }
                if (logs.hasMissingTypeErrors()) {
                    if (isLastRound) {
                        // Processing is done yet there are still missing type errors, only report
                        // those and don't generate code for the database class since compilation
                        // will fail anyway.
                        logs.writeTo(context, RLog.MissingTypeErrorFilter)
                        return@mapNotNull null
                    } else {
                        // Abandon processing this database class since it needed a type element
                        // that is missing. It is possible that the type will be generated by a
                        // further annotation processing round, so we will try again by adding
                        // this class element to a deferred set.
                        rejectedElements.add(annotatedElement)
                        return@mapNotNull null
                    }
                } else {
                    logs.writeTo(context)
                    return@mapNotNull database
                }
            }

        val daoMethodsMap = databases?.flatMap { db -> db.daoMethods.map { it to db } }?.toMap()
        daoMethodsMap?.let {
            prepareDaosForWriting(databases, it.keys.toList())
            it.forEach { (daoMethod, db) ->
                DaoWriter(
                    daoMethod.dao,
                    db.element,
                    context.codeLanguage
                ).write(context.processingEnv)
            }
        }

        databases?.forEach { db ->
            DatabaseWriter(db, context.codeLanguage).write(context.processingEnv)
            if (db.exportSchema) {
                val qName = db.element.qualifiedName
                val filename = "${db.version}.json"
                val exportToResources =
                    Context.BooleanProcessorOptions.EXPORT_SCHEMA_RESOURCE.getValue(env)
                val schemaOutFolderPath = context.schemaOutFolderPath
                if (exportToResources) {
                    context.logger.w(ProcessorErrors.EXPORTING_SCHEMA_TO_RESOURCES)
                    val schemaFileOutputStream = env.filer.writeResource(
                        filePath = Path.of("schemas", qName, filename),
                        originatingElements = listOf(db.element)
                    )
                    db.exportSchema(schemaFileOutputStream)
                } else if (schemaOutFolderPath != null) {
                    val schemaOutFolder = SchemaFileResolver.RESOLVER.getFile(
                        Path.of(schemaOutFolderPath)
                    )
                    if (!schemaOutFolder.exists()) {
                        schemaOutFolder.mkdirs()
                    }
                    val dbSchemaFolder = File(schemaOutFolder, qName)
                    if (!dbSchemaFolder.exists()) {
                        dbSchemaFolder.mkdirs()
                    }
                    db.exportSchema(
                        File(dbSchemaFolder, "${db.version}.json")
                    )
                } else {
                    context.logger.w(
                        warning = Warning.MISSING_SCHEMA_LOCATION,
                        element = db.element,
                        msg = ProcessorErrors.MISSING_SCHEMA_EXPORT_DIRECTORY
                    )
                }
            }
            db.autoMigrations.forEach { autoMigration ->
                AutoMigrationWriter(db.element, autoMigration, context.codeLanguage)
                    .write(context.processingEnv)
            }
        }

        return rejectedElements
    }

    /**
     * Traverses all dao methods and assigns them suffix if they are used in multiple databases.
     */
    private fun prepareDaosForWriting(
        databases: List<androidx.room.vo.Database>,
        daoMethods: List<DaoMethod>
    ) {
        daoMethods.groupBy { it.dao.typeName }
            // if used only in 1 database, nothing to do.
            .filter { entry -> entry.value.size > 1 }
            .forEach { entry ->
                entry.value.groupBy { daoMethod ->
                    // first suffix guess: Database's simple name
                    val db = databases.first { db -> db.daoMethods.contains(daoMethod) }
                    db.typeName.simpleNames.last()
                }.forEach { (dbName, methods) ->
                    if (methods.size == 1) {
                        // good, db names do not clash, use db name as suffix
                        methods.first().dao.setSuffix(dbName)
                    } else {
                        // ok looks like a dao is used in 2 different databases both of
                        // which have the same name. enumerate.
                        methods.forEachIndexed { index, method ->
                            method.dao.setSuffix("${dbName}_$index")
                        }
                    }
                }
            }
    }

    companion object {
        internal fun getEnvConfig(options: Map<String, String>) =
            XProcessingEnvConfig.DEFAULT.copy(
                excludeMethodsWithInvalidJvmSourceNames = !GENERATE_KOTLIN.getValue(options)
            )
    }
}