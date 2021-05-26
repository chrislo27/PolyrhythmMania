package io.github.chrislo27.paintbox.ui.layout

import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.UIElement
import io.github.chrislo27.paintbox.ui.area.Bounds


/**
 * An abstract pane that lays out its children. Use [HBox] and [VBox] for implementations.
 */
abstract class AbstractHVBox : Pane() {

    /**
     * The spacing in between children.
     */
    val spacing: FloatVar = FloatVar(0f)

    /**
     * A flag to disable layouts. When set to true, a layout will be forced.
     * This is useful for pausing layout computation until all children have been added.
     */
    val disableLayouts: Var<Boolean> = Var(false)
    
    init {
        spacing.addListener {
            attemptLayout()
        }
        disableLayouts.addListener {
            if (!it.getOrCompute()) {
                attemptLayout()
            }
        }
    }

    /**
     * Sets [disableLayouts] to true, runs the [func], then sets [disableLayouts] to false.
     * This is intended as an optimization when adding a set of children to avoid constant layout recomputations.
     */
    inline fun temporarilyDisableLayouts(func: () -> Unit) {
        disableLayouts.set(true)
        func()
        disableLayouts.set(false)
    }
    
    protected fun attemptLayout() {
        if (disableLayouts.getOrCompute()) return
        layoutChildrenImpl()
    }
    
    protected abstract fun layoutChildrenImpl()

    override fun onChildAdded(newChild: UIElement) {
        super.onChildAdded(newChild)
        attemptLayout()
    }

    override fun onChildRemoved(oldChild: UIElement) {
        super.onChildRemoved(oldChild)
        attemptLayout()
    }
}

/**
 * A [Pane] that lays out its children from left to right. Children of this [HBox] should expect their
 * [bounds.x][Bounds.x] to be changed.
 */
open class HBox : AbstractHVBox() {
    enum class Align {
        LEFT, CENTRE, RIGHT;
    }
    
    val align: Var<Align> = Var(Align.LEFT)
    
    init {
        align.addListener {
            attemptLayout()
        }
    }
    
    override fun layoutChildrenImpl() {
        val spacing = this.spacing.getOrCompute()
        val align = this.align.getOrCompute()
        val children = this.children.toList()
        
        var accX = 0f
        for (child in children) {
            child.bounds.x.set(accX)
            accX += child.bounds.width.getOrCompute()
            accX += spacing
        }
        if (children.isNotEmpty()) {
            accX -= spacing
        }
        val totalWidth = accX
        val thisWidth = this.contentZone.width.getOrCompute()
        
        val offset: Float = when (align) {
            Align.LEFT -> 0f // Nothing needs to be done as the elements will already be left aligned
            Align.CENTRE -> (thisWidth - totalWidth) / 2f
            Align.RIGHT -> (thisWidth - totalWidth)
        }
        if (offset != 0f) {
            for (child in children) {
                val childX = child.bounds.x.getOrCompute()
                child.bounds.x.set(childX + offset)
            }
        }
    }
}

/**
 * A [Pane] that lays out its children from top to bottom. Children of this [VBox] should expect their
 * [bounds.y][Bounds.y] to be changed.
 */
open class VBox : AbstractHVBox() {
    enum class Align {
        TOP, CENTRE, BOTTOM;
    }

    val align: Var<Align> = Var(Align.TOP)

    init {
        align.addListener {
            attemptLayout()
        }
    }

    override fun layoutChildrenImpl() {
        val spacing = this.spacing.getOrCompute()
        val align = this.align.getOrCompute()
        val children = this.children.toList()

        var accY = 0f
        for (child in children) {
            child.bounds.y.set(accY)
            accY += child.bounds.height.getOrCompute()
            accY += spacing
        }
        if (children.isNotEmpty()) {
            accY -= spacing
        }
        val totalHeight = accY
        val thisHeight = this.contentZone.height.getOrCompute()

        val offset: Float = when (align) {
            Align.TOP -> 0f // Nothing needs to be done as the elements will already be top aligned
            Align.CENTRE -> (thisHeight - totalHeight) / 2f
            Align.BOTTOM -> (thisHeight - totalHeight)
        }
        if (offset != 0f) {
            for (child in children) {
                val childY = child.bounds.y.getOrCompute()
                child.bounds.y.set(childY + offset)
            }
        }
    }
}