package paintbox.ui

import paintbox.binding.ReadOnlyBooleanVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.ui.control.FocusGroup


/**
 * To be implemented by a [UIElement] to show it is focusable. Only one element can have focus at any point,
 * tracked by the SceneRoot.
 */
interface Focusable {
    
    val hasFocus: ReadOnlyBooleanVar
    
    /**
     * The [FocusGroup] that this [Focusable] belongs to.
     *
     * This should not be set. In order to register a [Focusable] to a [FocusGroup], use [FocusGroup.addFocusable].
     */
    val focusGroup: Var<FocusGroup?>
    
    fun onFocusGained() {
    }
    
    fun onFocusLost() {
    }
    
    fun requestFocus() {
        if (this is UIElement) {
            val root = this.sceneRoot.getOrCompute()
            root?.setFocusedElement(this)
        }
    }
    
    fun requestUnfocus() {
        if (this is UIElement) {
            val root = this.sceneRoot.getOrCompute()
            if (root != null) {
                val currentFocus = root.currentFocusedElement.getOrCompute()
                if (currentFocus === this) {
                    root.setFocusedElement(null)
                }
            }
        }
    }
}