package io.github.chrislo27.paintbox.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.ui.area.ReadOnlyBounds
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var


open class UIElement : UIBounds() {

    val parent: Var<UIElement?> = Var(null)
    
    var children: List<UIElement> = emptyList()
        private set
    
    val inputListeners: Var<List<InputEventListener>> = Var(emptyList())
    val sceneRoot: ReadOnlyVar<SceneRoot?> = Var {
        parent.use()?.sceneRoot?.use()
    }

    /**
     * If false, this element and its children will not be rendered.
     */
    val visible: Var<Boolean> = Var(true)

    /**
     * If true, this element will render with [ScissorStack] clipping on its entire bounds.
     */
    val doClipping: Var<Boolean> = Var(false)

    /**
     * The opacity of this [UIElement].
     */
    val opacity: FloatVar = FloatVar(1f)
    /**
     * The [apparentOpacity] level of the [parent], if there is no parent then 1.0 is used.
     */
    val parentOpacity: ReadOnlyVar<Float> = FloatVar {
        parent.use()?.apparentOpacity?.use() ?: 1f
    }
    /**
     * The opacity level of this [UIElement], taking into account the parent's opacity level using
     * `parentOpacity * this.opacity`.
     *
     * This is to be used by the rendering implementation.
     */
    val apparentOpacity: ReadOnlyVar<Float> = FloatVar {
        parentOpacity.use() * opacity.use()
    }
    
    init {
        bindWidthToParent(0f)
        bindHeightToParent(0f)
    }

    @Suppress("RedundantModalityModifier")
    final fun render(originX: Float, originY: Float, batch: SpriteBatch) {
        if (!visible.getOrCompute()) return
        
        val clip = doClipping.getOrCompute()
        val childOriginBounds = this.contentZone
        val childOriginX = childOriginBounds.x.getOrCompute()
        val childOriginY = childOriginBounds.y.getOrCompute()
        renderOptionallyWithClip(originX, originY, batch, clip) { _, _, _ ->
            this.renderSelf(originX, originY, batch)
            this.renderChildren(originX + childOriginX, originY - childOriginY, batch)
            this.renderSelfAfterChildren(originX, originY, batch)
        }
    }
    
    protected open fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
    }
    
    protected /*open*/ fun renderChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        children.forEach { 
            it.render(originX, originY, batch)
        }
    }
    
    protected open fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
    }

    fun addChild(child: UIElement) {
        if (child !in children) {
            child.parent.getOrCompute()?.removeChild(child)
            children = children + child
            child.parent.set(this)
        }
    }

    fun removeChild(child: UIElement) {
        if (child in children) {
            children = children - child
            child.parent.set(null)
        }
    }
    
    fun addInputEventListener(listener: InputEventListener) {
        val current = inputListeners.getOrCompute()
        if (listener !in current) {
            inputListeners.set(current + listener)
        }
    }

    fun removeInputEventListener(listener: InputEventListener) {
        val current = inputListeners.getOrCompute()
        if (listener in current) {
            inputListeners.set(current - listener)
        }
    }

    /**
     * Begins clipping, defaulting to this UIElement's bounds. Wrap the drawing for the clip section in an
     * if statement with the return value of this function. Returns false if the resultant scissor would have zero area.
     * Call [SpriteBatch.flush] before calling this function and before calling [clipEnd].
     */
    fun clipBegin(originX: Float, originY: Float, x: Float, y: Float, width: Float, height: Float): Boolean {
        val root = sceneRoot.getOrCompute()
        val rootBounds = root?.bounds
        val rootWidth: Float = rootBounds?.width?.getOrCompute() ?: width
        val rootHeight: Float = rootBounds?.height?.getOrCompute() ?: height

        val scissorX = (originX + x) / rootWidth * Gdx.graphics.width
        val scissorY = ((originY - y) / rootHeight) * Gdx.graphics.height
        val scissorW = (width / rootWidth) * Gdx.graphics.width
        val scissorH = (height / rootHeight) * Gdx.graphics.height
        val scissor = Rectangle(scissorX, scissorY - scissorH, scissorW, scissorH)
        
        val pushScissor = ScissorStack.pushScissor(scissor)
        return pushScissor
    }
    
    fun clipBegin(originX: Float, originY: Float): Boolean {
        val bounds = this.bounds
        return clipBegin(originX, originY, bounds.x.getOrCompute(), bounds.y.getOrCompute(), bounds.width.getOrCompute(), bounds.height.getOrCompute())
    }
    
    fun clipEnd() {
        ScissorStack.popScissor()
    }
    
    fun bindWidthToParent(adjust: Float = 0f) {
        val thisBounds = this.bounds
        thisBounds.width.bind { 
            (this@UIElement.parent.use()?.let { p -> p.contentZone.width.use() } ?: 0f) + adjust
        }
    }

    fun bindHeightToParent(adjust: Float = 0f) {
        val thisBounds = this.bounds
        thisBounds.height.bind {
            (this@UIElement.parent.use()?.let { p -> p.contentZone.height.use() } ?: 0f) + adjust
        }
    }
    
    protected inline fun renderOptionallyWithClip(originX: Float, originY: Float, batch: SpriteBatch, clip: Boolean,
                                                  renderFunc: (originX: Float, originY: Float, batch: SpriteBatch) -> Unit) {
        if (clip) {
            batch.flush()
            if (clipBegin(originX, originY)) {
                renderFunc(originX, originY, batch)
                batch.flush()
                clipEnd()
            }
        } else {
            renderFunc(originX, originY, batch)
        }
    }

    /**
     * Returns a list of UIElements from this element to the child that contains the point.
     * IMPLEMENTATION NOTE: This function assumes that all the children for a parent fit inside of that parent's bounds.
     */
    fun pathTo(x: Float, y: Float): List<UIElement> {
        val res = mutableListOf<UIElement>()
        var current: UIElement = this
        var currentBounds: ReadOnlyBounds = current.bounds
        var xOffset: Float = currentBounds.x.getOrCompute()
        var yOffset: Float = currentBounds.y.getOrCompute()
        while (current.children.isNotEmpty()) {
            val found = current.children.findLast { it.bounds.containsPointLocal(x - xOffset, y - yOffset) } ?: break
            res += found
            current = found
            currentBounds = current.bounds
            xOffset += currentBounds.x.getOrCompute()
            yOffset += currentBounds.y.getOrCompute()
        }
        return res
    }
    
    /**
     * Returns a list of UIElements from this element to the child that contains the point within [UIBounds.borderZone].
     * IMPLEMENTATION NOTE: This function assumes that all the children for a parent fit inside of that parent's bounds.
     */
    fun pathToForInput(x: Float, y: Float): List<UIElement> {
        val res = mutableListOf<UIElement>()
        var current: UIElement = this
        var currentBounds: ReadOnlyBounds = current.borderZone
        var xOffset: Float = currentBounds.x.getOrCompute()
        var yOffset: Float = currentBounds.y.getOrCompute()
        while (current.children.isNotEmpty()) {
            val found = current.children.findLast { it.borderZone.containsPointLocal(x - xOffset, y - yOffset) } ?: break
            res += found
            current = found
            currentBounds = current.borderZone
            xOffset += currentBounds.x.getOrCompute()
            yOffset += currentBounds.y.getOrCompute()
        }
        return res
    }

//    tailrec fun pathTo(x: Float, y: Float, startAt: UIElement = this, acc: MutableList<UIElement> = mutableListOf()): List<UIElement> {
//        val found = startAt.children.findLast { it.bounds.containsPoint(x, y) } ?: return acc
//        acc += found
//        return pathTo(x - found.bounds.x.getOrCompute(), y - found.bounds.y.getOrCompute(), found, acc)
//    }
    
    operator fun plusAssign(child: UIElement) = addChild(child)
    operator fun minusAssign(child: UIElement) = removeChild(child)

}

