package polyrhythmmania.editor.pane.track

import polyrhythmmania.editor.track.Track


class EditorTrackPane(allTracksPane: AllTracksPane, val trackID: String)
    : TrackPane(allTracksPane) {
    
    val track: Track = editor.tracks.getValue(trackID)
    
}