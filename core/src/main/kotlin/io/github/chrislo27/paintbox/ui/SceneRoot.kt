package io.github.chrislo27.paintbox.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import io.github.chrislo27.paintbox.Paintbox
import io.github.chrislo27.paintbox.binding.*
import io.github.chrislo27.paintbox.ui.contextmenu.ContextMenu
import io.github.chrislo27.paintbox.util.gdxutils.drawRect


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
    val mainLayer: Layer = Layer("main", enableTooltips = true, exclusiveTooltipAccess = false, rootElement = this)
    val dialogLayer: Layer = Layer("dialog", enableTooltips = true, exclusiveTooltipAccess = true)
    val contextMenuLayer: Layer = Layer("contextMenu", enableTooltips = true, exclusiveTooltipAccess = false)
    val tooltipLayer: Layer = Layer("tooltip", enableTooltips = false, exclusiveTooltipAccess = false)
    val allLayers: List<Layer> = listOf(mainLayer, dialogLayer, contextMenuLayer, tooltipLayer)
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
    
    private var rootContextMenu: ContextMenu? = null
    private var rootDialogElement: UIElement? = null
    
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
        
        contextMenuLayer.root.addInputEventListener { event ->
            var inputConsumed = false
            if (event is TouchDown) {
                if (rootContextMenu != null) {
                    hideRootContextMenu()
                    inputConsumed = true
                }
            }
            inputConsumed
        }
        
        dialogLayer.root.addInputEventListener { event ->
            rootDialogElement != null // Dialog layer eats all input when active
        }
    }
    
    fun renderAsRoot(batch: SpriteBatch) {
        (frameUpdateTrigger as Var).invert()
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
            val isDialogPresent = this.rootDialogElement != null
            for (layer in allLayers) {
                if (isDialogPresent && layer == mainLayer) continue
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
        hideRootContextMenu()
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

    /**
     * Shows the [contextMenu] as the root menu. This will hide the existing context menu if any.
     */
    fun showRootContextMenu(contextMenu: ContextMenu) {
        hideRootContextMenu()
        addContextMenuToScene(contextMenu)
        rootContextMenu = contextMenu
        contextMenuLayer.resetHoveredElementPath()
    }

    /**
     * Hides the root context menu if any.
     */
    fun hideRootContextMenu() {
        val currentRootMenu = rootContextMenu ?: return
        removeContextMenuFromScene(currentRootMenu)
        rootContextMenu = null
        contextMenuLayer.resetHoveredElementPath()
    }

    /**
     * Shows the [dialog] as the root dialog element. This will hide the existing dialog if any.
     */
    fun showRootDialog(dialog: UIElement) {
        hideRootDialog()
        rootDialogElement = dialog
        dialogLayer.root.addChild(dialog)
        dialogLayer.resetHoveredElementPath()
        cancelTooltip()
    }

    /**
     * Hides the root dialog element if any.
     */
    fun hideRootDialog() {
        val currentRootDialog = rootDialogElement ?: return
        dialogLayer.root.removeChild(currentRootDialog)
        rootDialogElement = null
        dialogLayer.resetHoveredElementPath()
        cancelTooltip()
    }

    /**
     * Adds the [contextMenu] to the scene. The [contextMenu] should be a "menu child" of another [ContextMenu],
     * but all context menus reside on the same level of the scene graph.
     * 
     * This function is called from [ContextMenu.addChildMenu] so you should not call this on your own.
     * 
     * To show a root context menu, call [showRootContextMenu].
     * 
     * This does NOT connect the parent-child
     * relationship. One should call [ContextMenu.addChildMenu] for that.
     */
    fun addContextMenuToScene(contextMenu: ContextMenu) {
        // Add to the contextMenu layer scene
        // Compute the width/height layouts
        // Position the context menu according to its parent (if any)
        val root = contextMenuLayer.root
        if (contextMenu !in root.children) {
            root.addChild(contextMenu)
            
            contextMenu.computeSize(this)
            
            // Temporary impl: assumes they are only root context menus and positions it at the mouse
            val w = contextMenu.bounds.width.getOrCompute()
            val h = contextMenu.bounds.height.getOrCompute()
            var x = mousePosition.x.getOrCompute()
            var y = mousePosition.y.getOrCompute()

            val thisWidth = this.bounds.width.getOrCompute()
            val thisHeight = this.bounds.height.getOrCompute()
            if (x + w > thisWidth) x = thisWidth - w
            if (x < 0f) x = 0f
            if (y + h > thisHeight) y = thisHeight - h
            if (y < 0f) y = 0f
            
            contextMenu.bounds.x.set(x)
            contextMenu.bounds.y.set(y)
            
            // TODO position the context menu according to its parent if NOT the root
            
            contextMenu.onAddedToScene.invoke(this)
            
            cancelTooltip()
        }
    }

    /**
     * Removes the [contextMenu] from the scene. The [contextMenu] may be a "menu child" of another [ContextMenu],
     * but all context menus reside on the same level of the scene graph.
     * Any children of [contextMenu] will NOT be removed, that is the responsiblity of [ContextMenu.removeChildMenu].
     * 
     * This function is called from [ContextMenu.removeChildMenu] so you should not call this on your own.
     * 
     * To hide a root context menu, call [hideRootContextMenu].
     * 
     * This does NOT disconnect the parent-child
     * relationship. One should call [ContextMenu.removeChildMenu] for that.
     */
    fun removeContextMenuFromScene(contextMenu: ContextMenu) {
        // Remove from the contextMenu layer scene
        val root = contextMenuLayer.root
        if (root.removeChild(contextMenu)) {
            contextMenu.onRemovedFromScene.invoke(this)
            cancelTooltip()
        }
    }
    
    fun isContextMenuActive(): Boolean = rootContextMenu != null
    
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
    
    inner class Layer(val name: String, val enableTooltips: Boolean, val exclusiveTooltipAccess: Boolean,
                      rootElement: UIElement = Pane()) {
        /**
         * Used by [InputSystem] for mouse-path tracking.
         */
        val lastHoveredElementPath: MutableList<UIElement> = mutableListOf()
        
        val root: UIElement = rootElement
        
        fun resetHoveredElementPath() {
            // FIXME may need improvemnets
            lastHoveredElementPath.clear()
            this@SceneRoot.cancelTooltip()
        }
        
        fun shouldEatTooltipAccess(): Boolean {
            return exclusiveTooltipAccess && root.children.isNotEmpty()
        }
    }
    
}