/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.hilt.lifecycle

import androidx.hilt.assisted.toDependencyRequest
import com.google.auto.common.MoreElements
import com.squareup.javapoet.ClassName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

/**
 * Data class that represents a Hilt injected ViewModel
 */
internal data class ViewModelInjectElements(
    val typeElement: TypeElement,
    val constructorElement: ExecutableElement
) {
    val className = ClassName.get(typeElement)

    val factoryClassName = ClassName.get(
        MoreElements.getPackage(typeElement).qualifiedName.toString(),
        "${className.simpleNames().joinToString("_")}_AssistedFactory"
    )

    val moduleClassName = ClassName.get(
        MoreElements.getPackage(typeElement).qualifiedName.toString(),
        "${className.simpleNames().joinToString("_")}_HiltModule"
    )

    val dependencyRequests = constructorElement.parameters.map { constructorArg ->
        constructorArg.toDependencyRequest()
    }
}
