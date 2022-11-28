package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import paintbox.util.MathHelper
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
                    triggerPiston(to.pistonIndex, false)
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
    private var pistonIndex: Int = 0
    
    fun render(batch: SpriteBatch) {
        val width = camera.viewportWidth
        val height = camera.viewportHeight
        
        batch.draw(StoryAssets.get<Texture>("desk_bg_background"), 0f, 0f, width, height)
        batch.draw(StoryAssets.get<Texture>("desk_bg_tube_1"), 0f, 0f, width, height)
        batch.draw(StoryAssets.get<Texture>("desk_bg_piston_background"), 0f, 0f, width, height)
        
        // TODO draw envelope in tube
        val delta = Gdx.graphics.deltaTime
        envelopes.forEach { 
            it.update(delta)
            val tex = StoryAssets.get<Texture>("desk_bg_envelope_${getEnvelopeFrameNum(it.offset)}")
            val offset = tex.width / 2f
            batch.draw(tex, (it.x - offset) * UI_SCALE, height - (it.y + offset) * UI_SCALE, tex.width * UI_SCALE * 1f, tex.height * UI_SCALE * 1f)
        }
        envelopes.removeIf { it.shouldDelete }
        
        val pistonFrame = pistonIndex + 1
        batch.draw(StoryAssets.get<Texture>("desk_bg_pistons_$pistonFrame"), 0f, 0f, width, height)
        
        batch.draw(StoryAssets.get<Texture>("desk_bg_tube_2"), 0f, 0f, width, height)
        batch.draw(StoryAssets.get<Texture>("desk_bg_tube_3"), 0f, 0f, width, height)
        batch.draw(StoryAssets.get<Texture>("desk_bg_inbox"), 0f, 0f, width, height)
    }
    
    fun sendEnvelope() {
        envelopes += Envelope(envelopeOffsetNum++)
    }
    
    fun triggerPiston(pistonIndex: Int, middle: Boolean) {
        // TODO middle doesn't work because sprites are incorrect
        val newIndex = (pistonIndex + 1) * 2 // (if (middle) 1 else 0)
        if (this.pistonIndex < newIndex && newIndex in 0 until PISTON_FRAMES) {
            this.pistonIndex = newIndex
        }
    }
    
    fun resetAll() {
        removeAllEnvelopes()
        resetPistons()
    }
    
    fun resetPistons() {
        pistonIndex = 0
    }
    
    fun removeAllEnvelopes() {
        envelopes.clear()
    }
    
    private fun getEnvelopeFrameNum(offset: Int = 0, speed: Float = 0.1f): Int {
        val count = ENVELOPE_FRAMES.size
        return ENVELOPE_FRAMES[((MathHelper.getSawtoothWave(speed / ((count * 2 - 1) / count)) * count).toInt() + offset) % count]
    }
    
}
