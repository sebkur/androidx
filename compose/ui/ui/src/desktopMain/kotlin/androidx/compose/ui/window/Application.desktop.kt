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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.window

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.configureSwingGlobalsForCompose
import androidx.compose.ui.platform.GlobalSnapshotManager
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.system.exitProcess

/**
 * An entry point for the Compose application. See [awaitApplication] for more information.
 *
 * Usually this entry point is used inside `main()` function:
 * ```
 * fun main() = application {
 *
 * }
 * ```
 *
 * After all windows are closed and all operations are completed, the application will end.
 * Set [exitProcessOnExit] to `false`, if you need to execute some code after [application] block, otherwise the code after it
 * won't be executed, as [application] will exit the process.
 *
 * This entry point is a blocking operation (it blocks the current thread until application
 * finishes) and can't be called inside UI thread. To launch new application from UI thread (for
 * example, from some event listener), use `GlobalScope.launchApplication` instead.
 *
 * Application can launch background tasks using [LaunchedEffect]
 * or create [Window], [Dialog], or [Tray] in a declarative Compose way:
 *
 * ```
 * fun main() = application {
 *     val isSplashScreenShowing by remember { mutableStateOf(true) }
 *
 *     LaunchedEffect(Unit) {
 *         delay(2000)
 *         isSplashScreenShowing = false
 *     }
 *
 *     if (isSplashScreenShowing) {
 *         Window(title = "Splash") {}
 *     } else {
 *         Window(title = "App") {}
 *     }
 * }
 * ```
 *
 * When there is no any active compositions, this function will end.
 * Active composition is a composition that have active coroutine (for example, launched in
 * [LaunchedEffect]) or that have child composition created inside [Window], [Dialog], or [Tray].
 *
 * Don't use any animation in this function
 * (for example, [withFrameNanos] or [androidx.compose.animation.core.animateFloatAsState]),
 * because underlying [MonotonicFrameClock] hasn't synchronized with any display, and produces
 * frames as fast as possible.
 *
 * All animation's should be created inside Composable content of the
 * [Window] / [Dialog] / [ComposePanel].
 *
 * @param exitProcessOnExit should `exitProcess(0)` be called after the application is closed.
 * exitProcess speedup process exit (instant instead of 1-4sec).
 * If `false`, the execution of the function will be unblocked after application is exited
 * (when the last window is closed, and all [LaunchedEffect] are complete).
 * @see [awaitApplication]
 */
fun application(
    exitProcessOnExit: Boolean = true,
    content: @Composable ApplicationScope.() -> Unit
) {
    val configureSwingGlobals = System.getProperty("compose.application.configure.swing.globals") == "true"
    if (configureSwingGlobals) {
        configureSwingGlobalsForCompose()
    }

    runBlocking {
        awaitApplication {
            content()
        }
    }

    if (exitProcessOnExit) {
        exitProcess(0)
    }
}

/**
 * Short variant of launching application inside [CoroutineScope].
 *
 * This function is equivalent of:
 * ```
 * CoroutineScope.launch {
 *     awaitApplication {
 *
 *     }
 * }
 * ```
 *
 * Don't use `GlobalScope.launchApplication {}` to launch application inside `main()` function
 * without waiting it to end.
 * As it will not block the main thread, and application process will stop
 * (because global coroutines are daemon threads, daemon threads don't keep process alive:
 * https://kotlinlang.org/docs/coroutines-basics.html#global-coroutines-are-like-daemon-threads)
 *
 * @see [awaitApplication]
 */
fun CoroutineScope.launchApplication(
    content: @Composable ApplicationScope.() -> Unit
): Job {
    if (System.getProperty("compose.application.configure.swing.globals") == "true") {
        configureSwingGlobalsForCompose()
    }
    return launch {
        awaitApplication(content = content)
    }
}

/**
 * An entry point for the Compose application.
 *
 * Application can launch background tasks using [LaunchedEffect]
 * or create [Window], [Dialog], or [Tray] in a declarative Compose way:
 *
 * ```
 * fun main() = runBlocking {
 *     awaitApplication {
 *         val isSplashScreenShowing by remember { mutableStateOf(true) }
 *
 *         LaunchedEffect(Unit) {
 *             delay(2000)
 *             isSplashScreenShowing = false
 *         }
 *
 *         if (isSplashScreenShowing) {
 *             Window(title = "Splash") {}
 *         } else {
 *             Window(title = "App") {}
 *         }
 *     }
 * }
 * ```
 *
 * When there is no any active compositions, this function will end.
 * Active composition is a composition that have active coroutine (for example, launched in
 * [LaunchedEffect]) or that have child composition created inside [Window], [Dialog], or [Tray].
 *
 * Don't use any animation in this function
 * (for example, [withFrameNanos] or [androidx.compose.animation.core.animateFloatAsState]),
 * because underlying [MonotonicFrameClock] hasn't synchronized with any display, and produces
 * frames as fast as possible.
 *
 * All animation's should be created inside Composable content of the
 * [Window] / [Dialog] / [ComposePanel].
 */
suspend fun awaitApplication(
    content: @Composable ApplicationScope.() -> Unit
) {
    if (System.getProperty("compose.application.configure.swing.globals") == "true") {
        configureSwingGlobalsForCompose()
    }
    withContext(Dispatchers.Swing) {
        withContext(YieldFrameClock) {
            GlobalSnapshotManager.ensureStarted()

            val recomposer = Recomposer(coroutineContext)
            var isOpen by mutableStateOf(true)

            val applicationScope = object : ApplicationScope {
                override fun exitApplication() {
                    isOpen = false
                }
            }

            launch {
                recomposer.runRecomposeAndApplyChanges()
            }

            launch {
                val applier = ApplicationApplier()
                val composition = Composition(applier, recomposer)
                try {
                    composition.setContent {
                        if (isOpen) {
                            CompositionLocalProvider(
                                // Resources which are defined at the application level can use
                                // density to calculate intrinsicSize
                                LocalDensity provides GlobalDensity
                            ) {
                                applicationScope.content()
                            }
                        }
                    }
                    recomposer.close()
                    recomposer.join()
                } finally {
                    composition.dispose()
                }
            }
        }
    }
}

/**
 * Scope used by [application], [awaitApplication], [launchApplication]
 */
interface ApplicationScope {
    /**
     * Close all windows created inside the application and cancel all launched effects
     * (they launch via [LaunchedEffect] adn [rememberCoroutineScope].
     */
    fun exitApplication()
}

private object YieldFrameClock : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(
        onFrame: (frameTimeNanos: Long) -> R
    ): R {
        // We call `yield` to avoid blocking UI thread. If we don't call this then application
        // can be frozen for the user in some cases as it will not receive any input events.
        //
        // Swing dispatcher will process all pending events and resume after `yield`.
        yield()
        return onFrame(System.nanoTime())
    }
}

private class ApplicationApplier : Applier<Unit> {
    override val current: Unit = Unit
    override fun down(node: Unit) = Unit
    override fun up() = Unit
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun clear() = Unit
    override fun onEndChanges() = Unit
}