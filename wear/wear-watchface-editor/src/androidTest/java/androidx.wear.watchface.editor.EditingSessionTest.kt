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

package androidx.wear.watchface.editor

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.complications.ComplicationBounds
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationType
import androidx.wear.watchface.CanvasComplication
import androidx.wear.watchface.Complication
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.style.Layer
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`

private const val LEFT_COMPLICATION_ID = 1000
private const val RIGHT_COMPLICATION_ID = 1001
private const val BACKGROUND_COMPLICATION_ID = 1111

/** Trivial "editor" which exposes the EditorSession for testing. */
public class OnWatchFaceEditingTestActivity : ComponentActivity() {
    public lateinit var editorSession: EditorSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editorSession = EditorSession.createOnWatchEditingSession(this, intent!!)!!
    }
}

@RunWith(AndroidJUnit4::class)
@MediumTest
public class EditorSessionTest {
    private val testComponentName = ComponentName("test.package", "test.class")
    private val testInstanceId = "TEST_INSTANCE_ID"
    private var editorDelegate = Mockito.mock(WatchFace.EditorDelegate::class.java)
    private val screenBounds = Rect(0, 0, 400, 400)

    private val redStyleOption =
        UserStyleSetting.ListUserStyleSetting.ListOption("red_style", "Red", icon = null)

    private val greenStyleOption =
        UserStyleSetting.ListUserStyleSetting.ListOption("green_style", "Green", icon = null)

    private val blueStyleOption =
        UserStyleSetting.ListUserStyleSetting.ListOption("bluestyle", "Blue", icon = null)

    private val colorStyleList = listOf(redStyleOption, greenStyleOption, blueStyleOption)

    private val colorStyleSetting = UserStyleSetting.ListUserStyleSetting(
        "color_style_setting",
        "Colors",
        "Watchface colorization", /* icon = */
        null,
        colorStyleList,
        listOf(Layer.BASE_LAYER)
    )

    private val classicStyleOption =
        UserStyleSetting.ListUserStyleSetting.ListOption("classic_style", "Classic", icon = null)

    private val modernStyleOption =
        UserStyleSetting.ListUserStyleSetting.ListOption("modern_style", "Modern", icon = null)

    private val gothicStyleOption =
        UserStyleSetting.ListUserStyleSetting.ListOption("gothic_style", "Gothic", icon = null)

    private val watchHandStyleList =
        listOf(classicStyleOption, modernStyleOption, gothicStyleOption)

    private val watchHandStyleSetting = UserStyleSetting.ListUserStyleSetting(
        "hand_style_setting",
        "Hand Style",
        "Hand visual look", /* icon = */
        null,
        watchHandStyleList,
        listOf(Layer.TOP_LAYER)
    )

    private val mockLeftCanvasComplication = Mockito.mock(CanvasComplication::class.java)
    private val leftComplication =
        Complication.createRoundRectComplicationBuilder(
            LEFT_COMPLICATION_ID,
            mockLeftCanvasComplication,
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.SUNRISE_SUNSET),
            ComplicationBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()

    private val mockRightCanvasComplication = Mockito.mock(CanvasComplication::class.java)
    private val rightComplication =
        Complication.createRoundRectComplicationBuilder(
            RIGHT_COMPLICATION_ID,
            mockRightCanvasComplication,
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.DAY_OF_WEEK),
            ComplicationBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()

    private val mockBackgroundCanvasComplication = Mockito.mock(CanvasComplication::class.java)
    private val backgroundComplication =
        Complication.createBackgroundComplicationBuilder(
            BACKGROUND_COMPLICATION_ID,
            mockBackgroundCanvasComplication,
            emptyList(),
            DefaultComplicationProviderPolicy()
        ).build()

    private fun createOnWatchFaceEditingTestActivity(
        userStyleSettings: List<UserStyleSetting>,
        complications: List<Complication>,
        previewReferenceTimeMillis: Long = 12345
    ): ActivityScenario<OnWatchFaceEditingTestActivity> {
        val userStyleRepository = UserStyleRepository(UserStyleSchema(userStyleSettings))
        val complicationsManager = ComplicationsManager(complications, userStyleRepository)

        WatchFace.registerEditorDelegate(testComponentName, editorDelegate)
        `when`(editorDelegate.complicationsManager).thenReturn(complicationsManager)
        `when`(editorDelegate.userStyleRepository).thenReturn(userStyleRepository)
        `when`(editorDelegate.screenBounds).thenReturn(screenBounds)
        `when`(editorDelegate.previewReferenceTimeMillis).thenReturn(previewReferenceTimeMillis)

        return ActivityScenario.launch(
            WatchFaceEditorContract().createIntent(
                ApplicationProvider.getApplicationContext<Context>(),
                EditorRequest(testComponentName, testInstanceId, null)
            ).apply {
                component = ComponentName(
                    ApplicationProvider.getApplicationContext<Context>(),
                    OnWatchFaceEditingTestActivity::class.java
                )
            }
        )
    }

    @Test
    public fun watchFaceComponentName() {
        val scenario = createOnWatchFaceEditingTestActivity(emptyList(), emptyList())
        scenario.onActivity {
            assertThat(it.editorSession.watchFaceComponentName).isEqualTo(testComponentName)
        }
    }

    @Test
    public fun instanceId() {
        val scenario = createOnWatchFaceEditingTestActivity(emptyList(), emptyList())
        scenario.onActivity {
            assertThat(it.editorSession.instanceId).isEqualTo(testInstanceId)
        }
    }

    @Test
    public fun backgroundComplicationId_noBackgroundComplication() {
        val scenario = createOnWatchFaceEditingTestActivity(emptyList(), emptyList())
        scenario.onActivity {
            assertThat(it.editorSession.backgroundComplicationId).isEqualTo(null)
        }
    }

    @Test
    public fun previewReferenceTimeMillis() {
        val scenario = createOnWatchFaceEditingTestActivity(emptyList(), emptyList(), 54321L)
        scenario.onActivity {
            assertThat(it.editorSession.previewReferenceTimeMillis).isEqualTo(54321L)
        }
    }

    @Test
    public fun userStyleSchema() {
        val scenario = createOnWatchFaceEditingTestActivity(
            listOf(colorStyleSetting, watchHandStyleSetting),
            emptyList()
        )
        scenario.onActivity {
            val userStyleSchema = it.editorSession.userStyleSchema
            assertThat(userStyleSchema.userStyleSettings.size).isEqualTo(2)
            assertThat(userStyleSchema.userStyleSettings[0].id).isEqualTo(colorStyleSetting.id)
            assertThat(userStyleSchema.userStyleSettings[0].options.size)
                .isEqualTo(colorStyleSetting.options.size)
            assertThat(userStyleSchema.userStyleSettings[1].id).isEqualTo(watchHandStyleSetting.id)
            assertThat(userStyleSchema.userStyleSettings[1].options.size)
                .isEqualTo(watchHandStyleSetting.options.size)
            // We could test more state but this should be enough.
        }
    }

    @Test
    public fun backgroundComplicationId() {
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(backgroundComplication)
        )
        scenario.onActivity {
            assertThat(it.editorSession.backgroundComplicationId).isEqualTo(
                BACKGROUND_COMPLICATION_ID
            )
        }
    }

    @Test
    public fun complicationState() {
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication, backgroundComplication)
        )
        scenario.onActivity {
            assertThat(it.editorSession.complicationState.size).isEqualTo(3)
            assertThat(it.editorSession.complicationState[LEFT_COMPLICATION_ID]!!.bounds)
                .isEqualTo(Rect(80, 160, 160, 240))
            assertThat(it.editorSession.complicationState[LEFT_COMPLICATION_ID]!!.boundsType)
                .isEqualTo(ComplicationBoundsType.ROUND_RECT)

            assertThat(it.editorSession.complicationState[RIGHT_COMPLICATION_ID]!!.bounds)
                .isEqualTo(Rect(240, 160, 320, 240))
            assertThat(it.editorSession.complicationState[RIGHT_COMPLICATION_ID]!!.boundsType)
                .isEqualTo(ComplicationBoundsType.ROUND_RECT)

            assertThat(it.editorSession.complicationState[BACKGROUND_COMPLICATION_ID]!!.bounds)
                .isEqualTo(screenBounds)
            assertThat(it.editorSession.complicationState[BACKGROUND_COMPLICATION_ID]!!.boundsType)
                .isEqualTo(ComplicationBoundsType.BACKGROUND)
            // We could test more state but this should be enough.
        }
    }

    @Test
    public fun complicationPreviewData() {
        // TODO(alexclarke): Write a test when we hook up the new preview data fetching API.
    }

    @Test
    public fun launchComplicationProviderChooser() {
        // TODO(alexclarke): Figure out a way of testing this.
    }

    @Test
    public fun getComplicationIdAt() {
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication, backgroundComplication)
        )
        scenario.onActivity {
            assertThat(it.editorSession.getComplicationIdAt(0, 0)).isEqualTo(null)
            assertThat(it.editorSession.getComplicationIdAt(85, 165))
                .isEqualTo(LEFT_COMPLICATION_ID)
            assertThat(it.editorSession.getComplicationIdAt(245, 165))
                .isEqualTo(RIGHT_COMPLICATION_ID)
        }
    }

    @Test
    public fun takeWatchFaceScreenshot() {
        val scenario = createOnWatchFaceEditingTestActivity(emptyList(), emptyList())
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        `when`(
            editorDelegate.takeScreenshot(
                RenderParameters.DEFAULT_INTERACTIVE,
                1234L,
                null
            )
        ).thenReturn(bitmap)

        scenario.onActivity {
            assertThat(
                it.editorSession.takeWatchFaceScreenshot(
                    RenderParameters.DEFAULT_INTERACTIVE,
                    1234L,
                    null
                )
            ).isEqualTo(bitmap)
        }
    }

    @Test
    public fun userStyleInActivityResult() {
        val scenario = createOnWatchFaceEditingTestActivity(
            listOf(colorStyleSetting, watchHandStyleSetting),
            emptyList()
        )
        scenario.onActivity { activity ->
            // Select [blueStyleOption] and [gothicStyleOption].
            val styleMap = activity.editorSession.userStyle.selectedOptions.toMutableMap()
            for (userStyleSetting in activity.editorSession.userStyleSchema.userStyleSettings) {
                styleMap[userStyleSetting] = userStyleSetting.options.last()
            }
            activity.editorSession.userStyle = UserStyle(styleMap)
            activity.setWatchRequestResult(activity.editorSession)
            activity.finish()
        }

        val result = WatchFaceEditorContract().parseResult(
            scenario.result.resultCode,
            scenario.result.resultData
        )

        assertThat(result.userStyle[colorStyleSetting.id]).isEqualTo(blueStyleOption.id)
        assertThat(result.userStyle[watchHandStyleSetting.id]).isEqualTo(gothicStyleOption.id)
    }
}
