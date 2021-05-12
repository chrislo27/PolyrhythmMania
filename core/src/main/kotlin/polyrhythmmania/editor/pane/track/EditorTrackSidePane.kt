package polyrhythmmania.editor.pane.track

import io.github.chrislo27.paintbox.ui.Pane
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.editor.Track


/**
 * An [EditorTrackSidePane] is a track pane that directly represents a [Track] in the editor.
 */
class EditorTrackSidePane(val allTracksPane: AllTracksPane, val trackID: String)
    : Pane() {

    val editorPane: EditorPane = allTracksPane.editorPane
    val trackView: TrackView = allTracksPane.trackView
    val editor: Editor = editorPane.editor
    
    val track: Track = editor.trackMap[trackID] ?: error("No track found in the editor with id \"$trackID\"")
    val sidePanel: SidePanel
    
    init {
        sidePanel = SidePanel(editorPane)
        this += sidePanel
        
        this.sidePanel.titleLabel.markup.set(editorPane.palette.markupBordered)
        this.sidePanel.titleText.bind { Localization.getVar("editor.track.${trackID}").use() }
    }
}