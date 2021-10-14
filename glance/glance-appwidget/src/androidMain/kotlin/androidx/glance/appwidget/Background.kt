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

package androidx.glance.appwidget

import androidx.glance.Modifier
import androidx.glance.appwidget.unit.ColorProvider
import androidx.glance.background
import androidx.glance.unit.Color

/**
 * Apply a background color to the element this modifier is attached to. This will cause the
 * element to paint the specified [Color] as its background, choosing [day] or [night]
 * depending on the device configuration, which will fill the bounds of the element.
 */
public fun Modifier.background(day: Color, night: Color): Modifier =
    background(ColorProvider(day, night))
