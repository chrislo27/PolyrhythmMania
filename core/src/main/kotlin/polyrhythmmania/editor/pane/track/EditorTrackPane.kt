package polyrhythmmania.editor.pane.track

import io.github.chrislo27.paintbox.ui.Pane
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.editor.track.Track


/**
 * An [EditorTrackPane] is a track pane that directly represents a [Track] in the editor.
 */
class EditorTrackPane(val allTracksPane: AllTracksPane, val trackID: String)
    : Pane() {

    val editorPane: EditorPane = allTracksPane.editorPane
    val trackView: TrackView = allTracksPane.trackView
    val editor: Editor = editorPane.editor
    
    val track: Track = editor.tracks[trackID] ?: error("No track found in the editor with id \"$trackID\"")
    val sidePanel: SidePanel
    
    init {
        sidePanel = SidePanel(editorPane)
        this += sidePanel
    }
}