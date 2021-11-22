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

package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.AccessibilityControllerImpl
import androidx.compose.ui.platform.DesktopPlatform
import androidx.compose.ui.platform.PlatformComponent
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.density
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoView
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Point
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.FocusEvent
import java.awt.event.InputEvent
import java.awt.event.InputMethodEvent
import java.awt.event.InputMethodListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.MouseWheelEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.im.InputMethodRequests
import javax.accessibility.Accessible
import javax.accessibility.AccessibleContext
import javax.swing.SwingUtilities
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent

internal class ComposeLayer {
    private var isDisposed = false

    // TODO(demin): probably we need to get rid of asynchronous events. it was added because of
    //  slow lazy scroll. But events become unpredictable, and we can't consume them.
    //  Alternative solution to a slow scroll - merge multiple scroll events into a single one.
    private val events = AWTDebounceEventQueue()

    private val _component = ComponentImpl()
    val component: SkiaLayer get() = _component

    internal val scene = ComposeScene(
        Dispatchers.Swing,
        _component,
        Density(1f),
        _component::needRedraw
    )

    private val density get() = _component.density.density

    fun makeAccessible(component: Component) = object : Accessible {
        override fun getAccessibleContext(): AccessibleContext? {
            if (System.getenv("COMPOSE_DISABLE_ACCESSIBILITY") != null) return null
            val controller =
                scene.mainOwner?.accessibilityController as? AccessibilityControllerImpl
            val accessible = controller?.rootAccessible
            accessible?.getAccessibleContext()?.accessibleParent = component.parent as Accessible
            return accessible?.getAccessibleContext()
        }
    }

    private inner class ComponentImpl :
        SkiaLayer(externalAccessibleFactory = ::makeAccessible), Accessible, PlatformComponent {
        var currentInputMethodRequests: InputMethodRequests? = null

        private var window: Window? = null
        private var windowListener = object : WindowFocusListener {
            override fun windowGainedFocus(e: WindowEvent) = refreshWindowFocus()
            override fun windowLostFocus(e: WindowEvent) = refreshWindowFocus()
        }

        override fun addNotify() {
            super.addNotify()
            resetDensity()
            initContent()
            updateSceneSize()
            window = SwingUtilities.getWindowAncestor(this)
            window?.addWindowFocusListener(windowListener)
            refreshWindowFocus()
        }

        override fun removeNotify() {
            window?.removeWindowFocusListener(windowListener)
            window = null
            refreshWindowFocus()
            super.removeNotify()
        }

        override fun paint(g: Graphics) {
            resetDensity()
            super.paint(g)
        }

        override fun getInputMethodRequests() = currentInputMethodRequests
        private var _desiredCursor: Cursor? = null
        override var desiredCursor: Cursor
            get() = _desiredCursor ?: super.getCursor()
            set(value) { _desiredCursor = value }

        override fun commitCursor() {
            super.setCursor(_desiredCursor ?: Cursor(Cursor.DEFAULT_CURSOR))
            _desiredCursor = null
        }

        override fun enableInput(inputMethodRequests: InputMethodRequests) {
            currentInputMethodRequests = inputMethodRequests
            enableInputMethods(true)
            val focusGainedEvent = FocusEvent(this, FocusEvent.FOCUS_GAINED)
            inputContext.dispatchEvent(focusGainedEvent)
        }

        override fun disableInput() {
            currentInputMethodRequests = null
        }

        override fun doLayout() {
            super.doLayout()
            updateSceneSize()
        }

        private fun updateSceneSize() {
            this@ComposeLayer.scene.constraints = Constraints(
                maxWidth = (width * density.density).toInt().coerceAtLeast(0),
                maxHeight = (height * density.density).toInt().coerceAtLeast(0)
            )
        }

        override fun getPreferredSize(): Dimension {
            return if (isPreferredSizeSet) super.getPreferredSize() else Dimension(
                (this@ComposeLayer.scene.contentSize.width / density.density).toInt(),
                (this@ComposeLayer.scene.contentSize.height / density.density).toInt()
            )
        }

        override val locationOnScreen: Point
            @Suppress("ACCIDENTAL_OVERRIDE") // KT-47743
            get() = super.getLocationOnScreen()

        override var density: Density = Density(1f)

        private fun resetDensity() {
            density = (this as SkiaLayer).density
            if (this@ComposeLayer.scene.density != density) {
                this@ComposeLayer.scene.density = density
                updateSceneSize()
            }
        }

        override val windowInfo = WindowInfoImpl()

        private fun refreshWindowFocus() {
            windowInfo.isWindowFocused = window?.isFocused ?: false
        }

        override fun scheduleSyntheticMoveEvent() {
            SwingUtilities.invokeLater {
                val lastMouseEvent = lastMouseEvent ?: return@invokeLater
                val source = lastMouseEvent.source as Component
                source.dispatchEvent(
                    MouseEvent(
                        source,
                        MouseEvent.MOUSE_MOVED,
                        System.nanoTime(),
                        lastMouseEvent.modifiersEx,
                        lastMouseEvent.x,
                        lastMouseEvent.y,
                        0,
                        false
                    )
                )
            }
        }
    }

    init {
        _component.skikoView = object : SkikoView {
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                try {
                    scene.render(canvas, nanoTime)
                } catch (e: Throwable) {
                    if (System.getProperty("compose.desktop.render.ignore.errors") == null) {
                        throw e
                    }
                }
            }
        }

        _component.addInputMethodListener(object : InputMethodListener {
            override fun caretPositionChanged(event: InputMethodEvent?) {
                if (event != null) {
                    scene.onInputMethodEvent(event)
                }
            }

            override fun inputMethodTextChanged(event: InputMethodEvent) = events.post {
                scene.onInputMethodEvent(event)
            }
        })

        _component.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) = Unit
            override fun mousePressed(event: MouseEvent) = onMouseEvent(event)
            override fun mouseReleased(event: MouseEvent) = onMouseEvent(event)
            override fun mouseEntered(event: MouseEvent) = onMouseEvent(event)
            override fun mouseExited(event: MouseEvent) = onMouseEvent(event)
        })
        _component.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(event: MouseEvent) = onMouseEvent(event)
            override fun mouseMoved(event: MouseEvent) = onMouseEvent(event)
        })
        _component.addMouseWheelListener { event ->
            onMouseWheelEvent(event)
        }
        _component.focusTraversalKeysEnabled = false
        _component.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) = scene.sendKeyEvent(event)
            override fun keyReleased(event: KeyEvent) = scene.sendKeyEvent(event)
            override fun keyTyped(event: KeyEvent) = scene.sendKeyEvent(event)
        })
    }

    private var lastMouseEvent: MouseEvent? = null

    private fun onMouseEvent(event: MouseEvent) {
        lastMouseEvent = event
        events.post {
            scene.onMouseEvent(density, event)
        }
    }

    private fun onMouseWheelEvent(event: MouseWheelEvent) {
        lastMouseEvent = event
        events.post {
            scene.onMouseWheelEvent(density, event)
        }
    }

    fun dispose() {
        check(!isDisposed)
        scene.close()
        events.cancel()
        _component.dispose()
        _initContent = null
        isDisposed = true
    }

    var compositionLocalContext: CompositionLocalContext? by scene::compositionLocalContext

    fun setContent(
        onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        onKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        content: @Composable () -> Unit
    ) {
        // If we call it before attaching, everything probably will be fine,
        // but the first composition will be useless, as we set density=1
        // (we don't know the real density if we have unattached component)
        _initContent = {
            scene.setContent(
                onPreviewKeyEvent = onPreviewKeyEvent,
                onKeyEvent = onKeyEvent,
                content = content
            )
        }
        initContent()
    }

    private var _initContent: (() -> Unit)? = null

    private fun initContent() {
        if (_component.isDisplayable) {
            _initContent?.invoke()
            _initContent = null
        }
    }
}

@Suppress("ControlFlowWithEmptyBody")
@OptIn(ExperimentalComposeUiApi::class)
private fun ComposeScene.onMouseEvent(
    density: Float,
    event: MouseEvent
) {
    val eventType = when (event.id) {
        MouseEvent.MOUSE_PRESSED -> PointerEventType.Press
        MouseEvent.MOUSE_RELEASED -> PointerEventType.Release
        MouseEvent.MOUSE_DRAGGED -> PointerEventType.Move
        MouseEvent.MOUSE_MOVED -> PointerEventType.Move
        MouseEvent.MOUSE_ENTERED -> PointerEventType.Enter
        MouseEvent.MOUSE_EXITED -> PointerEventType.Exit
        else -> PointerEventType.Unknown
    }
    sendPointerEvent(
        eventType = eventType,
        position = Offset(event.x.toFloat(), event.y.toFloat()) * density,
        timeMillis = event.`when`,
        type = PointerType.Mouse,
        buttons = event.buttons,
        keyboardModifiers = event.keyboardModifiers,
        nativeEvent = event
    )
}

@Suppress("ControlFlowWithEmptyBody")
@OptIn(ExperimentalComposeUiApi::class)
private fun ComposeScene.onMouseWheelEvent(
    density: Float,
    event: MouseWheelEvent
) {
    sendPointerEvent(
        eventType = PointerEventType.Scroll,
        position = Offset(event.x.toFloat(), event.y.toFloat()) * density,
        scrollDelta = if (event.isShiftDown) {
            Offset(event.preciseWheelRotation.toFloat(), 0f)
        } else {
            Offset(0f, event.preciseWheelRotation.toFloat())
        },
        timeMillis = event.`when`,
        type = PointerType.Mouse,
        buttons = event.buttons,
        keyboardModifiers = event.keyboardModifiers,
        nativeEvent = event
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private fun ComposeScene.sendKeyEvent(event: KeyEvent) {
    sendKeyEvent(ComposeKeyEvent(event))
}

@OptIn(ExperimentalComposeUiApi::class)
private val MouseEvent.buttons get() = PointerButtons(
    isPrimaryPressed = (modifiersEx and MouseEvent.BUTTON1_DOWN_MASK) != 0 && !isMacOsCtrlClick,
    isSecondaryPressed = (modifiersEx and MouseEvent.BUTTON3_DOWN_MASK) != 0 || isMacOsCtrlClick,
    isTertiaryPressed = (modifiersEx and MouseEvent.BUTTON2_DOWN_MASK) != 0,
    isBackPressed = (modifiersEx and MouseEvent.getMaskForButton(4)) != 0,
    isForwardPressed = (modifiersEx and MouseEvent.getMaskForButton(5)) != 0,
)

@OptIn(ExperimentalComposeUiApi::class)
private val MouseEvent.keyboardModifiers get() = PointerKeyboardModifiers(
    isCtrlPressed = (modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0,
    isMetaPressed = (modifiersEx and InputEvent.META_DOWN_MASK) != 0,
    isAltPressed = (modifiersEx and InputEvent.ALT_DOWN_MASK) != 0,
    isShiftPressed = (modifiersEx and InputEvent.SHIFT_DOWN_MASK) != 0,
    isAltGraphPressed = (modifiersEx and InputEvent.ALT_GRAPH_DOWN_MASK) != 0,
    isSymPressed = false,
    isFunctionPressed = false,
    isCapsLockOn = getLockingKeyStateSafe(KeyEvent.VK_CAPS_LOCK),
    isScrollLockOn = getLockingKeyStateSafe(KeyEvent.VK_SCROLL_LOCK),
    isNumLockOn = getLockingKeyStateSafe(KeyEvent.VK_NUM_LOCK),
)

private fun getLockingKeyStateSafe(
    mask: Int
): Boolean = try {
    Toolkit.getDefaultToolkit().getLockingKeyState(mask)
} catch (_: Exception) {
    false
}

private val MouseEvent.isMacOsCtrlClick
    get() = (
            DesktopPlatform.Current == DesktopPlatform.MacOS &&
                    ((modifiersEx and InputEvent.BUTTON1_DOWN_MASK) != 0) &&
                    ((modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0)
            )