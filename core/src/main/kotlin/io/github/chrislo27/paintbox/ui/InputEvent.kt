package io.github.chrislo27.paintbox.ui



fun interface InputEventListener {
    fun handle(event: InputEvent): Boolean
}

open class InputEvent

class KeyDown(val key: Int) : InputEvent()
class KeyUp(val key: Int) : InputEvent()
class KeyTyped(val character: Char) : InputEvent()
class MouseMoved(val x: Float, val y: Float) : InputEvent()
class Scrolled(val amountX: Float, val amountY: Float) : InputEvent()

/**
 * Represents the gdx touchDown event in InputProcessor
 */
class TouchDown(val x: Float, val y: Float, val button: Int, val pointer: Int) : InputEvent()

/**
 * Represents the gdx touchUp event in InputProcessor
 */
class TouchUp(val x: Float, val y: Float, val button: Int, val pointer: Int) : InputEvent()

/**
 * Represents the gdx touchDragged event in InputProcessor
 */
class TouchDragged(val x: Float, val y: Float, val pointer: Int) : InputEvent()

/**
 * Fired when the mouse enters this UI element
 */
class MouseEntered(val x: Float, val y: Float) : InputEvent()

/**
 * Fired when the mouse exits this UI element
 */
class MouseExited(val x: Float, val y: Float) : InputEvent()

/**
 * Called when a mouse button is pressed on this element.
 */
class ClickPressed(val x: Float, val y: Float, val button: Int) : InputEvent()

/**
 * Called when a mouse button is released on this element, having previously received the [ClickPressed]
 * event. [consumedPrior] will be true if this element previously consumed the [ClickPressed] event.
 */
class ClickReleased(val x: Float, val y: Float, val button: Int, val consumedPrior: Boolean,
                    val isWithinBounds: Boolean) : InputEvent()
