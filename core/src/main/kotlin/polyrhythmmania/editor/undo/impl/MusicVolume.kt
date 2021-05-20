package polyrhythmmania.editor.undo.impl

import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.undo.ReversibleAction
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.TempoChange


class DeleteMusicVolumeAction(val toDelete: MusicVolume)
    : ReversibleAction<Editor> {

    override fun redo(context: Editor) {
        val newList = context.musicVolumes.getOrCompute().toMutableList()
        newList.remove(toDelete)
        context.musicVolumes.set(newList)
        context.compileEditorMusicVols()
    }

    override fun undo(context: Editor) {
        val newList = context.musicVolumes.getOrCompute().toMutableList()
        newList.add(toDelete)
        newList.sortBy { it.beat }
        context.musicVolumes.set(newList)
        context.compileEditorMusicVols()
    }
}

class AddMusicVolumeAction(val toAdd: MusicVolume)
    : ReversibleAction<Editor> {

    override fun redo(context: Editor) {
        val newList = context.musicVolumes.getOrCompute().toMutableList()
        newList.add(toAdd)
        newList.sortBy { it.beat }
        context.musicVolumes.set(newList)
        context.compileEditorMusicVols()
    }

    override fun undo(context: Editor) {
        val newList = context.musicVolumes.getOrCompute().toMutableList()
        newList.remove(toAdd)
        context.musicVolumes.set(newList)
        context.compileEditorMusicVols()
    }
}

class ChangeMusicVolumeAction(val previous: MusicVolume, var next: MusicVolume)
    : ReversibleAction<Editor> {

    override fun redo(context: Editor) {
        val newList = context.musicVolumes.getOrCompute().toMutableList()
        newList.remove(previous)
        newList.add(next)
        context.musicVolumes.set(newList)
        context.compileEditorMusicVols()
    }

    override fun undo(context: Editor) {
        val newList = context.musicVolumes.getOrCompute().toMutableList()
        newList.remove(next)
        newList.add(previous)
        context.musicVolumes.set(newList)
        context.compileEditorMusicVols()
    }
}