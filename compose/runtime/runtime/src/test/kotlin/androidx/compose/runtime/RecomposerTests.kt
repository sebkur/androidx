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

package androidx.compose.runtime

import androidx.compose.runtime.mock.Linear
import androidx.compose.runtime.mock.TestMonotonicFrameClock
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectNoChanges
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecomposerTests {

    @Test
    fun recomposerRecomposesWhileOpen() = runTest {
        val testClock = TestMonotonicFrameClock(this)
        withContext(testClock) {
            val recomposer = Recomposer(coroutineContext)
            val runner = launch {
                recomposer.runRecomposeAndApplyChanges()
            }
            testScheduler.runCurrent()

            val composition = Composition(UnitApplier(), recomposer)
            var state by mutableStateOf(0)
            var lastRecomposedState = -1
            composition.setContent {
                lastRecomposedState = state
            }
            assertEquals(0, lastRecomposedState, "initial composition")
            Snapshot.withMutableSnapshot { state = 1 }
            assertNotNull(
                withTimeoutOrNull(3_000) { recomposer.awaitIdle() },
                "timed out waiting for recomposer idle for recomposition"
            )
            assertEquals(1, lastRecomposedState, "recomposition")
            recomposer.close()
            assertNotNull(
                withTimeoutOrNull(3_000) { recomposer.join() },
                "timed out waiting for recomposer.join"
            )
            assertNotNull(
                withTimeoutOrNull(3_000) { runner.join() },
                "timed out waiting for recomposer runner job"
            )
            Snapshot.withMutableSnapshot { state = 2 }
            assertNotNull(
                withTimeoutOrNull(3_000) {
                    recomposer.currentState.first { it <= Recomposer.State.PendingWork }
                },
                "timed out waiting for recomposer to not have active pending work"
            )
            assertEquals(1, lastRecomposedState, "expected no recomposition by closed recomposer")
        }
    }

    @Test
    fun recomposerRemainsOpenUntilEffectsJoin() = runTest {
        val testClock = TestMonotonicFrameClock(this)
        withContext(testClock) {
            val recomposer = Recomposer(coroutineContext)
            val runner = launch {
                recomposer.runRecomposeAndApplyChanges()
            }
            val composition = Composition(UnitApplier(), recomposer)
            val completer = Job()
            composition.setContent {
                LaunchedEffect(completer) {
                    completer.join()
                }
            }
            recomposer.awaitIdle()
            recomposer.close()
            recomposer.awaitIdle()
            assertTrue(runner.isActive, "runner is still active")
            completer.complete()
            assertNotNull(
                withTimeoutOrNull(5_000) { recomposer.join() },
                "Expected recomposer join"
            )
            assertEquals(
                Recomposer.State.ShutDown,
                recomposer.currentState.first(),
                "recomposer state"
            )
            assertNotNull(
                withTimeoutOrNull(5_000) { runner.join() },
                "Expected runner join"
            )
        }
    }

    @Test
    fun testRecomposition() = compositionTest {
        val counter = Counter()
        val triggers = mapOf(
            99 to Trigger(),
            100 to Trigger(),
            102 to Trigger(),
        )
        compose {
            RecomposeTestComponentsA(
                counter,
                triggers
            )
        }

        assertEquals(1, counter["A"])
        assertEquals(1, counter["100"])
        assertEquals(1, counter["101"])
        assertEquals(1, counter["102"])

        triggers[100]?.recompose()
        triggers[102]?.recompose()

        // nothing should happen synchronously
        assertEquals(1, counter["A"])
        assertEquals(1, counter["100"])
        assertEquals(1, counter["101"])
        assertEquals(1, counter["102"])

        expectNoChanges()

        assertEquals(1, counter["A"])
        assertEquals(2, counter["100"])
        assertEquals(1, counter["101"])
        assertEquals(2, counter["102"])

        // recompose() both the parent and the child... and show that the child only
        // recomposes once as a result
        triggers[99]?.recompose()
        triggers[102]?.recompose()

        expectNoChanges()

        assertEquals(2, counter["A"])
        assertEquals(3, counter["100"])
        assertEquals(2, counter["101"])
        assertEquals(3, counter["102"])
    }

    @Test // regression b/157111271
    fun testInsertDuringRecomposition() = compositionTest {
        var includeA by mutableStateOf(false)
        var someState by mutableStateOf(0)
        var someOtherState by mutableStateOf(1)

        @Composable fun B(@Suppress("UNUSED_PARAMETER") value: Int) {
            // empty
        }

        @Composable fun A() {
            B(someState)
            someState++
        }

        @Composable fun T() {
            TestSubcomposition {
                // Take up some slot space
                // This makes it more likely to reproduce bug 157111271.
                remember(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15) {
                    1
                }
                if (includeA) {
                    Wrapper {
                        B(0)
                        B(someOtherState)
                        B(2)
                        B(3)
                        B(4)
                        A()
                    }
                }
            }
        }

        compose {
            T()
        }

        includeA = true
        advance(ignorePendingWork = true)

        someOtherState = 10
        advance(ignorePendingWork = true)

        advance(ignorePendingWork = true)
    }

    @Test // regression test for b/161892016
    fun testMultipleRecompose() = compositionTest {
        class A

        var state1 by mutableStateOf(1)
        var state2 by mutableStateOf(1)

        @Composable
        fun validate(a: A?) {
            assertNotNull(a)
        }

        @Composable
        fun use(@Suppress("UNUSED_PARAMETER") i: Int) {
        }

        @Composable
        fun useA(a: A = A()) {
            validate(a)
            use(state2)
        }

        @Composable
        fun test() {
            use(state1)
            useA()
        }

        compose {
            test()
        }

        // Recompose test() skipping useA()
        state1 = 2
        advance()

        state2 = 2
        advance()
        advance()
    }

    @Test
    @OptIn(ExperimentalComposeApi::class)
    fun concurrentRecompositionOffMainThread() = runTest {
        val clock = TestMonotonicFrameClock(this)
        withContext(clock) {
            val recomposer = Recomposer(coroutineContext)
            launch {
                recomposer.runRecomposeConcurrentlyAndApplyChanges(Dispatchers.Default)
            }

            val composition = Composition(UnitApplier(), recomposer)
            val threadLog = Channel<Thread>(Channel.BUFFERED)
            lateinit var recomposeScope: RecomposeScope
            composition.setContent {
                threadLog.trySend(Thread.currentThread())
                val scope = currentRecomposeScope
                SideEffect {
                    recomposeScope = scope
                }
            }

            val firstCompositionThread = threadLog.receive()

            recomposeScope.invalidate()
            testScheduler.advanceUntilIdle()

            val secondCompositionThread = threadLog.receive()
            assertNotEquals(firstCompositionThread, secondCompositionThread)

            recomposer.close()
            testScheduler.advanceUntilIdle()
        }
    }

    @Test
    @OptIn(ExperimentalComposeApi::class)
    fun concurrentRecompositionInvalidationDuringComposition() = runTest {
        val clock = AutoTestFrameClock()
        withContext(coroutineContext + clock) {
            val recomposer = Recomposer(coroutineContext)
            launch {
                recomposer.runRecomposeConcurrentlyAndApplyChanges(Dispatchers.Default)
            }
            testScheduler.runCurrent()

            val composition = Composition(UnitApplier(), recomposer)
            var longRecomposition by mutableStateOf(false)
            val longRecompositionLatch = CountDownLatch(1)
            val applyCount = AtomicInteger(0)
            val recomposeLatch = CountDownLatch(2)
            composition.setContent {
                recomposeLatch.countDown()
                if (longRecomposition) {
                    longRecompositionLatch.await()
                }
                SideEffect {
                    applyCount.incrementAndGet()
                }
            }

            assertEquals(1, applyCount.get(), "applyCount after initial composition")

            Snapshot.withMutableSnapshot {
                longRecomposition = true
            }

            testScheduler.advanceUntilIdle()

            assertTrue(recomposeLatch.await(5, TimeUnit.SECONDS), "recomposeLatch await timed out")
            assertEquals(1, applyCount.get(), "applyCount after starting long recomposition")

            longRecompositionLatch.countDown()
            recomposer.awaitIdle()

            assertEquals(2, applyCount.get(), "applyCount after long recomposition")

            recomposer.close()
        }
    }

    @Test
    @OptIn(ExperimentalComposeApi::class, DelicateCoroutinesApi::class)
    fun concurrentRecompositionOnCompositionSpecificContext() = runTest(AutoTestFrameClock()) {
        val recomposer = Recomposer(coroutineContext)
        launch {
            recomposer.runRecomposeConcurrentlyAndApplyChanges(Dispatchers.Default)
        }

        newSingleThreadContext("specialThreadPool").use { pool ->
            val composition = Composition(UnitApplier(), recomposer, pool)
            var recomposition by mutableStateOf(false)
            val recompositionThread = Channel<Thread>(1)
            composition.setContent {
                if (recomposition) {
                    recompositionThread.trySend(Thread.currentThread())
                }
            }

            Snapshot.withMutableSnapshot {
                recomposition = true
            }
            testScheduler.runCurrent()

            assertTrue(
                withTimeoutOrNull(3_000) {
                    recompositionThread.receive()
                }?.name?.contains("specialThreadPool") == true,
                "recomposition did not occur on expected thread"
            )

            recomposer.close()
        }
    }

    @Test
    @OptIn(ExperimentalComposeApi::class)
    fun compositionRecomposeContextDelegation() {
        val recomposer = Recomposer(EmptyCoroutineContext)
        val parent = Composition(UnitApplier(), recomposer, CoroutineName("testParent"))
        lateinit var child: ControlledComposition
        parent.setContent {
            val parentContext = rememberCompositionContext()
            SideEffect {
                child = ControlledComposition(UnitApplier(), parentContext)
            }
        }

        assertEquals(
            "testParent",
            child.recomposeCoroutineContext[CoroutineName]?.name,
            "child did not inherit parent recomposeCoroutineContext"
        )
    }

    @Test
    fun recomposerCancelReportsShuttingDownImmediately() = runTest(AutoTestFrameClock()) {
        val recomposer = Recomposer(coroutineContext)
        launch(start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }

        // Create a composition with a LaunchedEffect that will need to be resumed for cancellation
        // before the recomposer can fully join.
        Composition(UnitApplier(), recomposer).setContent {
            LaunchedEffect(Unit) {
                awaitCancellation()
            }
        }

        recomposer.cancel()
        // runTest will not dispatch resumed continuations for cancellation yet;
        // read the current state immediately.
        val state = recomposer.currentState.value
        assertTrue(
            state <= Recomposer.State.ShuttingDown,
            "recomposer state $state but expected <= ShuttingDown"
        )
    }
}

private class UnitApplier : Applier<Unit> {
    override val current: Unit
        get() = Unit

    override fun down(node: Unit) {
    }

    override fun up() {
    }

    override fun insertTopDown(index: Int, instance: Unit) {
    }

    override fun insertBottomUp(index: Int, instance: Unit) {
    }

    override fun remove(index: Int, count: Int) {
    }

    override fun move(from: Int, to: Int, count: Int) {
    }

    override fun clear() {
    }
}

class Counter {
    private var counts = mutableMapOf<String, Int>()
    fun inc(key: String) = counts.getOrPut(key, { 0 }).let { counts[key] = it + 1 }
    fun reset() {
        counts = mutableMapOf()
    }

    operator fun get(key: String) = counts[key] ?: 0
}

@Composable
private fun RecomposeTestComponentsA(counter: Counter, triggers: Map<Int, Trigger>) {
    counter.inc("A")
    triggers[99]?.subscribe()
    Linear {
        for (id in 100..102) {
            key(id) {
                RecomposeTestComponentsB(
                    counter,
                    triggers,
                    id
                )
            }
        }
    }
}

@Composable
private fun RecomposeTestComponentsB(
    counter: Counter,
    triggers: Map<Int, Trigger>,
    id: Int = 0
) {
    counter.inc("$id")
    triggers[id]?.subscribe()
    Text("$id")
}

@Composable
private fun Wrapper(content: @Composable () -> Unit) {
    content()
}

private class AutoTestFrameClock : MonotonicFrameClock {
    private val time = AtomicLong(0)

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        return onFrame(time.getAndAdd(16_000_000))
    }
}
