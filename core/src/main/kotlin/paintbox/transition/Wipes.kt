package paintbox.transition

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import paintbox.util.gdxutils.drawQuad
import paintbox.util.gdxutils.fillRect
import kotlin.math.absoluteValue

open class WipeTransition(
        val head: Boolean,
        val color: Color, duration: Float,
        val slope: Float = 4f, val interpolation: Interpolation = Interpolation.linear,
        /** Default: wipes from right to left */
        val invertDirection: Boolean = false,
) : Transition(duration) {
    
    override fun render(transitionScreen: TransitionScreen, screenRender: () -> Unit) {
        screenRender()

        val camera = transitionScreen.main.nativeCamera
        val batch = transitionScreen.main.batch
        transitionScreen.main.resetViewportToScreen()
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.setColor(color.r, color.g, color.b, color.a)

        val slopePart = (if (slope == 0f) 0f else (camera.viewportWidth / slope.absoluteValue))
        
        /*
        Right to left (invertDirection = false):
        Head:          Tail:
            /[]             []/
        
        Left to right (invertDirection = true):
        Head:          Tail:
            []/             /[]
            
         */
        val slopeOnLeft: Boolean = invertDirection xor head
        
        val xOffset = if (!invertDirection) {
            if (head)
                interpolation.apply(camera.viewportWidth, -slopePart, transitionScreen.percentageCurrent)
            else
                interpolation.apply(0f, -(camera.viewportWidth + slopePart), transitionScreen.percentageCurrent)
        } else {
            if (head)
                interpolation.apply(-(camera.viewportWidth + slopePart), 0f, transitionScreen.percentageCurrent)
            else
                interpolation.apply(-(slopePart), camera.viewportWidth, transitionScreen.percentageCurrent)
        }

        // Slope
        if (slopeOnLeft) {
            if (slope > 0f) {
                batch.drawQuad(xOffset, 0f, color, xOffset + slopePart, 0f, color,
                        xOffset + slopePart, camera.viewportHeight, color, xOffset + slopePart, camera.viewportHeight, color)
            } else if (slope < 0f) {
                batch.drawQuad(xOffset + slopePart, 0f, color, xOffset + slopePart, 0f, color,
                        xOffset + slopePart, camera.viewportHeight, color, xOffset, camera.viewportHeight, color)
            }
        } else {
            if (slope > 0f) {
                batch.drawQuad(xOffset + camera.viewportWidth, 0f, color, xOffset + camera.viewportWidth, 0f, color,
                        xOffset + slopePart + camera.viewportWidth, camera.viewportHeight, color, xOffset + camera.viewportWidth, camera.viewportHeight, color)
            } else if (slope < 0f) {
                batch.drawQuad(xOffset + camera.viewportWidth, 0f, color, xOffset + slopePart + camera.viewportWidth, 0f, color,
                        xOffset + camera.viewportWidth, camera.viewportHeight, color, xOffset + camera.viewportWidth, camera.viewportHeight, color)
            }
        }

        // Block
        if (slopeOnLeft) {
            batch.fillRect(xOffset + slopePart, 0f, camera.viewportWidth, camera.viewportHeight)
        } else {
            batch.fillRect(xOffset, 0f, camera.viewportWidth, camera.viewportHeight)
        }

        batch.setColor(1f, 1f, 1f, 1f)
        batch.end()
    }

    override fun dispose() {
    }
}

open class WipeTransitionHead(color: Color, duration: Float, slope: Float = 4f, interpolation: Interpolation = Interpolation.linear, invertDirection: Boolean = false)
    : WipeTransition(true, color, duration, slope, interpolation, invertDirection)
open class WipeTransitionTail(color: Color, duration: Float, slope: Float = 4f, interpolation: Interpolation = Interpolation.linear, invertDirection: Boolean = false)
    : WipeTransition(false, color, duration, slope, interpolation, invertDirection)

