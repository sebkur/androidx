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

package androidx.compose.mpp.demo.textfield

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.TextField
import androidx.compose.mpp.demo.Screen
import androidx.compose.mpp.demo.textfield.android.CapitalizationAutoCorrectDemo
import androidx.compose.mpp.demo.textfield.android.KeyboardTypeDemo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val TextFields = Screen.Selection(
    "TextFields",
    Screen.Example("AlmostFullscreen") {
        AlmostFullscreen()
    },
    Screen.Example("Android Capitalization/AutoCorrect") {
        CapitalizationAutoCorrectDemo()
    },
    Screen.Example("Android Keyboard Types") {
        KeyboardTypeDemo()
    },
)

@Composable
private fun AlmostFullscreen() {
    val textState = remember {
        mutableStateOf(
            buildString {
                repeat(100) {
                    appendLine("Text line $it")
                }
            }
        )
    }
    TextField(
        textState.value, { textState.value = it },
        Modifier.fillMaxSize().padding(vertical = 40.dp)
    )
}

