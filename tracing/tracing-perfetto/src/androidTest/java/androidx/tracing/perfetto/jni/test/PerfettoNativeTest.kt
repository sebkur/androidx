/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.tracing.perfetto.jni.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.tracing.perfetto.jni.PerfettoNative
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PerfettoNativeTest {
    companion object {
        init {
            PerfettoNative.loadLib()
        }
        const val libraryVersion = "1.0.0-alpha15" // TODO: get using reflection
    }

    @Test
    fun testEvents() {
        PerfettoNative.nativeRegisterWithPerfetto()
        assertThat(PerfettoNative.nativeVersion()).isEqualTo(libraryVersion)
        assertThat(PerfettoNative.Metadata.version).isEqualTo(libraryVersion)

        PerfettoNative.nativeTraceEventBegin(123, "foo")
        PerfettoNative.nativeTraceEventBegin(321, "bar")
        PerfettoNative.nativeTraceEventEnd()
        PerfettoNative.nativeTraceEventEnd()

        // TODO: verify the content by getting it back from Perfetto
    }
}
