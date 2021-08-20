package paintbox.util.viewport

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScalingViewport
import com.badlogic.gdx.utils.viewport.ExtendViewport
import kotlin.math.roundToInt


/**
 * Like [ExtendViewport] except the world size changes too.
 */
class ExtendFixedRatioViewport(val originalWorldWidth: Float, val originalWorldHeight: Float, camera: OrthographicCamera) 
    : ScalingViewport(Scaling.fit, originalWorldWidth, originalWorldHeight, camera) {
    
    override fun update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean) {
        val scaled = scaling.apply(originalWorldWidth, originalWorldHeight, screenWidth.toFloat(), screenHeight.toFloat())
        val viewportWidth = scaled.x.roundToInt()
        val viewportHeight = scaled.y.roundToInt()

        setWorldSize(scaled.x, scaled.y)
        
        // Center.
        setScreenBounds((screenWidth - viewportWidth) / 2, (screenHeight - viewportHeight) / 2, viewportWidth, viewportHeight)

        apply(centerCamera)
    }
}