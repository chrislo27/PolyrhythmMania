package io.github.chrislo27.paintbox.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import io.github.chrislo27.paintbox.Paintbox
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.util.gdxutils.drawRect


/**
 * The [SceneRoot] element has the position 0, 0 and always has the width and height of the UI screen space.
 */
class SceneRoot(width: Float, height: Float) : UIElement() {
    
    val inputSystem: InputSystem = InputSystem(this)
    
    constructor(width: Int, height: Int) : this(width.toFloat(), height.toFloat())
    
    init {
        (sceneRoot as Var).set(this)
        bounds.also { b ->
            b.x.set(0f)
            b.y.set(0f)
            b.width.set(width)
            b.height.set(height)
        }
    }
    
    fun renderAsRoot(batch: SpriteBatch) {
        render(bounds.x.getOrCompute(), bounds.y.getOrCompute() + bounds.height.getOrCompute(), batch)
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        // NO-OP
    }
    
    private fun UIElement.drawDebugRect(originX: Float, originY: Float, batch: SpriteBatch, onlyVisible: Boolean) {
        val thisBounds = bounds
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

        val drawOutlines = Paintbox.stageOutlines
        if (drawOutlines != Paintbox.StageOutlineMode.NONE) {
            val lastPackedColor = batch.packedColor
            batch.setColor(0f, 1f, 0f, 1f)
            this.drawDebugRect(originX, originY, batch, drawOutlines == Paintbox.StageOutlineMode.ONLY_VISIBLE)
            batch.packedColor = lastPackedColor
        }
    }

    fun renderChildren(batch: SpriteBatch) {
        renderChildren(bounds.x.getOrCompute(), bounds.y.getOrCompute() + bounds.height.getOrCompute(), batch)
    }
    
    fun resize(width: Float, height: Float, posX: Float = 0f, posY: Float = 0f) {
        bounds.also { b ->
            b.x.set(posX)
            b.y.set(posY)
            b.width.set(width)
            b.height.set(height)
        }
    }
    
    fun resize(camera: OrthographicCamera) {
        resize(camera.viewportWidth, camera.viewportHeight,
               camera.position.x - (camera.zoom * camera.viewportWidth / 2.0f),
               camera.position.y - (camera.zoom * camera.viewportHeight / 2.0f))
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
    
}