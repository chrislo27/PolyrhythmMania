package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.render.Tileset
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

    open fun getRenderWidth(): Float = 1f
    open fun getRenderHeight(): Float = 1f
    
    open fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine) {
    }
    
    open fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {}
    
}

open class SimpleRenderedEntity(world: World) : Entity(world) {
    
    companion object {
        protected val tmpVec = Vector3()
    }

    /*protected */open fun getTextureRegionFromTileset(tileset: Tileset): TextureRegion? = null

    open fun getSpriteWidth(): Float = getRenderWidth()
    open fun getSpriteHeight(): Float = getRenderHeight()
    
    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine) {
        val texReg = getTextureRegionFromTileset(tileset) ?: return
        val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(this.position))
        batch.draw(texReg, convertedVec.x, convertedVec.y, getRenderWidth(), getRenderHeight())
    }

}