package polyrhythmmania.editor.undo.impl

import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.undo.ReversibleAction
import polyrhythmmania.engine.tempo.TempoChange


class MoveTempoChangeAction(val previous: TempoChange, val next: TempoChange)
    : ReversibleAction<Editor> {

    override fun redo(context: Editor) {
        val newList = context.tempoChanges.getOrCompute().toMutableList()
        newList.remove(previous)
        newList.add(next)
        context.tempoChanges.set(newList)
        context.compileEditorTempos()
    }

    override fun undo(context: Editor) {
        val newList = context.tempoChanges.getOrCompute().toMutableList()
        newList.remove(next)
        newList.add(previous)
        context.tempoChanges.set(newList)
        context.compileEditorTempos()
    }
}

class DeleteTempoChangeAction(val toDelete: TempoChange)
    : ReversibleAction<Editor> {

    override fun redo(context: Editor) {
        val newList = context.tempoChanges.getOrCompute().toMutableList()
        newList.remove(toDelete)
        context.tempoChanges.set(newList)
        context.compileEditorTempos()
    }

    override fun undo(context: Editor) {
        val newList = context.tempoChanges.getOrCompute().toMutableList()
        newList.add(toDelete)
        newList.sortBy { it.beat }
        context.tempoChanges.set(newList)
        context.compileEditorTempos()
    }
}

class AddTempoChangeAction(val toAdd: TempoChange)
    : ReversibleAction<Editor> {

    override fun redo(context: Editor) {
        val newList = context.tempoChanges.getOrCompute().toMutableList()
        newList.add(toAdd)
        newList.sortBy { it.beat }
        context.tempoChanges.set(newList)
        context.compileEditorTempos()
    }

    override fun undo(context: Editor) {
        val newList = context.tempoChanges.getOrCompute().toMutableList()
        newList.remove(toAdd)
        context.tempoChanges.set(newList)
        context.compileEditorTempos()
    }
}

class ChangeTempoChangeAction(val previous: TempoChange, var next: TempoChange)
    : ReversibleAction<Editor> {

    override fun redo(context: Editor) {
        val newList = context.tempoChanges.getOrCompute().toMutableList()
        newList.remove(previous)
        newList.add(next)
        context.tempoChanges.set(newList)
        context.compileEditorTempos()
    }

    override fun undo(context: Editor) {
        val newList = context.tempoChanges.getOrCompute().toMutableList()
        newList.remove(next)
        newList.add(previous)
        context.tempoChanges.set(newList)
        context.compileEditorTempos()
    }
}

class ChangeStartingTempoAction(val previous: Float, var next: Float)
    : ReversibleAction<Editor> {

    override fun redo(context: Editor) {
        context.startingTempo.set(next)
        context.compileEditorTempos()
    }

    override fun undo(context: Editor) {
        context.startingTempo.set(previous)
        context.compileEditorTempos()
    }
}
