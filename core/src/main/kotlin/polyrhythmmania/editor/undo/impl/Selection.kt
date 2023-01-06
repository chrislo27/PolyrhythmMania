package polyrhythmmania.editor.undo.impl

import polyrhythmmania.editor.Click
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.undo.ReversibleAction


class SelectionAction(val previousSelection: Set<Block>, val newSelection: Set<Block>)
    : ReversibleAction<Editor> {
    
    override fun redo(context: Editor) {
        context.clearBlockSelection()
        newSelection.forEach(context::addBlockSelection)
    }

    override fun undo(context: Editor) {
        context.clearBlockSelection()
        previousSelection.forEach(context::addBlockSelection)
    }
}

class DeletionAction(val blocksToDelete: List<Block>)
    : ReversibleAction<Editor> {
    override fun redo(context: Editor) {
        context.removeBlocks(blocksToDelete)
    }

    override fun undo(context: Editor) {
        context.addBlocks(blocksToDelete)
    }
}

class PlaceAction(val blocksToAdd: List<Block>)
    : ReversibleAction<Editor> {
    override fun redo(context: Editor) {
        context.addBlocks(blocksToAdd)
    }

    override fun undo(context: Editor) {
        context.removeBlocks(blocksToAdd)
    }
}

class MoveAction(val blocks: Map<Block, Pos>) 
    : ReversibleAction<Editor> {
    data class Pos(val previous: Click.DragSelection.BlockRegion, val next: Click.DragSelection.BlockRegion)

    override fun redo(context: Editor) {
        blocks.forEach { (block, pos) -> 
            block.beat = pos.next.beat
            block.trackIndex = pos.next.track
        }
    }

    override fun undo(context: Editor) {
        blocks.forEach { (block, pos) ->
            block.beat = pos.previous.beat
            block.trackIndex = pos.previous.track
        }
    }
}