package polyrhythmmania.screen.play.pause

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import paintbox.registry.AssetRegistry
import paintbox.util.MathHelper
import paintbox.util.gdxutils.drawQuad
import paintbox.util.gdxutils.fillRect
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class TengokuPauseBackground {
    
    interface GradientRenderer {
        fun render(delta: Float, batch: SpriteBatch, camera: OrthographicCamera, seed: Int)
    }
    
    class DefaultGradientRenderer : GradientRenderer {
        var cycleSpeed: Float = 1 / 30f
        val topColor: Color = Color.valueOf("232CDD")
        val bottomColor: Color = Color.valueOf("d020a0")
        private val hsv: FloatArray = FloatArray(3)
        
        override fun render(delta: Float, batch: SpriteBatch, camera: OrthographicCamera, seed: Int) {
            val width = camera.viewportWidth
            val height = camera.viewportHeight
            
            if (cycleSpeed > 0f) {
                topColor.toHsv(hsv)
                hsv[0] = (hsv[0] - delta * cycleSpeed * 360f) % 360f
                topColor.fromHsv(hsv)
                bottomColor.toHsv(hsv)
                hsv[0] = (hsv[0] - delta * cycleSpeed * 360f) % 360f
                bottomColor.fromHsv(hsv)
            }

            batch.drawQuad(0f, 0f, bottomColor, width, 0f, bottomColor,
                    width, height, topColor, 0f, height, topColor)
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }
    
    open class SingleColorGradientRenderer(val color: Color) : GradientRenderer {
        override fun render(delta: Float, batch: SpriteBatch, camera: OrthographicCamera, seed: Int) {
            batch.color = this.color
            batch.fillRect(0f, 0f, camera.viewportWidth, camera.viewportHeight)
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }
    
    object DebugColorGradientRenderer : SingleColorGradientRenderer(Color.valueOf("DB2323"))
    
    private val random = Random()
    var seed: Int = 0
        private set
    var gradientRenderer: GradientRenderer = DefaultGradientRenderer()
    var squareCount: Int = 90
    val triangleSlope: Float = 1 / 2f
    private val bgSquareTexReg: TextureRegion = TextureRegion(AssetRegistry.get<Texture>("pause_square"))

    init {
        randomizeSeed()
    }
    
    fun randomizeSeed() {
        seed = random.nextInt(100_000_000)
    }
    
    fun render(delta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        gradientRenderer.render(delta, batch, camera, seed)
        batch.setColor(1f, 1f, 1f, 1f)

        // Squares
        val width = camera.viewportWidth
        val currentMsTime = System.currentTimeMillis()
        val squareCount = this.squareCount
        batch.setColor(1f, 1f, 1f, 0.65f)
        for (i in 0 until squareCount) {
            val alpha = i / squareCount.toFloat()
            val size = Interpolation.circleIn.apply(20f, 80f, alpha) * 1.5f
            val rotation = MathHelper.getSawtoothWave(currentMsTime + (273L * alpha * 2).roundToLong(),
                    Interpolation.circleOut.apply(0.65f, 1.15f, alpha) * 0.75f) * (if (i % 2 == 0) -1 else 1)

            val yInterval = Interpolation.circleOut.apply(8f, 5f, alpha)
            val yAlpha = 1f - MathHelper.getSawtoothWave(currentMsTime + (562L * alpha * 2).roundToLong(), yInterval)
            val x = MathUtils.lerp(width * -0.1f, width * 1.1f, yAlpha)
            val y = (width * 1.4142135f * (i + 23) * (alpha + seed) + (yAlpha * yInterval).roundToInt()) % (width * 1.25f)

            drawSquare(batch, x - size / 2, y - size / 2, rotation * 360f, size)
        }

        batch.setColor(1f, 1f, 1f, 1f)
    }

    private fun drawSquare(batch: SpriteBatch, x: Float, y: Float, rot: Float, size: Float) {
        val width = size
        val height = size
        batch.draw(bgSquareTexReg, x - width / 2, y - height / 2, width / 2, height / 2, width, height, 1f, 1f, rot)
    }
}