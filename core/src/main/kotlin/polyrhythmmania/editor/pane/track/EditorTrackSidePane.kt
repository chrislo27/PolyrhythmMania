package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.graphics.Color
import paintbox.ui.Pane
import polyrhythmmania.Localization
import polyrhythmmania.editor.*
import polyrhythmmania.editor.pane.EditorPane


/**
 * An [EditorTrackSidePane] is a track pane that directly represents a [Track] in the editor.
 */
class EditorTrackSidePane(val allTracksPane: AllTracksPane, val trackID: TrackID)
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
        this.sidePanel.titleText.bind { Localization.getVar("editor.track.${trackID.id}").use() }
        this.sidePanel.titleLabel.textColor.bind { 
            // Grey out when it is a disallowed track for the active instantiator OR drag
            val allowed = Color.WHITE
            val disallowed = Color.LIGHT_GRAY

            val instantiatorList = editorPane.upperPane.instantiatorPane.instantiatorList
            val allowedTracks = instantiatorList.currentItem.use().allowedTracks
            val thisTrack = editor.trackMap[trackID]
            val thisTrackTypes = thisTrack?.allowedTypes ?: emptySet()
            
            val click = editor.click.use()
            if (click is Click.DragSelection) {
                if (!click.tracksThatWillAccept.any { t -> t.allowedTypes.any { bt -> bt in thisTrackTypes } }) {
                    disallowed
                } else allowed
            } else {
                if (allowedTracks != null && !allowedTracks.any { bt -> bt in thisTrackTypes }) {
                    disallowed
                } else allowed
            }
        }
    }
}