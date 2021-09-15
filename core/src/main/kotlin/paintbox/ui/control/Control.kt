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
    : Skinnable<SELF>(), HasTooltip, HasPressedState {

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

    final override val isHoveredOver: ReadOnlyVar<Boolean> = Var(false)
    final override val isPressedDown: ReadOnlyVar<Boolean> = Var(false)
    final override val pressedState: ReadOnlyVar<PressedState> = HasPressedState.createDefaultPressedStateVar(isHoveredOver, isPressedDown)

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

        addInputEventListener { event ->
            if (!apparentDisabledState.getOrCompute()) {
                defaultInputEventHandler(event)
            } else false
        }
    }

    override fun addDefaultInputEventListener() {
        // NO-OP
    }

    /**
     * Attempts to trigger the [onAction] value so long if this [Control] is not [disabled][apparentDisabledState].
     */
    fun triggerAction(): Boolean {
        return !apparentDisabledState.getOrCompute() && onAction()
    }

}
