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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun ContextMenuArea(
    items: () -> List<ContextMenuItem>,
    state: ContextMenuState = remember { ContextMenuState() },
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val data = ContextMenuData(items, LocalContextMenuData.current)

    ContextMenuDataProvider(data) {
        Box(Modifier.contextMenuDetector(state, enabled), propagateMinConstraints = true) {
            content()
        }
        LocalContextMenuRepresentation.current.Representation(state, data)
    }
}

@Composable
fun ContextMenuDataProvider(
    items: () -> List<ContextMenuItem>,
    content: @Composable () -> Unit
) {
    ContextMenuDataProvider(
        ContextMenuData(items, LocalContextMenuData.current),
        content
    )
}

@Composable
internal fun ContextMenuDataProvider(
    data: ContextMenuData,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalContextMenuData provides data
    ) {
        content()
    }
}

private val LocalContextMenuData = staticCompositionLocalOf<ContextMenuData?> {
    null
}

@OptIn(ExperimentalDesktopApi::class)
private fun Modifier.contextMenuDetector(
    state: ContextMenuState,
    enabled: Boolean = true
): Modifier {
    return if (
        enabled && state.status == ContextMenuState.Status.Closed
    ) {
        this.pointerInput(state) {
            forEachGesture {
                awaitPointerEventScope {
                    val event = awaitEventFirstDown()
                    if (event.buttons.isSecondaryPressed) {
                        event.changes.forEach { it.consumeDownChange() }
                        state.status =
                            ContextMenuState.Status.Open(Rect(event.changes[0].position, 0f))
                    }
                }
            }
        }
    } else {
        Modifier
    }
}

class ContextMenuItem(
    val label: String,
    val onClick: () -> Unit
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ContextMenuItem

        if (label != other.label) return false
        if (onClick != other.onClick) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + onClick.hashCode()
        return result
    }

    override fun toString(): String {
        return "ContextMenuItem(label='$label')"
    }
}

class ContextMenuData(
    val items: () -> List<ContextMenuItem>,
    val next: ContextMenuData?
) {

    val itemsSeq: Sequence<ContextMenuItem>
        get() =
            sequence {
                yieldAll(items())
                next?.let { yieldAll(it.itemsSeq) }
            }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ContextMenuData

        if (items != other.items) return false
        if (next != other.next) return false

        return true
    }

    override fun hashCode(): Int {
        var result = items.hashCode()
        result = 31 * result + (next?.hashCode() ?: 0)
        return result
    }
}

class ContextMenuState {
    sealed class Status {
        class Open(
            val rect: Rect
        ) : Status() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false

                other as Open

                if (rect != other.rect) return false

                return true
            }

            override fun hashCode(): Int {
                return rect.hashCode()
            }

            override fun toString(): String {
                return "Open(rect=$rect)"
            }
        }

        object Closed : Status()
    }

    var status: Status by mutableStateOf(Status.Closed)
}

interface ContextMenuRepresentation {
    @Composable
    fun Representation(state: ContextMenuState, data: ContextMenuData)
}

val LocalContextMenuRepresentation = staticCompositionLocalOf { BasicContextMenuRepresentation }