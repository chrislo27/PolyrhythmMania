package paintbox

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.utils.Disposable
import paintbox.transition.TransitionScreen


abstract class PaintboxScreen : Screen, InputProcessor, Disposable {

    abstract val main: PaintboxGame

    override fun render(delta: Float) {
    }

    open fun renderUpdate() {
    }

    open fun getDebugString(): String? {
        return null
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun show() {
        main.inputMultiplexer.removeProcessor(this)
        main.inputMultiplexer.addProcessor(this)
    }

    override fun hide() {
        main.inputMultiplexer.removeProcessor(this)
    }

    /**
     * Called by [TransitionScreen] just before the current screen is switched to this one.
     */
    open fun showTransition() {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun keyDown(keycode: Int): Boolean {
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }
}