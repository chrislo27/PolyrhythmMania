package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.WorldRenderer
import kotlin.math.absoluteValue
import kotlin.math.floor


class EntityInputIndicator(world: World, val isDpad: Boolean)
    : Entity(world) {

    companion object {
        private val tmpVec = Vector3()
    }
    
    var visible: Boolean = true

    override fun getRenderWidth(): Float = 16f / 32f
    override fun getRenderHeight(): Float = 16f / 32f

    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine) {
        if (!visible) return
        val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(this.position))
        val texReg = if (isDpad) tileset.indicatorD else tileset.indicatorA
        val renderWidth = getRenderWidth()
        val renderHeight = getRenderHeight()
        
        val bumpHeight = 2f / 32f
        val beat = engine.beat
        val bumpTime = 0.28f
        val normalizedBeat = if (beat < 0f) (beat + floor(beat).absoluteValue) else (beat)
        val bumpAmt = (1f - (normalizedBeat % 1f).coerceIn(0f, bumpTime) / bumpTime)//.coerceIn(0f, 1f)
        
        batch.draw(texReg, convertedVec.x - renderWidth / 2f,
                convertedVec.y + (bumpAmt) * bumpHeight - (2f / 32f),
                renderWidth, renderHeight)
    }
}