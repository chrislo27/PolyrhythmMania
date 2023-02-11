package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import paintbox.util.wave.WaveUtils
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.screen.desktop.DesktopUI.Companion.UI_SCALE
import kotlin.math.sqrt


class DesktopBackground(val camera: OrthographicCamera) {
    
    companion object {
        private const val PISTON_FRAMES: Int = 9
        private val ENVELOPE_FRAMES: List<Int> = listOf(1, 2, 3, 4, 5, 4, 3, 2)
    }
    
    private data class PathPoint(val x: Float, val y: Float, val pistonIndex: Int)
    
    private inner class Envelope(val offset: Int) {
        var x: Float = 0f
        var y: Float = 0f
        var currentPathIndex: Int = 0
        var shouldDelete: Boolean = false

        private var pathAlpha: Float = 0f

        fun update(delta: Float) {
            if (currentPathIndex !in 0 until path.size - 1) {
                shouldDelete = true
                return
            }
            
            val fromIndex = currentPathIndex
            val toIndex = fromIndex + 1
            val from = path[fromIndex]
            val to = path[toIndex]
            val distance = sqrt((to.x - from.x) * (to.x - from.x) + (to.y - from.y) * (to.y - from.y))
            val pathVelocity = 300f
            val pathSpeedMul = 1f
            
            pathAlpha += delta / (distance / (pathVelocity * pathSpeedMul))
            pathAlpha = pathAlpha.coerceIn(0f, 1f)
            
            val interpolation = Interpolation.linear
            this.x = interpolation.apply(from.x, to.x, pathAlpha)
            this.y = interpolation.apply(from.y, to.y, pathAlpha)
            
            if (pathAlpha >= 1f) {
                if (to.pistonIndex > -1) {
                    triggerPiston(to.pistonIndex)
                }
                
                nextPath(toIndex)
            }
        }
        
        private fun nextPath(newIndex: Int) {
            pathAlpha = 0f
            currentPathIndex = newIndex
        }
    }
    
    private val path: List<PathPoint> = listOf(
            PathPoint(271f, 214f, -1),
            PathPoint(175f, 167f, 0),
            PathPoint(223f, 143f, 1),
            PathPoint(319f, 190f, -1), // Skipped, out of screen-space
            PathPoint(367f, 166f, -1),
            PathPoint(207f, 87f, 2),
            PathPoint(255f, 63f, 3),
            PathPoint(415f, 142f, -1),
            PathPoint(271f, 214f, -1),
    )
    private val envelopes: MutableList<Envelope> = mutableListOf()
    private var envelopeOffsetNum: Int = 0
    private var pistonFrame: Int = 0
    private var incrementPistonFrameAfterSec: Float = 0f
    
    fun render(batch: SpriteBatch, isItemAvailable: Boolean) {
        val width = camera.viewportWidth
        val height = camera.viewportHeight
        
        batch.draw(StoryAssets.get<Texture>("desk_bg_background"), 0f, 0f, width, height)
//        batch.draw(StoryAssets.get<Texture>("desk_bg_tube_1"), 0f, 0f, width, height)
        batch.draw(StoryAssets.get<Texture>("desk_bg_piston_background"), 0f, 0f, width, height)
        
        val delta = Gdx.graphics.deltaTime
        var highestEnvelopeOffsetNum = -1
        var anyNeedDeleting = false
        envelopes.forEach {
            if (it.offset > highestEnvelopeOffsetNum) {
                highestEnvelopeOffsetNum = it.offset
            }
            
            it.update(delta)
            if (it.shouldDelete) {
                anyNeedDeleting = true
            }
            
            val tex = StoryAssets.get<Texture>("desk_bg_envelope_${getEnvelopeFrameNum(it.offset)}")
            val offset = tex.width / 2f
            batch.draw(tex, (it.x - offset) * UI_SCALE, height - (it.y + offset) * UI_SCALE, tex.width * UI_SCALE * 1f, tex.height * UI_SCALE * 1f)
        }
        if (anyNeedDeleting) {
            if (envelopes.any { it.offset == highestEnvelopeOffsetNum && it.shouldDelete }) {
                resetPistons()
            }
            envelopes.removeIf { it.shouldDelete }
        }
        
        batch.draw(StoryAssets.get<Texture>("desk_bg_pistons_${this.pistonFrame + 1}"), 0f, 0f, width, height)
        
//        batch.draw(StoryAssets.get<Texture>("desk_bg_tube_2"), 0f, 0f, width, height)
//        batch.draw(StoryAssets.get<Texture>("desk_bg_tube_3"), 0f, 0f, width, height)
        batch.draw(StoryAssets.get<Texture>(if (isItemAvailable) "desk_bg_inbox_available" else "desk_bg_inbox"), 0f, 0f, width, height)
        
        if (incrementPistonFrameAfterSec > 0) {
            incrementPistonFrameAfterSec -= delta
            if (incrementPistonFrameAfterSec <= 0f) {
                incrementPistonFrameAfterSec = 0f
                pistonFrame++
            }
        }
    }
    
    fun sendEnvelope() {
        envelopes += Envelope(envelopeOffsetNum++)
    }

    /**
     * @param pistonIndex 0-indexed piston. NOT the frame number!
     */
    fun triggerPiston(pistonIndex: Int) {
        // pistonIndex = 0, middle = false = index 1
        val newIndex = (pistonIndex * 2) + 1
        if (this.pistonFrame < newIndex && newIndex in 0 until PISTON_FRAMES) {
            this.pistonFrame = newIndex
            this.incrementPistonFrameAfterSec = 1f / 10 // TODO 1/30
        }
    }
    
    fun resetAll() {
        removeAllEnvelopes()
        resetPistons()
    }
    
    fun resetPistons() {
        this.pistonFrame = 0
        this.incrementPistonFrameAfterSec = 0f
    }
    
    fun removeAllEnvelopes() {
        envelopes.clear()
    }
    
    private fun getEnvelopeFrameNum(offset: Int = 0, speed: Float = 0.1f): Int {
        val count = ENVELOPE_FRAMES.size
        return ENVELOPE_FRAMES[((WaveUtils.getSawtoothWave(speed / ((count * 2 - 1) / count)) * count).toInt() + offset) % count]
    }
    
}
