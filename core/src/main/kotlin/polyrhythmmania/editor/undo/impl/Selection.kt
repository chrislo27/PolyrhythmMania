package polyrhythmmania.editor.undo.impl

import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.track.block.Block
import polyrhythmmania.editor.undo.ReversibleAction


class SelectionAction(val previousSelection: Set<Block>, val newSelection: Set<Block>)
    : ReversibleAction<Editor> {
    
    override fun redo(context: Editor) {
        val selection = context.selectedBlocks as MutableMap
        selection.clear()
        newSelection.forEach { selection[it] = true }
    }

    override fun undo(context: Editor) {
        val selection = context.selectedBlocks as MutableMap
        selection.clear()
        previousSelection.forEach { selection[it] = true }
    }
}