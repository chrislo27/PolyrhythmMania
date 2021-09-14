package paintbox.ui.control

import paintbox.ui.Focusable
import paintbox.ui.SceneRoot
import kotlin.math.sign


class FocusGroup {
    
    private val focusables: MutableList<Focusable> = mutableListOf()
    
    fun addFocusable(focusable: Focusable) {
        if (focusable !in focusables) {
            focusables += focusable
            val oldGroup = focusable.focusGroup.getOrCompute()
            oldGroup?.removeFocusable(focusable)
            focusable.focusGroup.set(this)
        }
    }
    
    fun removeFocusable(focusable: Focusable) {
        if (focusable.focusGroup.getOrCompute() == this) {
            focusables -= focusable
            focusable.focusGroup.set(null)
        }
    }
    
    private fun focusFirst(): Focusable {
        return focusables.first().also { f ->
            f.requestFocus()
        }
    }
    
    private fun focus(current: Focusable, dir: Int): Focusable? {
        if (focusables.isEmpty()) return null
        
        val indexOfCurrent = focusables.indexOf(current)
        if (indexOfCurrent == -1 || dir == 0) {
            return focusFirst()
        }

        val size = focusables.size
        val newIndex = (indexOfCurrent + size + dir.sign) % size
        val newFocusable = focusables[newIndex]
        
        newFocusable.requestFocus()
        
        return newFocusable
    }
    
    fun focusNext(current: Focusable): Focusable? {
        return focus(current, 1)
    }
    
    fun focusPrevious(current: Focusable): Focusable? {
        return focus(current, -1)
    }
    
}