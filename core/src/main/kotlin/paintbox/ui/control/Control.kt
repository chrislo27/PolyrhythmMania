package paintbox.ui.control

import com.badlogic.gdx.Input
import paintbox.ui.skin.Skinnable
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.ui.*


/**
 * A [Control] represents a (semi-)interactable type of UI element that is also skinnable.
 * There are also utilities to handle common input interactions.
 */
abstract class Control<SELF : Control<SELF>>
    : Skinnable<SELF>(), HasTooltip {

    companion object {
        private val DEFAULT_ACTION: () -> Boolean = { false }
        private val DEFAULT_EVENT_ACTION: (InputEvent) -> Boolean = { false }
    }

    override val tooltipElement: Var<UIElement?> = Var(null)

    /**
     * If true, this [Control] will not interact with any inputs.
     */
    val disabled: Var<Boolean> = Var(false)

    /**
     * If true, this [Control] is disabled and will not interact.
     * This takes into account the parent's [apparentDisabledState].
     */
    val apparentDisabledState: ReadOnlyVar<Boolean> = Var {
        disabled.use() || (parent.use()?.let { parent ->
            if (parent is Control<*>) {
                parent.apparentDisabledState.use()
            } else false
        } ?: false)
    }

    val isHoveredOver: ReadOnlyVar<Boolean> = Var(false)
    val isPressedDown: ReadOnlyVar<Boolean> = Var(false)
    val pressedState: ReadOnlyVar<PressedState> = Var {
        val hovered = isHoveredOver.use()
        val pressed = isPressedDown.use()
        if (hovered && pressed) {
            PressedState.PRESSED_AND_HOVERED
        } else if (hovered) {
            PressedState.HOVERED
        } else if (pressed) {
            PressedState.PRESSED
        } else {
            PressedState.NONE
        }
    }

    var onAction: () -> Boolean = DEFAULT_ACTION
    var onAltAction: () -> Boolean = DEFAULT_ACTION
    var onLeftClick: (event: ClickReleased) -> Boolean = DEFAULT_EVENT_ACTION
    var onRightClick: (event: ClickReleased) -> Boolean = DEFAULT_EVENT_ACTION
    var onMiddleClick: (event: ClickReleased) -> Boolean = DEFAULT_EVENT_ACTION
    var onHoverStart: (event: MouseEntered) -> Boolean = DEFAULT_EVENT_ACTION
    var onHoverEnd: (event: MouseExited) -> Boolean = DEFAULT_EVENT_ACTION

    init {
        addInputEventListener { event ->
            when (event) {
                is MouseEntered -> {
                    (isHoveredOver as Var).set(true)
                }
                is MouseExited -> {
                    (isHoveredOver as Var).set(false)
                }
            }
            if (!apparentDisabledState.getOrCompute()) {
                when (event) {
                    is ClickPressed -> {
                        if (event.button == Input.Buttons.LEFT) {
                            (isPressedDown as Var).set(true)
                        }
                    }
                    is ClickReleased -> {
                        if (event.button == Input.Buttons.LEFT && isPressedDown.getOrCompute()) {
                            (isPressedDown as Var).set(false)
                        }
                    }
                }
            }
            false
        }
        @Suppress("LeakingThis")
        addDefaultInputEventListener()
    }

    protected open fun defaultInputEventListener(event: InputEvent): Boolean {
        return if (!apparentDisabledState.getOrCompute()) {
            when (event) {
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
        } else {
            false
        }
    }

    protected open fun addDefaultInputEventListener() {
        addInputEventListener { event ->
            defaultInputEventListener(event)
        }
    }

    /**
     * Attempts to trigger the [onAction] value so long if this [Control] is not [disabled][apparentDisabledState].
     */
    fun triggerAction(): Boolean {
        return !apparentDisabledState.getOrCompute() && onAction()
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

    enum class PressedState(val hovered: Boolean, val pressed: Boolean) {
        NONE(false, false),
        HOVERED(true, false),
        PRESSED(false, true),
        PRESSED_AND_HOVERED(true, true);
    }
}
