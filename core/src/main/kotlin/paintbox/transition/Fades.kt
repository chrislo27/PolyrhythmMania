package paintbox.transition


import com.badlogic.gdx.graphics.Color
import paintbox.util.gdxutils.fillRect


/**
 * Fades TO the specified colour from transparent to opaque.
 */
class FadeOut(duration: Float, val color: Color) : Transition(duration) {

    override fun render(transitionScreen: TransitionScreen, screenRender: () -> Unit) {
        screenRender()

        val camera = transitionScreen.main.nativeCamera
        val batch = transitionScreen.main.batch
        transitionScreen.main.resetViewportToScreen()
        batch.begin()
        batch.setColor(color.r, color.g, color.b, color.a * transitionScreen.percentageCurrent)
        batch.fillRect(0f, 0f, camera.viewportWidth * 1f, camera.viewportHeight * 1f)
        batch.setColor(1f, 1f, 1f, 1f)
        batch.end()
    }

    override fun dispose() {
    }

}

/**
 * Fades AWAY from the specified colour to transparent
 */
class FadeIn(duration: Float, val color: Color) : Transition(duration) {

    override fun render(transitionScreen: TransitionScreen, screenRender: () -> Unit) {
        screenRender()

        val camera = transitionScreen.main.nativeCamera
        val batch = transitionScreen.main.batch
        transitionScreen.main.resetViewportToScreen()
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.setColor(color.r, color.g, color.b, color.a * (1f - transitionScreen.percentageCurrent))
        batch.fillRect(0f, 0f, camera.viewportWidth * 1f, camera.viewportHeight * 1f)
        batch.setColor(1f, 1f, 1f, 1f)
        batch.end()
    }

    override fun dispose() {
    }

}
