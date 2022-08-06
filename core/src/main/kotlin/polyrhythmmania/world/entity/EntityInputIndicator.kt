package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.World
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset
import kotlin.math.absoluteValue
import kotlin.math.floor

class EntityInputIndicator(world: World, var isDpad: Boolean)
    : SimpleRenderedEntity(world) {

    var visible: Boolean = true

    override val renderWidth: Float = 16f / 32f
    override val renderHeight: Float = 16f / 32f
    
    private var lastBeat: Float = 0f

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        if (!visible || !world.worldSettings.showInputIndicators) return
        val tintedRegion = if (isDpad) tileset.indicatorDpad else tileset.indicatorA
        val renderWidth = this.renderWidth
        val renderHeight = this.renderHeight

        val bumpHeight = 2f / 32f
        val beat = lastBeat
        val bumpTime = 0.28f
        val normalizedBeat = if (beat < 0f) (beat + floor(beat).absoluteValue) else (beat)
        val bumpAmt = (1f - (normalizedBeat % 1f).coerceIn(0f, bumpTime) / bumpTime)//.coerceIn(0f, 1f)

        drawTintedRegion(batch, vec, tileset, tintedRegion, -renderWidth / 2f, (bumpAmt) * bumpHeight - (2f / 32f),
                renderWidth, renderHeight)
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        this.lastBeat = beat
    }
}
