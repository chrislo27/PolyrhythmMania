package polyrhythmmania.storymode.screen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import paintbox.util.ColorStack
import paintbox.util.gdxutils.drawQuad
import polyrhythmmania.screen.play.pause.TengokuPauseBackground


class StoryPlayGradientRenderer(cycleSpeedMultiplier: Float = 1f) : TengokuPauseBackground.GradientRenderer {

    private val colors: List<Color> = listOf(
            Color.valueOf("4aff4a"), // Green
            Color.valueOf("2963ff"), // Blue
            Color.valueOf("f13d5e"), // Red
            Color.valueOf("2963ff"), // Blue
    )

    var cycleSpeed: Float = (1f / ((colors.size + 1) * 1.5f)) * cycleSpeedMultiplier
    private var position: Float = 0f // 0..1

    override fun render(delta: Float, batch: SpriteBatch, camera: OrthographicCamera, seed: Int) {
        val width = camera.viewportWidth
        val height = camera.viewportHeight

        if (cycleSpeed > 0f) {
            position += cycleSpeed * delta
            position %= 1f
        }
        
        val offsetPosition = (position - (1f / colors.size) + 1f) % 1f
        val topColor = ColorStack.getAndPush().set(1f, 1f, 1f, 1f)
        val bottomColor = ColorStack.getAndPush().set(1f, 1f, 1f, 1f)
        
        interpolateColor(bottomColor, position)
        interpolateColor(topColor, offsetPosition)
        val inbetweenColor = ColorStack.getAndPush().set(topColor).lerp(bottomColor, 0.5f)

        // Arg order: bottom left, bottom right, top right, top left
        batch.drawQuad(0f, 0f, inbetweenColor, width, 0f, bottomColor,
                width, height, inbetweenColor, 0f, height, topColor)
        batch.setColor(1f, 1f, 1f, 1f)
        
        repeat(3) {
            ColorStack.pop()
        }
    }
    
    private fun interpolateColor(color: Color, position: Float) {
        val startIndex = (position * colors.size).toInt()
        val endIndex = startIndex + 1
        
        val numSegments = colors.size // The first is also the last, so it's not -1
        val interpolation = Interpolation.pow5
        
        val firstColor = colors[startIndex % colors.size]
        val secondColor = colors[endIndex % colors.size]
        color.set(firstColor).lerp(secondColor, interpolation.apply(position % (1f / numSegments) * numSegments))
    }
}
