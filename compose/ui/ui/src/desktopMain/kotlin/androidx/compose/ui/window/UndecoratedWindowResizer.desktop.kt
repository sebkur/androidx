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

package androidx.compose.ui.window

import androidx.compose.ui.awt.ComposeLayer
import java.awt.Cursor
import java.awt.Dimension
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener

internal const val DefaultBorderThickness = 8

internal class UndecoratedWindowResizer(
    private val window: Window,
    private val layer: ComposeLayer,
    var enabled: Boolean = false,
    var borderThickness: Int = DefaultBorderThickness
) {
    private var initialPointPos = Point()
    private var initialWindowPos = Point()
    private var initialWindowSize = Dimension()

    private enum class PressedTarget { Sides, Content }

    private val mouseListener = object : MouseAdapter(), MouseMotionListener {
        private var sides = 0
        private var pressedTarget: PressedTarget? = null

        override fun mouseDragged(event: MouseEvent) {
            refresh(event)

            if (pressedTarget == PressedTarget.Sides) {
                resize(sides)
            }
        }

        override fun mouseMoved(event: MouseEvent) = refresh(event)

        override fun mousePressed(event: MouseEvent) {
            initialPointPos = MouseInfo.getPointerInfo().location
            initialWindowPos = Point(window.x, window.y)
            initialWindowSize = Dimension(window.width, window.height)

            refresh(event)

            if (event.button == MouseEvent.BUTTON1) {
                pressedTarget = if (sides != 0) PressedTarget.Sides else PressedTarget.Content
            }
        }

        override fun mouseReleased(event: MouseEvent) {
            if (event.button == MouseEvent.BUTTON1) {
                pressedTarget = null
            }
            refresh(event)
        }

        override fun mouseEntered(event: MouseEvent) = refresh(event)
        override fun mouseExited(event: MouseEvent) = refresh(event)

        private fun refresh(event: MouseEvent) {
            if (!enabled) return

            if (pressedTarget == null) {
                sides = getSides(event.point)
            }

            if (sides != 0) {
                event.consume()
            }

            refreshCursor(sides)
        }
    }

    fun init() {
        layer.component.addMouseListener(mouseListener)
        layer.component.addMouseMotionListener(mouseListener)
    }

    private fun refreshCursor(sides: Int) {
        when (sides) {
            Side.Left.value -> setCursor(Cursor.W_RESIZE_CURSOR)
            Side.Top.value -> setCursor(Cursor.N_RESIZE_CURSOR)
            Side.Right.value -> setCursor(Cursor.E_RESIZE_CURSOR)
            Side.Bottom.value -> setCursor(Cursor.S_RESIZE_CURSOR)
            Corner.LeftTop.value -> setCursor(Cursor.NW_RESIZE_CURSOR)
            Corner.LeftBottom.value -> setCursor(Cursor.SW_RESIZE_CURSOR)
            Corner.RightTop.value -> setCursor(Cursor.NE_RESIZE_CURSOR)
            Corner.RightBottom.value -> setCursor(Cursor.SE_RESIZE_CURSOR)
        }
    }

    private fun setCursor(cursorType: Int) {
        layer.scene.component.desiredCursor = Cursor(cursorType)
    }

    private fun getSides(point: Point): Int {
        var sides = 0
        val tolerance = borderThickness
        if (point.x <= tolerance) {
            sides += Side.Left.value
        }
        if (point.x >= window.width - tolerance) {
            sides += Side.Right.value
        }
        if (point.y <= tolerance) {
            sides += Side.Top.value
        }
        if (point.y >= window.height - tolerance) {
            sides += Side.Bottom.value
        }
        return sides
    }

    private fun resize(sides: Int) {
        val pointPos = MouseInfo.getPointerInfo().location
        val diffX = pointPos.x - initialPointPos.x
        val diffY = pointPos.y - initialPointPos.y
        var newXPos = window.x
        var newYPos = window.y
        var newWidth = window.width
        var newHeight = window.height

        if (contains(sides, Side.Left.value)) {
            newWidth = initialWindowSize.width - diffX
            newWidth = newWidth.coerceAtLeast(window.minimumSize.width)
            newXPos = initialWindowPos.x + initialWindowSize.width - newWidth
        } else if (contains(sides, Side.Right.value)) {
            newWidth = initialWindowSize.width + diffX
        }
        if (contains(sides, Side.Top.value)) {
            newHeight = initialWindowSize.height - diffY
            newHeight = newHeight.coerceAtLeast(window.minimumSize.height)
            newYPos = initialWindowPos.y + initialWindowSize.height - newHeight
        } else if (contains(sides, Side.Bottom.value)) {
            newHeight = initialWindowSize.height + diffY
        }
        window.setLocation(newXPos, newYPos)
        window.setSize(newWidth, newHeight)
    }

    private fun contains(value: Int, other: Int): Boolean {
        if (value and other == other) {
            return true
        }
        return false
    }

    private enum class Side(val value: Int) {
        Left(0x0001),
        Top(0x0010),
        Right(0x0100),
        Bottom(0x1000)
    }

    private enum class Corner(val value: Int) {
        LeftTop(0x0011),
        LeftBottom(0x1001),
        RightTop(0x0110),
        RightBottom(0x1100)
    }
}