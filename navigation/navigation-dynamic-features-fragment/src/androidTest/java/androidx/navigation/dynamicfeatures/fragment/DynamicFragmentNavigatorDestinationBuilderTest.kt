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

package androidx.navigation.dynamicfeatures.fragment

import androidx.fragment.app.FragmentActivity
import androidx.navigation.contains
import androidx.navigation.createGraph
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DynamicFragmentNavigatorDestinationBuilderTest {
    @get:Rule
    val activityRule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private val fragmentManager get() = activityRule.activity.supportFragmentManager

    @UiThreadTest
    @Test fun moduleName() {
        val navHostFragment = DynamicNavHostFragment()
        fragmentManager.beginTransaction()
                .add(android.R.id.content, navHostFragment)
                .commitNow()
        val graph = navHostFragment.createGraph(startDestination = DESTINATION_ID) {
            fragment(DESTINATION_ID, FRAGMENT_CLASS_NAME) {
                moduleName = MODULE_NAME
            }
        }
        assertTrue("Destination should be added to the graph",
                DESTINATION_ID in graph)
    }

    @UiThreadTest
    @Test fun no_moduleName() {
        val navHostFragment = DynamicNavHostFragment()
        fragmentManager.beginTransaction()
                .add(android.R.id.content, navHostFragment)
                .commitNow()
        val graph = navHostFragment.createGraph(startDestination = DESTINATION_ID) {
            fragment(DESTINATION_ID, FRAGMENT_CLASS_NAME) {}
        }
        assertTrue("Destination should be added to the graph",
                DESTINATION_ID in graph)
    }
}

private const val DESTINATION_ID = 1
private const val MODULE_NAME = "module"
private const val FRAGMENT_CLASS_NAME = "androidx.navigation.dynamicfeatures.fragment.TestFragment"

class TestActivity : FragmentActivity()