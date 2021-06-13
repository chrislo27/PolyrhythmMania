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