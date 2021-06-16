package paintbox.ui.control

import com.badlogic.gdx.Input
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.ui.*



enum class PressedState(val hovered: Boolean, val pressed: Boolean) {
    NONE(false, false),
    HOVERED(true, false),
    PRESSED(false, true),
    PRESSED_AND_HOVERED(true, true);
}

/**
 * Indicates that the [UIElement] has a [PressedState].
 * It is recommended to use [HasPressedState.DefaultImpl] as a delegation implementation.
 */
interface HasPressedState {
    companion object {
        fun createDefaultPressedStateVar(isHoveredOver: ReadOnlyVar<Boolean>,
                                         isPressedDown: ReadOnlyVar<Boolean>): ReadOnlyVar<PressedState> {
            return Var {
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
        }
    }
    
    val isHoveredOver: ReadOnlyVar<Boolean>
    val isPressedDown: ReadOnlyVar<Boolean>
    val pressedState: ReadOnlyVar<PressedState>

    /**
     * The default implementation of [HasPressedState].
     * Does NOT handle the disabled state of [Control] ([Control] implements [HasPressedState] separately).
     * 
     * The using class should call [addDefaultPressedStateInputListener] to add the appropriate input event listener on init.
     */
    class DefaultImpl : HasPressedState {
        companion object {
            /**
             * A class that delegates its [HasPressedState] to [DefaultImpl] should call this function on init.
             */
            fun <SELF> addDefaultPressedStateInputListener(self: SELF)
                    where SELF : UIElement, SELF : HasPressedState {
                self.addInputEventListener { event ->
                    when (event) {
                        is MouseEntered -> {
                            (self.isHoveredOver as Var).set(true)
                        }
                        is MouseExited -> {
                            (self.isHoveredOver as Var).set(false)
                        }
                        is ClickPressed -> {
                            if (event.button == Input.Buttons.LEFT) {
                                (self.isPressedDown as Var).set(true)
                            }
                        }
                        is ClickReleased -> {
                            if (event.button == Input.Buttons.LEFT && self.isPressedDown.getOrCompute()) {
                                (self.isPressedDown as Var).set(false)
                            }
                        }
                    }
                    false
                }
            }
        }
        
        override val isHoveredOver: ReadOnlyVar<Boolean> = Var(false)
        override val isPressedDown: ReadOnlyVar<Boolean> = Var(false)
        override val pressedState: ReadOnlyVar<PressedState> = HasPressedState.createDefaultPressedStateVar(isHoveredOver, isPressedDown)
        
    }
}
