/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.text.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.foundation.Text
import androidx.ui.text.ParagraphStyle
import androidx.ui.text.TextStyle
import androidx.ui.text.annotatedString
import androidx.ui.text.style.TextAlign
import androidx.ui.text.style.TextDirection
import androidx.ui.text.style.TextIndent
import androidx.ui.unit.sp

@Sampled
@Composable
fun ParagraphStyleSample() {
    val textStyle = TextStyle(
        textAlign = TextAlign.Justify,
        lineHeight = 20.sp,
        textIndent = TextIndent(firstLine = 14.sp, restLine = 3.sp)
    )
    Text(
        text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
                "incididunt ut labore et dolore magna aliqua.\nUt enim ad minim veniam, quis " +
                "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
        style = textStyle
    )
}

@Sampled
@Composable
fun ParagraphStyleAnnotatedStringsSample() {
    val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
            "incididunt ut labore et dolore magna aliqua.\nUt enim ad minim veniam, quis " +
            "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."

    val paragraphStyle1 = ParagraphStyle(
        textDirection = TextDirection.ContentOrLtr,
        textIndent = TextIndent(firstLine = 14.sp)
    )
    val paragraphStyle2 = ParagraphStyle(
        lineHeight = 30.sp,
        textDirection = TextDirection.ContentOrLtr
    )

    Text(
        text = annotatedString {
            append(text)
            addStyle(paragraphStyle1, 0, text.indexOf('\n') + 1)
            addStyle(paragraphStyle2, text.indexOf('\n') + 1, text.length)
        }
    )
}
