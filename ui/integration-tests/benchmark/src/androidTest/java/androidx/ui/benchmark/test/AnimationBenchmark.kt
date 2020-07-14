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

package androidx.ui.benchmark.test

import androidx.animation.AnimationVector1D
import androidx.animation.AnimationVector2D
import androidx.animation.AnimationVector3D
import androidx.animation.AnimationVector4D
import androidx.animation.FastOutSlowInEasing
import androidx.animation.LinearEasing
import androidx.animation.LinearOutSlowInEasing
import androidx.animation.Spring
import androidx.animation.VectorizedKeyframesSpec
import androidx.animation.VectorizedSnapSpec
import androidx.animation.VectorizedSpringSpec
import androidx.animation.VectorizedTweenSpec
import androidx.animation.createAnimation
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class AnimationBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun animationSpec1D() {
        val anim = VectorizedSpringSpec<AnimationVector1D>(1f, Spring.StiffnessLow)
        val start = AnimationVector1D(0f)
        val end = AnimationVector1D(100f)

        benchmarkRule.measureRepeated {
            for (time in 0..1000 step 20) {
                anim.getValue(time.toLong(), start, end, start)
            }
        }
    }

    @Test
    fun animation1D() {
        val anim = VectorizedSpringSpec<AnimationVector1D>(1f, Spring.StiffnessLow)
        val start = AnimationVector1D(0f)
        val end = AnimationVector1D(100f)
        val fixedAnimation = anim.createAnimation(start, end, start)

        benchmarkRule.measureRepeated {
            for (time in 0..1000 step 20) {
                fixedAnimation.getValue(time.toLong())
            }
        }
    }

    @Test
    fun animationSpec2D() {
        val anim = VectorizedTweenSpec<AnimationVector2D>(1000)
        val start = AnimationVector2D(0f, 0f)
        val end = AnimationVector2D(100f, 300f)

        benchmarkRule.measureRepeated {
            for (time in 0..1000 step 20) {
                anim.getValue(time.toLong(), start, end, start)
            }
        }
    }

    @Test
    fun animation2D() {
        val anim = VectorizedTweenSpec<AnimationVector2D>(1000)
        val start = AnimationVector2D(0f, 0f)
        val end = AnimationVector2D(100f, 300f)
        val fixedAnimation = anim.createAnimation(start, end, start)

        benchmarkRule.measureRepeated {
            for (time in 0..1000 step 20) {
                fixedAnimation.getValue(time.toLong())
            }
        }
    }

    @Test
    fun animationSpec3D() {
        val anim = VectorizedSnapSpec<AnimationVector3D>()
        val start = AnimationVector3D(0f, 0f, -500f)
        val end = AnimationVector3D(100f, 300f, 780f)

        benchmarkRule.measureRepeated {
            for (time in 0..1) {
                anim.getValue(time.toLong(), start, end, start)
            }
        }
    }

    @Test
    fun animation3D() {
        val anim = VectorizedSnapSpec<AnimationVector3D>()
        val start = AnimationVector3D(0f, 0f, -500f)
        val end = AnimationVector3D(100f, 300f, 780f)
        val fixedAnimation = anim.createAnimation(start, end, start)

        benchmarkRule.measureRepeated {
            for (time in 0..1) {
                fixedAnimation.getValue(time.toLong())
            }
        }
    }

    @Test
    fun animationSpec4D() {
        val start = AnimationVector4D(0f, 0f, 0f, 0f)
        val end = AnimationVector4D(120f, -50f, 256f, 0f)
        val anim = VectorizedKeyframesSpec<AnimationVector4D>(
            keyframes = mapOf(
                0 to (start to LinearEasing),
                900 to (start to FastOutSlowInEasing), 1000 to (end to LinearOutSlowInEasing)
            ),
            durationMillis = 1000
        )

        benchmarkRule.measureRepeated {
            for (time in 0..1000 step 20) {
                anim.getValue(time.toLong(), start, end, start)
            }
        }
    }

    @Test
    fun animation4D() {
        val start = AnimationVector4D(0f, 0f, 0f, 0f)
        val end = AnimationVector4D(120f, -50f, 256f, 0f)
        val anim = VectorizedKeyframesSpec<AnimationVector4D>(
            keyframes = mapOf(
                0 to (start to LinearEasing),
                900 to (start to FastOutSlowInEasing), 1000 to (end to LinearOutSlowInEasing)
            ),
            durationMillis = 1000
        )
        val fixedAnimation = anim.createAnimation(start, end, start)

        benchmarkRule.measureRepeated {
            for (time in 0..1000 step 20) {
                fixedAnimation.getValue(time.toLong())
            }
        }
    }
}
