package paintbox.ui.layout

import paintbox.binding.*
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Bounds
import paintbox.ui.area.Insets


/**
 * An abstract pane that lays out its children. Use [HBox] and [VBox] for implementations.
 */
abstract class AbstractHVBox : Pane() {

    /*
    The element data cache stores a particular dimension of an element and the accumulated value. This allows
    for optimal adjustment when elements are added/removed/edited.
    
    When the attemptLayout(index) function is called, all the elements at the given index and
    after have their positions recalculate and updated. If the InternalAlignment is MIDDLE or MAX, every element
    will be updated (not necessarily recalculated).
     */

    enum class InternalAlignment {
        MIN, MIDDLE, MAX
    }
    
    protected inner class ElementData(val element: UIElement, var index: Int, var dimension: Float) {
        var position: Float = 0f
        var nextSpacing: Float = 0f
        
        val sizeListener: VarChangedListener<Float> = VarChangedListener { 
            this@AbstractHVBox.attemptLayout(index)
        }
    }

    protected val elementCache: MutableList<ElementData> = mutableListOf()

    /**
     * The spacing in between children.
     */
    val spacing: FloatVar = FloatVar(0f)

    /**
     * A flag to disable layouts. When set to true, a layout will be forced.
     * This is useful for pausing layout computation until all children have been added.
     */
    val disableLayouts: BooleanVar = BooleanVar(false)
    
    protected val internalAlignment: Var<InternalAlignment> = Var(InternalAlignment.MIN)

    init {
        spacing.addListener {
            attemptLayout(0)
        }
        internalAlignment.addListener {
            attemptLayout(0)
        }
        disableLayouts.addListener {
            if (!it.getOrCompute()) {
                attemptLayout(0)
            }
        }
    }

    /**
     * Returns either the width or height var for this box from [contentZone].
     */
    protected abstract fun getThisDimensional(): ReadOnlyFloatVar
    
    /**
     * Returns either the width or height var for an element.
     */
    protected abstract fun getDimensional(element: UIElement): FloatVar

    /**
     * Returns either the x or y var for an element.
     */
    protected abstract fun getPositional(element: UIElement): FloatVar

    /**
     * Sets [disableLayouts] to true, runs the [func], then sets [disableLayouts] to false.
     * This is intended as an optimization when adding a set of children to avoid constant layout recomputations.
     */
    inline fun temporarilyDisableLayouts(func: () -> Unit) {
        disableLayouts.set(true)
        func()
        disableLayouts.set(false)
    }

    protected fun attemptLayout(index: Int) {
        if (disableLayouts.get() || index >= elementCache.size) return
        
        val cache = elementCache
        var acc = if (index > 0) (cache[index - 1].let { it.position + it.dimension + it.nextSpacing }) else 0f
        val cacheSize = cache.size
        val spacingValue = spacing.get()
        
        for (i in index until cacheSize) {
            val d = cache[i]
            val element = d.element
            d.position = acc
            d.dimension = getDimensional(element).get()
            d.nextSpacing = spacingValue
            
            val pos = getPositional(element)
            pos.set(d.position)
            
            acc += d.dimension
            if (i < cacheSize - 1) {
                acc += d.nextSpacing
            }
        }
        
        // Alignment
        val align = this.internalAlignment.getOrCompute()
        if (align != InternalAlignment.MIN) {
            val totalSize = cache.last().let { it.position + it.dimension }
            val thisSize = getThisDimensional().get()
            val offset: Float = when (align) {
                InternalAlignment.MIN -> 0f // Not a possible branch
                InternalAlignment.MIDDLE -> (thisSize - totalSize) / 2f
                InternalAlignment.MAX -> (thisSize - totalSize)
            }

            for (i in 0 until cacheSize) {
                val d = elementCache[i]
                val element = d.element
                val pos = getPositional(element)
                pos.set(d.position + offset)
            }
        }
    }

    override fun onChildAdded(newChild: UIElement) {
        super.onChildAdded(newChild)

        // Add to end of cache.
        val currentCache = elementCache.toList()
        val prev = currentCache.lastOrNull()
        val dimensional = getDimensional(newChild)
        val elementData = ElementData(newChild, currentCache.size, dimensional.get())
        dimensional.addListener(elementData.sizeListener)
        elementCache += elementData
        attemptLayout((currentCache.size - 1).coerceAtLeast(0))
    }

    override fun onChildRemoved(oldChild: UIElement) {
        super.onChildRemoved(oldChild)

        // Find where in the cache it was deleted and update the subsequent ones
        val currentCache = elementCache.toList()
        val index = currentCache.indexOfFirst { it.element == oldChild }
        if (index < 0) return
        
        val removedData = elementCache.removeAt(index)
        getDimensional(oldChild).removeListener(removedData.sizeListener)
        
        for (i in (index + 1) until currentCache.size) {
            currentCache[i].index = i - 1
        }
        attemptLayout(index)
    }
}

/**
 * A [Pane] that lays out its children from left to right. Children of this [HBox] should expect their
 * [bounds.x][Bounds.x] to be changed, and should NOT have their width depend on their own x.
 */
open class HBox : AbstractHVBox() {
    
    enum class Align(val internal: InternalAlignment) {
        LEFT(InternalAlignment.MIN), CENTRE(InternalAlignment.MIDDLE), RIGHT(InternalAlignment.MAX);
    }

    val align: Var<Align> = Var(Align.LEFT)

    init {
        this.internalAlignment.bind { 
            align.use().internal
        }
        this.bounds.width.addListener {
            attemptLayout(0)
        }
    }

    override fun getDimensional(element: UIElement): FloatVar {
        return element.bounds.width
    }

    override fun getPositional(element: UIElement): FloatVar {
        return element.bounds.x
    }

    override fun getThisDimensional(): ReadOnlyFloatVar {
        return this.contentZone.width
    }

    fun sizeWidthToChildren(minimumWidth: Float = 0f) {
        val last = children.lastOrNull()
        var width = 0f
        if (last != null) {
            width = last.bounds.x.get() + last.bounds.width.get()
        }
        
        val borderInsets = this.border.getOrCompute()
        val marginInsets = this.margin.getOrCompute()
        val paddingInsets = this.padding.getOrCompute()
        fun Insets.leftright(): Float = this.left + this.right

        width += borderInsets.leftright() + marginInsets.leftright() + paddingInsets.leftright()


        this.bounds.width.set(width.coerceAtLeast(minimumWidth))
    }
}

/**
 * A [Pane] that lays out its children from top to bottom. Children of this [VBox] should expect their
 * [bounds.y][Bounds.y] to be changed, and should NOT have their height depend on their own y.
 */
open class VBox : AbstractHVBox() {
    
    enum class Align(val internal: InternalAlignment) {
        TOP(InternalAlignment.MIN), CENTRE(InternalAlignment.MIDDLE), BOTTOM(InternalAlignment.MAX);
    }

    val align: Var<Align> = Var(Align.TOP)

    init {
        this.internalAlignment.bind {
            align.use().internal
        }
        this.bounds.height.addListener {
            attemptLayout(0)
        }
    }

    override fun getDimensional(element: UIElement): FloatVar {
        return element.bounds.height
    }

    override fun getPositional(element: UIElement): FloatVar {
        return element.bounds.y
    }

    override fun getThisDimensional(): ReadOnlyFloatVar {
        return this.contentZone.height
    }
    
    fun sizeHeightToChildren(minimumHeight: Float = 0f) {
        val last = children.lastOrNull()
        var height = 0f
        if (last != null) {
            height = last.bounds.y.get() + last.bounds.height.get()
        }

        val borderInsets = this.border.getOrCompute()
        val marginInsets = this.margin.getOrCompute()
        val paddingInsets = this.padding.getOrCompute()
        fun Insets.topbottom(): Float = this.top + this.bottom
        
        height += borderInsets.topbottom() + marginInsets.topbottom() + paddingInsets.topbottom()
        
        this.bounds.height.set(height.coerceAtLeast(minimumHeight))
    }
}