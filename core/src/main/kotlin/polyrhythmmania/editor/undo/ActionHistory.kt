package polyrhythmmania.editor.undo

import paintbox.binding.IntVar
import paintbox.binding.ReadOnlyIntVar
import java.util.*

/**
 * Supports undoing and redoing on this instance.
 * @param maxItems Max items, <= 0 is infinite
 * @param SELF The real impl
 */
@Suppress("UNCHECKED_CAST")
open class ActionHistory<SELF : ActionHistory<SELF>>(val maxItems: Int = 128) {

    protected open fun createDeque(): Deque<ReversibleAction<SELF>> {
        return ArrayDeque()
    }

    private val undos: Deque<ReversibleAction<SELF>> by lazy { createDeque() }
    private val redos: Deque<ReversibleAction<SELF>> by lazy { createDeque() }
    
    protected val internalUndoStackSize: IntVar = IntVar(0)
    protected val internalRedoStackSize: IntVar = IntVar(0)
    val undoStackSize: ReadOnlyIntVar = internalUndoStackSize
    val redoStackSize: ReadOnlyIntVar = internalRedoStackSize

    /**
     * Mutate this object, adding the action on the undo stack, and clears all redos.
     */
    fun mutate(action: ReversibleAction<SELF>) {
        addActionWithoutMutating(action)

        action.redo(this as SELF)
        ensureCapacity()
    }

    /**
     * Adds an action without calling the redo method of the action. Redos are cleared.
     */
    fun addActionWithoutMutating(action: ReversibleAction<SELF>) {
        redos.clear()
        undos.push(action)
        ensureCapacity()
        internalUndoStackSize.set(undos.size)
        internalRedoStackSize.set(redos.size)
    }

    fun ensureCapacity() {
        if (maxItems > 0) {
            if (undos.size > maxItems) {
                undos.removeLast()
            }
            if (redos.size > maxItems) {
                redos.removeLast()
            }
        }
    }

    fun undo(): Boolean {
        if (!canUndo()) return false

        val action = undos.pop()
        action.undo(this as SELF)

        redos.push(action)
        ensureCapacity()
        internalUndoStackSize.set(undos.size)
        internalRedoStackSize.set(redos.size)

        return true
    }

    fun redo(): Boolean {
        if (!canRedo()) return false

        val action = redos.pop()
        action.redo(this as SELF)

        undos.push(action)
        ensureCapacity()
        internalUndoStackSize.set(undos.size)
        internalRedoStackSize.set(redos.size)

        return true
    }

    fun canUndo(): Boolean = undos.size > 0

    fun canRedo(): Boolean = redos.size > 0

    fun clear() {
        undos.clear()
        redos.clear()
        internalUndoStackSize.set(0)
        internalRedoStackSize.set(0)
    }
    
    fun peekAtUndoStack(): ReversibleAction<SELF>? = undos.peekFirst()

}
