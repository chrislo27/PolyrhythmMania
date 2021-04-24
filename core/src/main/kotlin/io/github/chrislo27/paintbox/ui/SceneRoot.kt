package io.github.chrislo27.paintbox.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import io.github.chrislo27.paintbox.Paintbox
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.binding.VarChangedListener
import io.github.chrislo27.paintbox.util.gdxutils.drawRect
import kotlin.properties.Delegates


/**
 * The [SceneRoot] element has the position 0, 0 and always has the width and height of the UI screen space.
 */
class SceneRoot(width: Float, height: Float) : UIElement() {
    
    companion object {
        const val DEFAULT_TOOLTIP_HOVER_TIME: Float = 1f
    }
    
    data class MousePosition(val x: FloatVar, val y: FloatVar)
    
    private val mouseVector: Vector2 = Vector2()
    val mousePosition: MousePosition = MousePosition(FloatVar(0f), FloatVar(0f))
    val mainLayer: Layer = Layer("main", this)
    val contextMenuLayer: Layer = Layer("contextMenu")
    val tooltipLayer: Layer = Layer("tooltip")
    val allLayers: List<Layer> = listOf(mainLayer, contextMenuLayer, tooltipLayer)
    val allLayersReversed: List<Layer> = allLayers.asReversed()
    val inputSystem: InputSystem = InputSystem(this)

    /**
     * A var that is always updated at the start of [renderAsRoot].
     */
    val frameUpdateTrigger: ReadOnlyVar<Boolean> = Var(false)
    
    
    val tooltipHoverTime: FloatVar = FloatVar(DEFAULT_TOOLTIP_HOVER_TIME) // TODO use this
    val currentElementWithTooltip: ReadOnlyVar<HasTooltip?> = Var(null)
    private val currentTooltipVar: Var<UIElement?> = Var(null)
    private var currentTooltip: UIElement? = null
    
    constructor(width: Int, height: Int) : this(width.toFloat(), height.toFloat())
    
    init {
        (sceneRoot as Var).set(this)
        updateAllLayerBounds(width, height)
        currentTooltipVar.addListener { v ->
            val layer = tooltipLayer
            val root = layer.root
            val currentElement = currentElementWithTooltip.getOrCompute()
            val oldValue = currentTooltip
            val newValue = v.getOrCompute()
            if (oldValue != null) {
                currentElement?.onTooltipEnded(oldValue)
                root.removeChild(oldValue)
            }
            if (newValue != null) {
                currentElement?.onTooltipStarted(newValue)
                root.addChild(newValue)
            }
            currentTooltip = newValue
        }
    }
    
    fun renderAsRoot(batch: SpriteBatch) {
        (frameUpdateTrigger as Var).set(!frameUpdateTrigger.getOrCompute())
        updateMouseVector()
        updateTooltipPosition()
        for (layer in allLayers) {
            val layerRoot = layer.root
            val layerBounds = layerRoot.bounds
            val originX = layerBounds.x.getOrCompute()
            val originY = layerBounds.y.getOrCompute() + layerBounds.height.getOrCompute()
            layerRoot.render(originX, originY, batch)
        }

        val drawOutlines = Paintbox.stageOutlines
        if (drawOutlines != Paintbox.StageOutlineMode.NONE) {
            val lastPackedColor = batch.packedColor
            batch.setColor(0f, 1f, 0f, 1f)
            val useOutlines = drawOutlines == Paintbox.StageOutlineMode.ONLY_VISIBLE
            for (layer in allLayers) {
                val layerRoot = layer.root
                val layerBounds = layerRoot.bounds
                val originX = layerBounds.x.getOrCompute()
                val originY = layerBounds.y.getOrCompute() + layerBounds.height.getOrCompute()
                layer.root.drawDebugRect(originX, originY, batch, useOutlines)
            }
            batch.packedColor = lastPackedColor
        }
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        // NO-OP
    }
    
    private fun UIElement.drawDebugRect(originX: Float, originY: Float, batch: SpriteBatch, onlyVisible: Boolean) {
        val thisBounds = this.bounds
        val x = originX + thisBounds.x.getOrCompute()
        val y = originY - thisBounds.y.getOrCompute()
        val w = thisBounds.width.getOrCompute()
        val h = thisBounds.height.getOrCompute()
        if (onlyVisible && !this.visible.getOrCompute()) return
        batch.drawRect(x, y - h, w, h, 1f)
        
        val childOffsetX = originX + this.contentZone.x.getOrCompute()
        val childOffsetY = originY - this.contentZone.y.getOrCompute()
        this.children.forEach { child ->
            child.drawDebugRect(childOffsetX, childOffsetY, batch, onlyVisible)
        }
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelfAfterChildren(originX, originY, batch)
    }

//    fun renderChildren(batch: SpriteBatch) {
//        renderChildren(bounds.x.getOrCompute(), bounds.y.getOrCompute() + bounds.height.getOrCompute(), batch)
//    }
    
    private fun updateAllLayerBounds(width: Float, height: Float, posX: Float = 0f, posY: Float = 0f) {
        // Intentionally updating this.bounds before other layers.
        bounds.also { b ->
            b.x.set(posX)
            b.y.set(posY)
            b.width.set(width)
            b.height.set(height)
        }
        for (layer in allLayers) {
            if (layer === mainLayer) continue
            val root = layer.root
            val bounds = root.bounds
            bounds.x.set(posX)
            bounds.y.set(posY)
            bounds.width.set(width)
            bounds.height.set(height)
        }
    }
    
    fun resize(width: Float, height: Float, posX: Float = 0f, posY: Float = 0f) {
        updateAllLayerBounds(width, height, posX, posY)
    }
    
    fun resize(camera: OrthographicCamera) {
        resize(camera.viewportWidth, camera.viewportHeight,
               camera.position.x - (camera.zoom * camera.viewportWidth / 2.0f),
               camera.position.y - (camera.zoom * camera.viewportHeight / 2.0f))
    }
    
    private fun updateTooltipPosition(tooltip: UIElement? = currentTooltip) {
        if (tooltip == null) return


        val bounds = tooltip.bounds
        val width = bounds.width.getOrCompute()
        val height = bounds.height.getOrCompute()
        val mouseX = mousePosition.x.getOrCompute()
        val mouseY = mousePosition.y.getOrCompute()
        val rootWidth = this.bounds.width.getOrCompute()
        val rootHeight = this.bounds.height.getOrCompute()
        val rightAlign = (mouseY <= height)
        bounds.y.set((mouseY - height).coerceIn(0f, rootHeight - height))
        bounds.x.set((if (rightAlign) (mouseX - width) else mouseX).coerceIn(0f, rootWidth - width))
    }

    /**
     * For [InputSystem] to call when the mouse starts hovering over a [HasTooltip] [UIElement].
     */
    fun startTooltip(element: HasTooltip, tooltipVar: ReadOnlyVar<UIElement?>) {
        val currentElementWithTooltip = currentElementWithTooltip as Var
        cancelTooltip()
        currentElementWithTooltip.set(element)
        currentTooltipVar.bind { 
            tooltipVar.use()
        }
    }

    /**
     * For [InputSystem] to call when the mouse stops hovering over the element with the active tooltip.
     */
    fun cancelTooltip() {
        val currentElementWithTooltip = currentElementWithTooltip as Var
        currentTooltipVar.set(null)
        currentElementWithTooltip.set(null)
    }
    
    private fun updateMouseVector() {
        val vector = mouseVector
        screenToUI(vector.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
        mousePosition.x.set(vector.x)
        mousePosition.y.set(vector.y)
    }

    /**
     * Converts screen coordinates (from gdx Input) to local UI coordinates.
     * This [SceneRoot]'s width and height are assumed to span the entire window
     * from Gdx.graphics.getWidth() and Gdx.graphics.getHeight(), with x and y offsets accordingly.
     * @return The mutated [vector]
     */
    fun screenToUI(vector: Vector2): Vector2 {
        val screenWidth = Gdx.graphics.width
        val screenHeight = Gdx.graphics.height
        val boundsX = bounds.x.getOrCompute()
        val boundsY = bounds.y.getOrCompute()
        val boundsWidth = bounds.width.getOrCompute()
        val boundsHeight = bounds.height.getOrCompute()
        
        vector.x /= screenWidth
        vector.y /= screenHeight
        vector.x *= boundsWidth
        vector.y *= boundsHeight
        vector.x -= boundsX
        vector.y -= boundsY
        
        return vector
    }

    /**
     * Converts local UI coordinates to screen coordinates (from gdx Input).
     * This [SceneRoot]'s width and height are assumed to span the entire window
     * from Gdx.graphics.getWidth() and Gdx.graphics.getHeight(), with x and y offsets accordingly.
     * @return The mutated [vector]
     */
    fun uiToScreen(vector: Vector2): Vector2 {
        val screenWidth = Gdx.graphics.width
        val screenHeight = Gdx.graphics.height
        val boundsX = bounds.x.getOrCompute()
        val boundsY = bounds.y.getOrCompute()
        val boundsWidth = bounds.width.getOrCompute()
        val boundsHeight = bounds.height.getOrCompute()

        vector.x += boundsX
        vector.y += boundsY
        vector.x /= boundsWidth
        vector.y /= boundsHeight
        vector.x *= screenWidth
        vector.y *= screenHeight

        return vector
    }
    
    inner class Layer(val name: String, rootElement: UIElement = Pane()) {
        /**
         * Used by [InputSystem] for mouse-path tracking.
         */
        val lastHoveredElementPath: MutableList<UIElement> = mutableListOf()
        
        val root: UIElement = rootElement
    }
    
}