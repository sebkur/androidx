/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.compose.runtime.internal.ComposableLambda

actual internal fun invokeComposable(composer: Composer, composable: @Composable () -> Unit) {
    composable.unsafeCast<ComposableLambda>().invoke(composer, 1)
}

actual internal fun <T> invokeComposableForResult(
    composer: Composer,
    composable: @Composable () -> T
): T {
    return composable.unsafeCast<Function2<Any, Int, T>>()
        .invoke(composer, 1)
        .unsafeCast<T>()
}