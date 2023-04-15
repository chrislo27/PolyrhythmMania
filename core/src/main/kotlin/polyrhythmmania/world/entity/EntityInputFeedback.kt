package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import paintbox.util.ColorStack
import paintbox.util.Vector3Stack
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.InputScore
import polyrhythmmania.engine.input.InputTimingRestriction
import polyrhythmmania.world.World
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TintedRegion

class EntityInputFeedback(world: World, val end: End, baseColor: Color, val inputScore: InputScore, val flashIndex: Int)
    : SimpleRenderedEntity(world), HasLightingRender {
    
    companion object {
        val ACE_COLOUR: Color = Color.valueOf("FFF800")
        val GOOD_COLOUR: Color = Color.valueOf("6DE23B")
        val BARELY_COLOUR: Color = Color.valueOf("FF7C26")
        val MISS_COLOUR: Color = Color.valueOf("E82727")
        val OFF_COLOUR: Color = Color.valueOf("404040")
    }
    
    enum class End {
        LEFT, MIDDLE, RIGHT;
    }
    
    private val originalColor: Color = baseColor.cpy()
    private val currentColor: Color = baseColor.cpy()
    private val currentBaseColor: Color = baseColor.cpy()
    private var currentFlashPercentage: Float = 0f
    
    private fun getBaseColorToUse(engine: Engine): Color {
        val inputter = engine.inputter
        val restriction = inputter.inputChallenge.restriction
        return if (restriction == InputTimingRestriction.ACES_ONLY && this.inputScore != InputScore.ACE) {
            OFF_COLOUR
        } else if (restriction == InputTimingRestriction.NO_BARELY && this.inputScore == InputScore.BARELY) {
            OFF_COLOUR
        } else {
            originalColor
        }
    }
    
    private fun getTintedRegion(tileset: Tileset): TintedRegion {
        return when (end) {
            End.LEFT -> tileset.inputFeedbackStart
            End.MIDDLE -> tileset.inputFeedbackMiddle
            End.RIGHT -> tileset.inputFeedbackEnd
        }
    }

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        val tintedRegion = getTintedRegion(tileset)
        val tmpColor = ColorStack.getAndPush().set(tintedRegion.color.getOrCompute()) // tintedRegion's color is likely just white
        tmpColor.mul(this.currentColor)
        drawTintedRegion(batch, vec, tileset, tintedRegion, 0f, 0f, renderWidth, renderHeight, tmpColor)
        ColorStack.pop()
    }

    override fun renderLightingEffect(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset) {
        val flash = this.currentFlashPercentage
        if (flash > 0f) {
            val tmpVec = Vector3Stack.getAndPush()
            val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(getRenderVec()))
            val packedColor = batch.packedColor

            val tintedRegion = getTintedRegion(tileset)
            val tmpColor = ColorStack.getAndPush().set(this.currentBaseColor)
            tmpColor.a *= flash * 0.4f
            drawTintedRegion(batch, convertedVec, tileset, tintedRegion, 0f, 0f, renderWidth, renderHeight, tmpColor)
            ColorStack.pop()
            
            Vector3Stack.pop()
            batch.packedColor = packedColor
        }
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        
        updateCurrentColor(engine)
    }
    
    fun updateCurrentColor(engine: Engine) {
        val updatedBaseColor = getBaseColorToUse(engine)
        this.currentBaseColor.set(updatedBaseColor)
        val currentSec = engine.seconds
        val flashSec = engine.inputter.inputFeedbackFlashes[flashIndex]
        val flashTime = 0.25f
        if (currentSec - flashSec < flashTime) {
            val percentage = 1f - ((currentSec - flashSec) / flashTime).coerceIn(0f, 1f)
            this.currentFlashPercentage = percentage
            currentColor.set(updatedBaseColor).lerp(Color.WHITE, percentage)
        } else {
            this.currentFlashPercentage = 0f
            currentColor.set(updatedBaseColor)
        }
    }
}
