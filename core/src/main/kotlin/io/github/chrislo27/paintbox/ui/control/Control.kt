package io.github.chrislo27.paintbox.ui.control

import com.badlogic.gdx.Input
import io.github.chrislo27.paintbox.ui.ClickReleased
import io.github.chrislo27.paintbox.ui.InputEvent
import io.github.chrislo27.paintbox.ui.MouseEntered
import io.github.chrislo27.paintbox.ui.MouseExited
import io.github.chrislo27.paintbox.ui.skin.Skinnable
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var


/**
 * A [Control] represents a (semi-)interactable type of UI element that is also skinnable.
 * There are also utilities to handle common input interactions.
 */
abstract class Control<SELF : Control<SELF>> : Skinnable<SELF>() {

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
    
    var onAction: () -> Boolean = { false }
    var onLeftClick: (event: ClickReleased) -> Boolean = { false }
    var onRightClick: (event: ClickReleased) -> Boolean = { false }
    var onMiddleClick: (event: ClickReleased) -> Boolean = { false }
    var onHoverStart: (event: MouseEntered) -> Boolean = { false }
    var onHoverEnd: (event: MouseExited) -> Boolean = { false }
    
    init {
        addDefaultInputEventListener()
    }
    
    protected open fun defaultInputEventListener(event: InputEvent): Boolean {
        return if (!apparentDisabledState.getOrCompute()) {
            when (event) {
                is ClickReleased -> {
                    if (event.isWithinBounds) {
                        if (event.button == Input.Buttons.LEFT) {
                            if (!onAction()) {
                                onLeftClick(event)
                            } else true
                        } else if (event.button == Input.Buttons.RIGHT) {
                            onRightClick(event)
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
    
    @JvmName("setOnActionUnit")
    inline fun setOnAction(crossinline value: () -> Unit) {
        onAction = {
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
