package io.github.chrislo27.paintbox.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.ui.area.ReadOnlyBounds
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.ui.border.Border
import io.github.chrislo27.paintbox.ui.border.NoBorder


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

    val borderStyle: Var<Border> = Var(NoBorder)

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

            val borderStyle = this.borderStyle.getOrCompute()
            borderStyle.renderBorder(originX, originY, batch, this)
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

    fun addChild(child: UIElement): Boolean {
        if (child !in children) {
            child.parent.getOrCompute()?.removeChild(child)
            children = children + child
            child.parent.set(this)
            this.onChildAdded(child)
            return true
        }
        return false
    }

    fun removeChild(child: UIElement): Boolean {
        if (child in children) {
            children = children - child
            child.parent.set(null)
            this.onChildRemoved(child)
            return true
        }
        return false
    }

    protected open fun onChildAdded(newChild: UIElement) {
    }
    
    protected open fun onChildRemoved(oldChild: UIElement) {
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

    fun bindWidthToParent(adjustBinding: Var.Context.() -> Float) {
        val thisBounds = this.bounds
        thisBounds.width.bind {
            (this@UIElement.parent.use()?.let { p -> p.contentZone.width.use() } ?: 0f) + adjustBinding()
        }
    }

    fun bindHeightToParent(adjustBinding: Var.Context.() -> Float) {
        val thisBounds = this.bounds
        thisBounds.height.bind {
            (this@UIElement.parent.use()?.let { p -> p.contentZone.height.use() } ?: 0f) + adjustBinding()
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
        var currentBounds: ReadOnlyBounds = current.contentZone
        var xOffset: Float = currentBounds.x.getOrCompute()
        var yOffset: Float = currentBounds.y.getOrCompute()
        while (current.children.isNotEmpty()) {
            val found = current.children.findLast { child ->
                child.bounds.containsPointLocal(x - xOffset, y - yOffset)
            } ?: break
            res += found
            current = found
            currentBounds = current.contentZone
            xOffset += currentBounds.x.getOrCompute()
            yOffset += currentBounds.y.getOrCompute()
        }
        return res
    }

    /**
     * Returns a list of [UIElement]s from this element to the child that contains the point within [UIBounds.contentZone].
     * It also excludes elements that are not [visible].
     * IMPLEMENTATION NOTE: This function assumes that all the children for a parent fit inside of that parent's bounds.
     */
    fun pathToForInput(x: Float, y: Float): List<UIElement> {
        val res = mutableListOf<UIElement>()
        var current: UIElement = this
        var currentBounds: ReadOnlyBounds = current.contentZone
        var xOffset: Float = currentBounds.x.getOrCompute()
        var yOffset: Float = currentBounds.y.getOrCompute()
        while (current.children.isNotEmpty()) {
            val found = current.children.findLast { child ->
                child.visible.getOrCompute() && child.borderZone.containsPointLocal(x - xOffset, y - yOffset)
            } ?: break
            res += found
            current = found
            currentBounds = current.contentZone
            xOffset += currentBounds.x.getOrCompute()
            yOffset += currentBounds.y.getOrCompute()
        }
        return res
    }

    /**
     * Returns the xy position relative to the uppermost parent.
     */
    fun getPosRelativeToRoot(vector: Vector2 = Vector2(0f, 0f)): Vector2 {
        vector.set(0f, 0f)
        // Traverse up the tree
        var current: UIElement = this
        var currentParent: UIElement? = current.parent.getOrCompute()
        while (currentParent != null) {
            val bounds: ReadOnlyBounds = currentParent.contentZone
            vector.x += bounds.x.getOrCompute()
            vector.y += bounds.y.getOrCompute()
            current = currentParent
            currentParent = current.parent.getOrCompute()
        }

        vector.x += this.bounds.x.getOrCompute()
        vector.y += this.bounds.y.getOrCompute()

        return vector
    }

    operator fun plusAssign(child: UIElement) {
        addChild(child)
    }
    operator fun minusAssign(child: UIElement) {
        removeChild(child)
    }

}

