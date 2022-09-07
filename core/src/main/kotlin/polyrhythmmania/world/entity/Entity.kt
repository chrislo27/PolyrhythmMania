package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.World
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset

open class Entity(val world: World) {

    val position: Vector3 = Vector3()
    var isKilled: Boolean = false
        private set
    
    fun kill() {
        if (!isKilled) {
            isKilled = true
            onKilled()
        }
    }
    
    protected open fun onKilled() {}

    open val renderWidth: Float = 1f
    open val renderHeight: Float = 1f
    
    open val renderSortOffsetX: Float get() = 0f
    open val renderSortOffsetY: Float get() = 0f
    open val renderSortOffsetZ: Float get() = 0f
    
    open fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset) {
    }
    
    open fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {}
    
    open fun onRemovedFromWorld(engine: Engine) {}

    /**
     * Sets this entity's culling rect. The [rect] should not be stored as it is reused.
     * [tmpVec3] is passed in as a temporary [Vector3] that can be used.
     */
    open fun setCullingRect(rect: Rectangle, tmpVec3: Vector3) {
        val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec3.set(this.position))
        rect.set(convertedVec.x, convertedVec.y, this.renderWidth, this.renderHeight)
    }

    /**
     * True if render culling is in effect. Only should be false for certain entities, like [EntityBackgroundImg].
     */
    open fun shouldApplyRenderCulling(): Boolean = true
    
}
