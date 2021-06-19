package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import paintbox.util.Vector3Stack
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.WorldRenderer
import kotlin.math.absoluteValue
import kotlin.math.floor


class EntityInputIndicator(world: World, val isDpad: Boolean)
    : SimpleRenderedEntity(world) {
    
    var visible: Boolean = true

    override val renderWidth: Float = 16f / 32f
    override val renderHeight: Float = 16f / 32f
    
    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine, vec: Vector3) {
        if (!visible) return
        val tintedRegion = if (isDpad) tileset.indicatorDpad else tileset.indicatorA
        val renderWidth = this.renderWidth
        val renderHeight = this.renderHeight

        val bumpHeight = 2f / 32f
        val beat = engine.beat
        val bumpTime = 0.28f
        val normalizedBeat = if (beat < 0f) (beat + floor(beat).absoluteValue) else (beat)
        val bumpAmt = (1f - (normalizedBeat % 1f).coerceIn(0f, bumpTime) / bumpTime)//.coerceIn(0f, 1f)
        
        batch.color = tintedRegion.color.getOrCompute()
        batch.draw(tintedRegion.region, vec.x - renderWidth / 2f,
                vec.y + (bumpAmt) * bumpHeight - (2f / 32f),
                renderWidth, renderHeight)
    }
}