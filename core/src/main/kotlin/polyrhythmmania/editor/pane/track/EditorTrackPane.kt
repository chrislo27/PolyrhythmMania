package polyrhythmmania.editor.pane.track

import polyrhythmmania.editor.track.Track


/**
 * An [EditorTrackPane] is a track pane that directly represents a [Track] in the editor.
 */
class EditorTrackPane(allTracksPane: AllTracksPane, val trackID: String)
    : TrackPane(allTracksPane) {
    
    val track: Track = editor.tracks.getValue(trackID)
    val verticalBeatLinesPane: VerticalBeatLinesPane = VerticalBeatLinesPane(editorPane)
    
    init {
        this.contentSection += verticalBeatLinesPane
    }
    
}