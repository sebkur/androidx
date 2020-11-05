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

package androidx.car.app.serialization;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An {@link Exception} that notates a failure in serializing/de-serializing to/from a bundle.
 */
public class BundlerException extends Exception {
    public BundlerException(@Nullable String msg, @NonNull Throwable e) {
        super(msg, e);
    }

    public BundlerException(@Nullable String msg) {
        super(msg);
    }
}
