package polyrhythmmania.ui

import com.badlogic.gdx.InputProcessor
import paintbox.binding.BooleanVar


class TogglableInputProcessor(val delegate: InputProcessor) : InputProcessor {
    
    val enabled: BooleanVar = BooleanVar(true)
    
    override fun keyDown(keycode: Int): Boolean {
        return if (enabled.get()) {
            delegate.keyDown(keycode)
        } else false
    }

    override fun keyUp(keycode: Int): Boolean {
        return if (enabled.get()) {
            delegate.keyUp(keycode)
        } else false
    }

    override fun keyTyped(character: Char): Boolean {
        return if (enabled.get()) {
            delegate.keyTyped(character)
        } else false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return if (enabled.get()) {
            delegate.touchDown(screenX, screenY, pointer, button)
        } else false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return if (enabled.get()) {
            delegate.touchUp(screenX, screenY, pointer, button)
        } else false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return if (enabled.get()) {
            delegate.touchDragged(screenX, screenY, pointer)
        } else false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return if (enabled.get()) {
            delegate.mouseMoved(screenX, screenY)
        } else false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return if (enabled.get()) {
            delegate.scrolled(amountX, amountY)
        } else false
    }
}
