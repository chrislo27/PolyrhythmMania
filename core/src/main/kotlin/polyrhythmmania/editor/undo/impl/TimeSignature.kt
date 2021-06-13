package polyrhythmmania.editor.undo.impl

import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.undo.ReversibleAction
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.timesignature.TimeSignature


class DeleteTimeSignatureAction(val toDelete: TimeSignature)
    : ReversibleAction<Editor> {

    override fun redo(context: Editor) {
        val newList = context.timeSignatures.getOrCompute().toMutableList()
        newList.remove(toDelete)
        context.timeSignatures.set(newList)
        context.compileEditorTimeSignatures()
    }

    override fun undo(context: Editor) {
        val newList = context.timeSignatures.getOrCompute().toMutableList()
        newList.add(toDelete)
        newList.sortBy { it.beat }
        context.timeSignatures.set(newList)
        context.compileEditorTimeSignatures()
    }
}

class AddTimeSignatureAction(val toAdd: TimeSignature)
    : ReversibleAction<Editor> {

    override fun redo(context: Editor) {
        val newList = context.timeSignatures.getOrCompute().toMutableList()
        newList.add(toAdd)
        newList.sortBy { it.beat }
        context.timeSignatures.set(newList)
        context.compileEditorTimeSignatures()
    }

    override fun undo(context: Editor) {
        val newList = context.timeSignatures.getOrCompute().toMutableList()
        newList.remove(toAdd)
        context.timeSignatures.set(newList)
        context.compileEditorTimeSignatures()
    }
}

class ChangeTimeSignatureAction(val previous: TimeSignature, var next: TimeSignature)
    : ReversibleAction<Editor> {

    override fun redo(context: Editor) {
        val newList = context.timeSignatures.getOrCompute().toMutableList()
        newList.remove(previous)
        newList.add(next)
        context.timeSignatures.set(newList)
        context.compileEditorTimeSignatures()
    }

    override fun undo(context: Editor) {
        val newList = context.timeSignatures.getOrCompute().toMutableList()
        newList.remove(next)
        newList.add(previous)
        context.timeSignatures.set(newList)
        context.compileEditorTimeSignatures()
    }
}