/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.impl.task

import androidx.appactions.interaction.capabilities.core.BaseSession
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.SessionFactory
import androidx.appactions.interaction.capabilities.core.impl.CapabilitySession
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.proto.AppActionsContext.AppAction
import androidx.appactions.interaction.proto.TaskInfo
import java.util.function.Supplier

/**
 * @param id a unique id for this capability, can be null
 * @param actionSpec the ActionSpec for this capability
 * @param sessionFactory the SessionFactory provided by the library user
 * @param sessionBridge a SessionBridge object that converts SessionT into TaskHandler instance
 * @param sessionUpdaterSupplier a Supplier of SessionUpdaterT instances
 */
internal class TaskCapabilityImpl<
    PropertyT,
    ArgumentsT,
    OutputT,
    SessionT : BaseSession<ArgumentsT, OutputT>,
    ConfirmationT,
    SessionUpdaterT,
>
constructor(
    override val id: String,
    private val actionSpec: ActionSpec<PropertyT, ArgumentsT, OutputT>,
    private val property: PropertyT,
    private val sessionFactory: SessionFactory<SessionT>,
    private val sessionBridge: SessionBridge<SessionT, ConfirmationT>,
    private val sessionUpdaterSupplier: Supplier<SessionUpdaterT>,
) : Capability {

    override fun getAppAction(): AppAction {
        return actionSpec
            .convertPropertyToProto(property)
            .toBuilder()
            .setTaskInfo(TaskInfo.newBuilder().setSupportsPartialFulfillment(true))
            .setIdentifier(id)
            .build()
    }

    override fun createSession(
        sessionId: String,
        hostProperties: HostProperties,
    ): CapabilitySession {
        val externalSession =
            sessionFactory.createSession(
                hostProperties,
            )
        return TaskCapabilitySession(
            sessionId,
            actionSpec,
            getAppAction(),
            sessionBridge.createTaskHandler(externalSession),
            externalSession,
        )
    }
}
