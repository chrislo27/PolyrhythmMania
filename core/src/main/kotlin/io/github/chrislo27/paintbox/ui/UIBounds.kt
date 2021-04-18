package io.github.chrislo27.paintbox.ui

import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.ui.area.Bounds
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.area.ReadOnlyBounds
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var


/**
 * Implements the bounds (margin/border/padding/content) system for [UIElement] to inherit from.
 */
open class UIBounds {

    /**
     * The [bounds] is the total area of this [UIBounds], encompassing the margin, border, padding, and content.
     */
    val bounds: Bounds = Bounds(0f, 0f, 0f, 0f)

    /**
     * The margin is a non-rendered, non-interactable area on the outermost area
     * within the [bounds].
     *
     * By definition, the margin zone is equal to [bounds], but there is a [marginZone] property for consistency.
     */
    val margin: Var<Insets> = Var(Insets.ZERO)

    /**
     * The border is an area between the margin and padding. It is rendered separately and not part
     * of each [UIElement]'s rendering zone, but is where input interactability starts.
     *
     * The border zone is the entire rectangular area encompassing the border and inner area, defined by
     * [borderZone].
     */
    val border: Var<Insets> = Var(Insets.ZERO)

    /**
     * The padding is a zone inside the border. The innermost area inside the padding is called the content area.
     *
     * The padding zone is the entire rectangular area encompassing the padding and inner area, defined by
     * [paddingZone].
     */
    val padding: Var<Insets> = Var(Insets.ZERO)

    /**
     * The margin zone is the rectangular area encompassing the [margin] insets and everything else inside of it.
     *
     * By definition, this is exactly equivalent to [bounds], and thus this implementation of [marginZone]
     * will return [bounds].
     * One should not attempt to cast [marginZone] to [Var], use [bounds] if you intend to mutate.
     */
    val marginZone: ReadOnlyBounds get() = this.bounds

    /**
     * The border zone is the rectangular area encompassing the [border] insets and everything else inside of it.
     */
    val borderZone: ReadOnlyBounds = createZoneBounds(marginZone, margin)

    /**
     * The padding zone is the rectangular area encompassing the [padding] insets and everything else inside of it.
     */
    val paddingZone: ReadOnlyBounds = createZoneBounds(borderZone, border)

    /**
     * The content zone is the innermost rectangular area bounded by the [padding] insets, not including padding.
     */
    val contentZone: ReadOnlyBounds = createZoneBounds(paddingZone, padding)

    fun toLocalString(): String {
        val bounds = this.bounds
        return "[x=${bounds.x.getOrCompute()}, y=${bounds.y.getOrCompute()}, w=${bounds.width.getOrCompute()}, h=${bounds.height.getOrCompute()}, margin=${margin.getOrCompute()}, border=${border.getOrCompute()}, padding=${padding.getOrCompute()}, borderZone=${borderZone}, paddingZone=${paddingZone}, contentZone=${contentZone}]"
    }

    override fun toString(): String = toLocalString()
    
    companion object {
        fun createZoneBounds(outerZoneBounds: ReadOnlyBounds, outerInsets: ReadOnlyVar<Insets>): ReadOnlyBounds {
            val xVar: FloatVar = FloatVar {
                val insets = outerInsets.use()
                val minX = outerZoneBounds.x.use() + insets.left
                val maxX = minX + outerZoneBounds.width.use() - insets.right - insets.left
                val width = if (maxX <= minX) 0f else (maxX - minX)
                if (width <= 0f) 
                    ((minX + maxX) / 2f)
                else (minX)
            }
            val yVar: FloatVar = FloatVar {
                val insets = outerInsets.use()
                val minY = outerZoneBounds.y.use() + insets.top
                val maxY = minY + outerZoneBounds.height.use() - insets.bottom - insets.top
                val height = if (maxY <= minY) 0f else (maxY - minY)
                if (height <= 0f)
                    ((minY + maxY) / 2f)
                else (minY)
            }
            val wVar: FloatVar = FloatVar {
                val insets = outerInsets.use()
                val minX = outerZoneBounds.x.use() + insets.left
                val maxX = minX + outerZoneBounds.width.use() - insets.right - insets.left
                if (maxX <= minX) 0f else (maxX - minX)
            }
            val hVar: FloatVar = FloatVar {
                val insets = outerInsets.use()
                val minY = outerZoneBounds.y.use() + insets.top
                val maxY = minY + outerZoneBounds.height.use() - insets.bottom - insets.top
                if (maxY <= minY) 0f else (maxY - minY)
            }
            
            return Bounds(xVar, yVar, wVar, hVar)
        }
    }
}
