package paintbox.ui

import com.badlogic.gdx.Input


open class ActionablePane : Pane() {
    companion object {
        val DEFAULT_ACTION: () -> Boolean = { false }
        val DEFAULT_EVENT_ACTION: (InputEvent) -> Boolean = { false }
    }

    var onAction: () -> Boolean = DEFAULT_ACTION
    var onAltAction: () -> Boolean = DEFAULT_ACTION
    var onLeftClick: (event: ClickReleased) -> Boolean = DEFAULT_EVENT_ACTION
    var onRightClick: (event: ClickReleased) -> Boolean = DEFAULT_EVENT_ACTION
    var onMiddleClick: (event: ClickReleased) -> Boolean = DEFAULT_EVENT_ACTION
    var onHoverStart: (event: MouseEntered) -> Boolean = DEFAULT_EVENT_ACTION
    var onHoverEnd: (event: MouseExited) -> Boolean = DEFAULT_EVENT_ACTION


    init {
        @Suppress("LeakingThis")
        addDefaultInputEventListener()
    }

    protected open fun defaultInputEventHandler(event: InputEvent): Boolean {
        return when (event) {
                is ClickReleased -> {
                    if (event.isCurrentlyWithinBounds) {
                        if (event.button == Input.Buttons.LEFT) {
                            if (!onAction()) {
                                onLeftClick(event)
                            } else true
                        } else if (event.button == Input.Buttons.RIGHT) {
                            if (!onAltAction()) {
                                onRightClick(event)
                            } else true
                        } else if (event.button == Input.Buttons.MIDDLE) {
                            onMiddleClick(event)
                        } else false
                    } else false
                }
                is MouseEntered -> {
                    onHoverStart(event)
                }
                is MouseExited -> {
                    onHoverEnd(event)
                }
                else -> false
            }
    }

    protected open fun addDefaultInputEventListener() {
        addInputEventListener { event ->
            defaultInputEventHandler(event)
        }
    }


    @JvmName("setOnActionUnit")
    inline fun setOnAction(crossinline value: () -> Unit) {
        onAction = {
            value()
            true
        }
    }

    @JvmName("setOnAltActionUnit")
    inline fun setOnAltAction(crossinline value: () -> Unit) {
        onAltAction = {
            value()
            true
        }
    }

    @JvmName("setOnLeftClickUnit")
    inline fun setOnLeftClick(crossinline value: (event: ClickReleased) -> Unit) {
        onLeftClick = {
            value(it)
            true
        }
    }

    @JvmName("setOnRightClickUnit")
    inline fun setOnRightClick(crossinline value: (event: ClickReleased) -> Unit) {
        onRightClick = {
            value(it)
            true
        }
    }

    @JvmName("setOnMiddleClickUnit")
    inline fun setOnMiddleClick(crossinline value: (event: ClickReleased) -> Unit) {
        onMiddleClick = {
            value(it)
            true
        }
    }

    @JvmName("setOnHoverStartUnit")
    inline fun setOnHoverStart(crossinline value: (event: MouseEntered) -> Unit) {
        onHoverStart = {
            value(it)
            true
        }
    }

    @JvmName("setOnHoverEndUnit")
    inline fun setOnHoverEnd(crossinline value: (event: MouseExited) -> Unit) {
        onHoverEnd = {
            value(it)
            true
        }
    }
}