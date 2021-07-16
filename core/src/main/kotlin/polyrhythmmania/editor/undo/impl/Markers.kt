package polyrhythmmania.editor.undo.impl

import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.MarkerType
import polyrhythmmania.editor.undo.ReversibleAction


class MoveMarkerAction(val marker: MarkerType, val previous: Float, var next: Float)
    : ReversibleAction<Editor> {
    
    override fun redo(context: Editor) {
        context.markerMap.getValue(marker).beat.set(next)
        if (marker == MarkerType.MUSIC_FIRST_BEAT) {
            context.compileEditorMusicInfo()
        }
    }

    override fun undo(context: Editor) {
        context.markerMap.getValue(marker).beat.set(previous)
        if (marker == MarkerType.MUSIC_FIRST_BEAT) {
            context.compileEditorMusicInfo()
        }
    }
}
