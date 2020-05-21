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

package androidx.ui.layout

import androidx.annotation.FloatRange
import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Measured
import androidx.ui.core.Modifier
import androidx.ui.core.VerticalAlignmentLine
import androidx.ui.layout.ColumnScope.alignWithSiblings
import androidx.ui.layout.RowScope.alignWithSiblings
import androidx.ui.unit.IntPx

/**
 * A layout composable that places its children in a vertical sequence. For a layout composable
 * that places its children in a horizontal sequence, see [Row].
 *
 * The layout model is able to assign children heights according to their weights provided
 * using the [ColumnScope.weight] modifier. If a child is not provided a weight, it will be
 * asked for its preferred height before the sizes of the children with weights are calculated
 * proportionally to their weight based on the remaining available space.
 *
 * When none of its children have weights, a [Column] will be as small as possible to fit its
 * children one on top of the other. In order to change the height of the [Column], use the
 * [Modifier.height] modifiers; e.g. to make it fill the available height [Modifier.fillMaxHeight]
 * can be used. If at least one child of a [Column] has a [weight][ColumnScope.weight],
 * the [Column] will fill the available height, so there is no need for [Modifier.fillMaxHeight].
 * However, if [Column]'s size should be limited, the [Modifier.height] or [Modifier.size] layout
 * modifiers should be applied.
 *
 * When the size of the [Column] is larger than the sum of its children sizes, a
 * [verticalArrangement] can be specified to define the positioning of the children inside the
 * [Column]. See [Arrangement] for available positioning behaviors; a custom arrangement can also
 * be defined using the constructor of [Arrangement].
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleColumn
 *
 * @param modifier The modifier to be applied to the Column.
 * @param verticalArrangement The vertical arrangement of the layout's children.
 * @param horizontalGravity The horizontal gravity of the layout's children.
 *
 * @see Column
 */
@Composable
fun Column(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalGravity: Alignment.Horizontal = Alignment.Start,
    children: @Composable ColumnScope.() -> Unit
) {
    RowColumnImpl(
        orientation = LayoutOrientation.Vertical,
        modifier = modifier,
        arrangement = verticalArrangement,
        crossAxisAlignment = horizontalGravity,
        crossAxisSize = SizeMode.Wrap,
        children = { ColumnScope.children() }
    )
}

/**
 * Scope for the children of [Column].
 */
@LayoutScopeMarker
object ColumnScope {
    /**
     * Position the element horizontally within the [Column] according to [align].
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleGravityInColumn
     */
    fun Modifier.gravity(align: Alignment.Horizontal) = this + GravityModifier(align)

    /**
     * Position the element horizontally such that its [alignmentLine] aligns with sibling elements
     * also configured to [alignWithSiblings]. [alignWithSiblings] is a form of [gravity],
     * so both modifiers will not work together if specified for the same layout.
     * Within a [Column], all components with [alignWithSiblings] will align horizontally using
     * the specified [VerticalAlignmentLine]s or values provided using the other
     * [alignWithSiblings] overload, forming a sibling group.
     * At least one element of the sibling group will be placed as it had [Alignment.Start] gravity
     * in [Column], and the alignment of the other siblings will be then determined such that
     * the alignment lines coincide. Note that if only one element in a [Column] has the
     * [alignWithSiblings] modifier specified the element will be positioned
     * as if it had [Alignment.Start] gravity.
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleRelativeToSiblingsInColumn
     */
    fun Modifier.alignWithSiblings(alignmentLine: VerticalAlignmentLine) =
        this + SiblingsAlignedModifier.WithAlignmentLine(alignmentLine)

    /**
     * Size the element's height proportional to its [weight] relative to other weighted sibling
     * elements in the [Column]. The parent will divide the vertical space remaining after measuring
     * unweighted child elements and distribute it according to this weight.
     * When [fill] is true, the element will be forced to occupy the whole height allocated to it.
     * Otherwise, the element is allowed to be smaller - this will result in [Column] being smaller,
     * as the unused allocated height will not be redistributed to other siblings.
     *
     * @sample androidx.ui.layout.samples.SimpleColumn
     */
    fun Modifier.weight(
        @FloatRange(from = 0.0, fromInclusive = false) weight: Float,
        fill: Boolean = true
    ): Modifier {
        require(weight > 0.0) { "invalid weight $weight; must be greater than zero" }
        return this + LayoutWeightImpl(weight, fill)
    }

    /**
     * Position the element horizontally such that the alignment line for the content as
     * determined by [alignmentLineBlock] aligns with sibling elements also configured to
     * [alignWithSiblings]. [alignWithSiblings] is a form of [gravity], so both modifiers
     * will not work together if specified for the same layout.
     * Within a [Column], all components with [alignWithSiblings] will align horizontally using
     * the specified [VerticalAlignmentLine]s or values obtained from [alignmentLineBlock],
     * forming a sibling group.
     * At least one element of the sibling group will be placed as it had [Alignment.Start] gravity
     * in [Column], and the alignment of the other siblings will be then determined such that
     * the alignment lines coincide. Note that if only one element in a [Column] has the
     * [alignWithSiblings] modifier specified the element will be positioned
     * as if it had [Alignment.Start] gravity.
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleRelativeToSiblings
     */
    fun Modifier.alignWithSiblings(
        alignmentLineBlock: (Measured) -> IntPx
    ) = this + SiblingsAlignedModifier.WithAlignmentLineBlock(alignmentLineBlock)
}
