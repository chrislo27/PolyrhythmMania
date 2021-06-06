package paintbox.ui



fun interface InputEventListener {
    fun handle(event: InputEvent): Boolean
}

open class InputEvent

open class MouseInputEvent(val x: Float, val y: Float) : InputEvent()

class KeyDown(val key: Int) : InputEvent()
class KeyUp(val key: Int) : InputEvent()
class KeyTyped(val character: Char) : InputEvent()
class MouseMoved(x: Float, y: Float) : MouseInputEvent(x, y)
class Scrolled(val amountX: Float, val amountY: Float) : InputEvent()

/**
 * Represents the gdx touchDown event in InputProcessor
 */
class TouchDown(x: Float, y: Float, val button: Int, val pointer: Int) : MouseInputEvent(x, y)

/**
 * Represents the gdx touchUp event in InputProcessor
 */
class TouchUp(x: Float, y: Float, val button: Int, val pointer: Int) : MouseInputEvent(x, y)

/**
 * Represents the gdx touchDragged event in InputProcessor
 */
class TouchDragged(x: Float, y: Float, val pointer: Int,
                   val isCurrentlyWithinBounds: Boolean) : MouseInputEvent(x, y)

/**
 * Fired when the mouse enters this UI element
 */
class MouseEntered(x: Float, y: Float) : MouseInputEvent(x, y)

/**
 * Fired when the mouse exits this UI element
 */
class MouseExited(x: Float, y: Float) : MouseInputEvent(x, y)

/**
 * Called when a mouse button is pressed on this element.
 */
class ClickPressed(x: Float, y: Float, val button: Int) : MouseInputEvent(x, y)

/**
 * Called when a mouse button is released on this element, having previously received the [ClickPressed]
 * event. [consumedPrior] will be true if this element previously consumed the [ClickPressed] event.
 */
class ClickReleased(x: Float, y: Float, val button: Int, val consumedPrior: Boolean,
                    val wasWithinBounds: Boolean, val isCurrentlyWithinBounds: Boolean) : MouseInputEvent(x, y)
