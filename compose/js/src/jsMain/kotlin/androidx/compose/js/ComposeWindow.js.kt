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

package androidx.compose.js

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext

class ComposeWindow {
    val layer = ComposeLayer()

    fun setContent(
        parentComposition: CompositionContext? = null,
        content: @Composable () -> Unit
    ) {
        println("ComposeWindow.setContent")
        layer.setContent(
            parentComposition = parentComposition,
            content = content
        )
    }
}