package paintbox.ui.control

import com.badlogic.gdx.Input
import paintbox.binding.BooleanVar
import paintbox.binding.ReadOnlyBooleanVar
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
    val disabled: BooleanVar = BooleanVar(false)

    /**
     * If true, this [Control] is disabled and will not interact.
     * This takes into account the parent's [apparentDisabledState].
     */
    val apparentDisabledState: ReadOnlyBooleanVar = BooleanVar {
        disabled.useB() || (parent.use()?.let { parent ->
            if (parent is Control<*>) {
                parent.apparentDisabledState.useB()
            } else false
        } ?: false)
    }

    final override val isHoveredOver: ReadOnlyBooleanVar = BooleanVar(false)
    final override val isPressedDown: ReadOnlyBooleanVar = BooleanVar(false)
    final override val pressedState: ReadOnlyVar<PressedState> = HasPressedState.createDefaultPressedStateVar(isHoveredOver, isPressedDown)

    init {
        addInputEventListener { event ->
            when (event) {
                is MouseEntered -> {
                    (isHoveredOver as BooleanVar).set(true)
                }
                is MouseExited -> {
                    (isHoveredOver as BooleanVar).set(false)
                }
            }
            if (!apparentDisabledState.get()) {
                when (event) {
                    is ClickPressed -> {
                        if (event.button == Input.Buttons.LEFT) {
                            (isPressedDown as BooleanVar).set(true)
                        }
                    }
                    is ClickReleased -> {
                        if (event.button == Input.Buttons.LEFT && isPressedDown.get()) {
                            (isPressedDown as BooleanVar).set(false)
                        }
                    }
                }
            }
            false
        }

        addInputEventListener { event ->
            if (!apparentDisabledState.get()) {
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
        return !apparentDisabledState.get() && onAction()
    }

}
