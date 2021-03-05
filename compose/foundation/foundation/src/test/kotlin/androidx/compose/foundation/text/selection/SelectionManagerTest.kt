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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SelectionManagerTest {
    private val selectionRegistrar = spy(SelectionRegistrarImpl())
    private val selectable = FakeSelectable()
    private val selectionManager = SelectionManager(selectionRegistrar)

    private val containerLayoutCoordinates = mock<LayoutCoordinates> {
        on { isAttached } doReturn true
    }
    private val startSelectable = mock<Selectable>()
    private val endSelectable = mock<Selectable>()
    private val middleSelectable = mock<Selectable>()
    private val lastSelectable = mock<Selectable>()

    private val startCoordinates = Offset(3f, 30f)
    private val endCoordinates = Offset(3f, 600f)

    private val fakeSelection =
        Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = 0,
                selectable = startSelectable
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = 5,
                selectable = endSelectable
            )
        )

    private val hapticFeedback = mock<HapticFeedback>()
    private val clipboardManager = mock<ClipboardManager>()
    private val textToolbar = mock<TextToolbar>()

    @Before
    fun setup() {
        selectionRegistrar.subscribe(selectable)
        selectionManager.containerLayoutCoordinates = containerLayoutCoordinates
        selectionManager.hapticFeedBack = hapticFeedback
        selectionManager.clipboardManager = clipboardManager
        selectionManager.textToolbar = textToolbar
    }

    @Test
    fun mergeSelections_sorting() {
        selectionManager.mergeSelections(
            startPosition = startCoordinates,
            endPosition = endCoordinates
        )

        verify(selectionRegistrar, times(1)).sort(containerLayoutCoordinates)
    }

    @Test
    fun mergeSelections_single_selectable_calls_getSelection_once() {
        val fakeNewSelection = mock<Selection>()

        selectable.selectionToReturn = fakeNewSelection

        selectionManager.mergeSelections(
            startPosition = startCoordinates,
            endPosition = endCoordinates,
            previousSelection = fakeSelection
        )

        assertThat(selectable.getSelectionCalledTimes).isEqualTo(1)
        assertThat(selectable.lastStartPosition).isEqualTo(startCoordinates)
        assertThat(selectable.lastEndPosition).isEqualTo(endCoordinates)
        assertThat(selectable.lastContainerLayoutCoordinates)
            .isEqualTo(selectionManager.requireContainerCoordinates())
        assertThat(selectable.lastLongPress).isEqualTo(false)
        assertThat(selectable.lastPreviousSelection).isEqualTo(fakeSelection)

        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun mergeSelections_multiple_selectables_calls_getSelection_multiple_times() {
        val selectable_another = mock<Selectable>()
        selectionRegistrar.subscribe(selectable_another)

        selectionManager.mergeSelections(
            startPosition = startCoordinates,
            endPosition = endCoordinates,
            previousSelection = fakeSelection
        )

        assertThat(selectable.getSelectionCalledTimes).isEqualTo(1)
        assertThat(selectable.lastStartPosition).isEqualTo(startCoordinates)
        assertThat(selectable.lastEndPosition).isEqualTo(endCoordinates)
        assertThat(selectable.lastContainerLayoutCoordinates)
            .isEqualTo(selectionManager.requireContainerCoordinates())
        assertThat(selectable.lastLongPress).isEqualTo(false)
        assertThat(selectable.lastPreviousSelection).isEqualTo(fakeSelection)

        verify(selectable_another, times(1))
            .getSelection(
                startPosition = startCoordinates,
                endPosition = endCoordinates,
                containerLayoutCoordinates = selectionManager.requireContainerCoordinates(),
                longPress = false,
                previousSelection = fakeSelection
            )
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun mergeSelections_selection_does_not_change_hapticFeedBack_Not_triggered() {
        val selection: Selection = mock()
        selectable.selectionToReturn = selection

        selectionManager.mergeSelections(
            startPosition = startCoordinates,
            endPosition = endCoordinates,
            previousSelection = selection
        )

        verify(
            hapticFeedback,
            times(0)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun getSelectedText_selection_null_return_null() {
        selectionManager.selection = null

        assertThat(selectionManager.getSelectedText()).isNull()
        assertThat(selectable.getTextCalledTimes).isEqualTo(0)
    }

    @Test
    fun getSelectedText_not_crossed_single_widget() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('e')
        val endOffset = text.indexOf('m')
        selectable.textToReturn = annotatedString
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectable = selectable
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectable = selectable
            ),
            handlesCrossed = false
        )

        assertThat(selectionManager.getSelectedText())
            .isEqualTo(annotatedString.subSequence(startOffset, endOffset))
        assertThat(selectable.getTextCalledTimes).isEqualTo(1)
    }

    @Test
    fun getSelectedText_crossed_single_widget() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')
        selectable.textToReturn = annotatedString
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectable = selectable
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectable = selectable
            ),
            handlesCrossed = true
        )

        assertThat(selectionManager.getSelectedText())
            .isEqualTo(annotatedString.subSequence(endOffset, startOffset))
        assertThat(selectable.getTextCalledTimes).isEqualTo(1)
    }

    @Test
    fun getSelectedText_not_crossed_multi_widgets() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')

        selectionRegistrar.subscribe(startSelectable)
        selectionRegistrar.subscribe(middleSelectable)
        selectionRegistrar.subscribe(endSelectable)
        selectionRegistrar.subscribe(lastSelectable)
        selectionRegistrar.sorted = true
        whenever(startSelectable.getText()).thenReturn(annotatedString)
        whenever(middleSelectable.getText()).thenReturn(annotatedString)
        whenever(endSelectable.getText()).thenReturn(annotatedString)
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectable = startSelectable
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectable = endSelectable
            ),
            handlesCrossed = false
        )

        val result = annotatedString.subSequence(startOffset, annotatedString.length) +
            annotatedString + annotatedString.subSequence(0, endOffset)
        assertThat(selectionManager.getSelectedText()).isEqualTo(result)
        assertThat(selectable.getTextCalledTimes).isEqualTo(0)
        verify(startSelectable, times(1)).getText()
        verify(middleSelectable, times(1)).getText()
        verify(endSelectable, times(1)).getText()
        verify(lastSelectable, times(0)).getText()
    }

    @Test
    fun getSelectedText_crossed_multi_widgets() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')

        selectionRegistrar.subscribe(endSelectable)
        selectionRegistrar.subscribe(middleSelectable)
        selectionRegistrar.subscribe(startSelectable)
        selectionRegistrar.subscribe(lastSelectable)
        selectionRegistrar.sorted = true
        whenever(startSelectable.getText()).thenReturn(annotatedString)
        whenever(middleSelectable.getText()).thenReturn(annotatedString)
        whenever(endSelectable.getText()).thenReturn(annotatedString)
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectable = startSelectable
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectable = endSelectable
            ),
            handlesCrossed = true
        )

        val result = annotatedString.subSequence(endOffset, annotatedString.length) +
            annotatedString + annotatedString.subSequence(0, startOffset)
        assertThat(selectionManager.getSelectedText()).isEqualTo(result)
        assertThat(selectable.getTextCalledTimes).isEqualTo(0)
        verify(startSelectable, times(1)).getText()
        verify(middleSelectable, times(1)).getText()
        verify(endSelectable, times(1)).getText()
        verify(lastSelectable, times(0)).getText()
    }

    @Test
    fun copy_selection_null_not_trigger_clipboardmanager() {
        selectionManager.selection = null

        selectionManager.copy()

        verify(clipboardManager, times(0)).setText(any())
    }

    @Test
    fun copy_selection_not_null_trigger_clipboardmanager_setText() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')
        selectable.textToReturn = annotatedString
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectable = selectable
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectable = selectable
            ),
            handlesCrossed = true
        )

        selectionManager.copy()

        verify(clipboardManager, times(1)).setText(
            annotatedString.subSequence(
                endOffset,
                startOffset
            )
        )
    }

    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')
        selectable.textToReturn = annotatedString
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectable = selectable
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectable = selectable
            ),
            handlesCrossed = true
        )
        selectionManager.hasFocus = true

        selectionManager.showSelectionToolbar()

        verify(textToolbar, times(1)).showMenu(
            eq(Rect.Zero),
            any(),
            isNull(),
            isNull(),
            isNull()
        )
    }

    @Test
    fun showSelectionToolbar_withoutFocus_notTrigger_textToolbar_showMenu() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')
        selectable.textToReturn = annotatedString
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectable = selectable
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectable = selectable
            ),
            handlesCrossed = true
        )
        selectionManager.hasFocus = false

        selectionManager.showSelectionToolbar()

        verify(textToolbar, never()).showMenu(
            eq(Rect.Zero),
            any(),
            isNull(),
            isNull(),
            isNull()
        )
    }

    @Test
    fun cancel_selection_calls_getSelection_selection_becomes_null() {
        val fakeSelection =
            Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectable = startSelectable
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 5,
                    selectable = endSelectable
                )
            )
        var selection: Selection? = fakeSelection
        val lambda: (Selection?) -> Unit = { selection = it }
        val spyLambda = spy(lambda)
        selectionManager.onSelectionChange = spyLambda
        selectionManager.selection = fakeSelection

        selectionManager.onRelease()

        assertThat(selectable.getSelectionCalledTimes).isEqualTo(1)
        assertThat(selectable.lastStartPosition).isEqualTo(Offset(-1f, -1f))
        assertThat(selectable.lastEndPosition).isEqualTo(Offset(-1f, -1f))
        assertThat(selectable.lastContainerLayoutCoordinates)
            .isEqualTo(selectionManager.requireContainerCoordinates())
        assertThat(selectable.lastLongPress).isEqualTo(false)
        assertThat(selectable.lastPreviousSelection).isEqualTo(fakeSelection)

        assertThat(selection).isNull()
        verify(spyLambda, times(1)).invoke(null)
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun notifySelectableChange_clears_selection() {
        val fakeSelection =
            Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectable = startSelectable
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 5,
                    selectable = endSelectable
                )
            )
        var selection: Selection? = fakeSelection
        val lambda: (Selection?) -> Unit = { selection = it }
        val spyLambda = spy(lambda)
        selectionManager.onSelectionChange = spyLambda
        selectionManager.selection = fakeSelection

        selectionRegistrar.notifySelectableChange(selectable)

        assertThat(selectable.getSelectionCalledTimes).isEqualTo(1)
        assertThat(selectable.lastStartPosition).isEqualTo(Offset(-1f, -1f))
        assertThat(selectable.lastEndPosition).isEqualTo(Offset(-1f, -1f))
        assertThat(selectable.lastContainerLayoutCoordinates)
            .isEqualTo(selectionManager.requireContainerCoordinates())
        assertThat(selectable.lastLongPress).isEqualTo(false)
        assertThat(selectable.lastPreviousSelection).isEqualTo(fakeSelection)

        assertThat(selection).isNull()
        verify(spyLambda, times(1)).invoke(null)
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}
