package paintbox.ui


/**
 * To be implemented by a [UIElement] to show it is focusable. Only one element can have focus at any point,
 * tracked by the SceneRoot.
 */
interface Focusable {
    
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
}