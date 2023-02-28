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

package androidx.appactions.interaction.service;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import io.grpc.BindableService;

/**
 * Factory for returning a BindableService from AppInteractionService.
 *
 * <p>This class is public because it's used in the testing library.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AppInteractionServiceFactory {

    /**
     * Creates a new instance of the gRPC service from the developer's AppInteractionService
     * (android service)
     */
    @NonNull
    public static BindableService create(@NonNull AppInteractionService appInteractionService) {
        return new AppInteractionServiceGrpcImpl(appInteractionService);
    }

    private AppInteractionServiceFactory() {}
}
