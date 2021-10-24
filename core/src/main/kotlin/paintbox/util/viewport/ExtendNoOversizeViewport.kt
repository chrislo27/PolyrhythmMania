package paintbox.util.viewport

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.*
import kotlin.math.roundToInt


/**
 * Like a hybrid of [FitViewport] and [ExtendViewport], but without the overfilling of [ExtendViewport] and with the
 * world resizing that [FitViewport] does not have.
 * 
 * [ExtendViewport] will try to oversize the world (without given limits), but [ExtendNoOversizeViewport] will
 * not extend the shorter dimension.
 * 
 * [FitViewport] doesn't change the world size upon update; [ExtendNoOversizeViewport] will.
 * The original world size ([originalWorldWidth], [originalWorldHeight]) is stored and is immutable.
 */
class ExtendNoOversizeViewport(val originalWorldWidth: Float, val originalWorldHeight: Float, camera: OrthographicCamera) 
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