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

package androidx.ui.integration.test.core.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Box
import androidx.compose.foundation.Text
import androidx.compose.ui.graphics.Color
import androidx.ui.test.ToggleableTestCase
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.ui.test.LayeredComposeTestCase

/**
 * The benchmark test case for [Text], where the input is a plain string.
 */
class TextBasicTestCase(
    private val text: String,
    private val width: Dp,
    private val fontSize: TextUnit
) : LayeredComposeTestCase, ToggleableTestCase {

    private val color = mutableStateOf(Color.Black)

    @Composable
    override fun emitMeasuredContent() {
        Text(text = text, color = color.value, fontSize = fontSize)
    }

    @Composable
    override fun emitContentWrappers(content: @Composable () -> Unit) {
        Box(
            modifier = Modifier.wrapContentSize(Alignment.Center).preferredWidth(width)
        ) {
            content()
        }
    }
    override fun toggleState() {
        if (color.value == Color.Black) {
            color.value = Color.Red
        } else {
            color.value = Color.Black
        }
    }
}
