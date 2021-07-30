package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.World
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.render.WorldRenderer

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
    
    open fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine) {
    }
    
    open fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {}
    
    open fun onRemovedFromWorld(engine: Engine) {}
}