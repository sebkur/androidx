/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation.shape.corner

import androidx.ui.graphics.Outline
import androidx.ui.graphics.Shape
import androidx.ui.unit.Density
import androidx.ui.unit.Px
import androidx.ui.unit.PxSize
import androidx.ui.unit.min
import androidx.ui.unit.minDimension
import androidx.ui.unit.px

/**
 * Base class for [Shape]s defined by four [CornerSize]s.
 *
 * @see RoundedCornerShape for an example of the usage.
 *
 * @param topLeft a size of the top left corner
 * @param topRight a size of the top right corner
 * @param bottomRight a size of the bottom left corner
 * @param bottomLeft a size of the bottom right corner
 */
abstract class CornerBasedShape(
    val topLeft: CornerSize,
    val topRight: CornerSize,
    val bottomRight: CornerSize,
    val bottomLeft: CornerSize
) : Shape {

    final override fun createOutline(size: PxSize, density: Density): Outline {
        val halfMinDimension = size.minDimension / 2f
        val topLeft = min(topLeft.toPx(size, density), halfMinDimension)
        val topRight = min(topRight.toPx(size, density), halfMinDimension)
        val bottomRight = min(bottomRight.toPx(size, density), halfMinDimension)
        val bottomLeft = min(bottomLeft.toPx(size, density), halfMinDimension)
        require(topLeft >= 0.px && topRight >= 0.px && bottomRight >= 0.px && bottomLeft >= 0.px) {
            "Corner size in Px can't be negative(topLeft = $topLeft, topRight = $topRight, " +
                    "bottomRight = $bottomRight, bottomLeft = $bottomLeft)!"
        }
        return createOutline(size, topLeft, topRight, bottomRight, bottomLeft)
    }

    /**
     * Creates [Outline] of this shape for the given [size].
     *
     * @param size the size of the shape boundary.
     * @param topLeft the resolved size of the top left corner
     * @param topRight the resolved size for the top right corner
     * @param bottomRight the resolved size for the bottom left corner
     * @param bottomLeft the resolved size for the bottom right corner
     */
    abstract fun createOutline(
        size: PxSize,
        topLeft: Px,
        topRight: Px,
        bottomRight: Px,
        bottomLeft: Px
    ): Outline

    /**
     * Creates a copy of this Shape with a new corner sizes.
     *
     * @param topLeft a size of the top left corner
     * @param topRight a size of the top right corner
     * @param bottomRight a size of the bottom left corner
     * @param bottomLeft a size of the bottom right corner
     */
    abstract fun copy(
        topLeft: CornerSize = this.topLeft,
        topRight: CornerSize = this.topRight,
        bottomRight: CornerSize = this.bottomRight,
        bottomLeft: CornerSize = this.bottomLeft
    ): CornerBasedShape

    /**
     * Creates a copy of this Shape with a new corner size.
     * @param all a size to apply for all four corners
     */
    fun copy(all: CornerSize): CornerBasedShape = copy(all, all, all, all)

    // Implementations can't be data classes as we defined the abstract copy() method and the data
    // class code generation is not compatible with it, so we provide our hashCode() and equals()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CornerBasedShape

        if (topLeft != other.topLeft) return false
        if (topRight != other.topRight) return false
        if (bottomRight != other.bottomRight) return false
        if (bottomLeft != other.bottomLeft) return false

        return true
    }

    override fun hashCode(): Int {
        var result = topLeft.hashCode()
        result = 31 * result + topRight.hashCode()
        result = 31 * result + bottomRight.hashCode()
        result = 31 * result + bottomLeft.hashCode()
        return result
    }
}
