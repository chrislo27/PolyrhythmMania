package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.graphics.Color
import paintbox.binding.FloatVar
import paintbox.ui.Pane
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.pane.EditorPane

class AllTracksPane(val editorPane: EditorPane) : Pane() {
    
    val trackView: TrackView = editorPane.editor.trackView
    
    val sidebarWidth: FloatVar = FloatVar(200f)
    val editorTrackHeight: Float = 48f
    
    val beatTrack: BeatTrack
    val tempoTrack: TempoTrack
    val musicVolTrack: MusicVolTrack
    val scrubTrack: ScrubTrack
    val editorTrackSides: List<EditorTrackSidePane>
    
    val editorTrackArea: EditorTrackArea
    
    init {
        editorTrackSides = mutableListOf()

        val topTracks = mutableListOf<LongTrackPane>()
        val bottomTracks = mutableListOf<LongTrackPane>()
        beatTrack = BeatTrack(this)
        topTracks += beatTrack
        tempoTrack = TempoTrack(this)
        topTracks += tempoTrack
        musicVolTrack = MusicVolTrack(this)
        bottomTracks += musicVolTrack
        scrubTrack = ScrubTrack(this)
        bottomTracks += scrubTrack
        
        val trackColours: List<Color> = (0 until 8).map { Color(1f, 1f, 1f, 1f).fromHsv((it * 3f / 8f * 360f) % 360f, 2 / 3f, 0.75f) }
        editorPane.editor.tracks.forEachIndexed { index, track ->
            editorTrackSides += EditorTrackSidePane(this, track.id).apply {
                this.sidePanel.sidebarBgColor.set(Color().set(trackColours[index % trackColours.size]))
            }
        }
        
        editorTrackArea = EditorTrackArea(this)

        var totalHeight = 0f
        var firstTrackPane = true
        for (trackPane in topTracks) {
            trackPane.bindWidthToParent()
            trackPane.bounds.y.set(totalHeight)
            totalHeight += trackPane.bounds.height.get()
            this += trackPane
            if (firstTrackPane) {
                firstTrackPane = false
                val b = trackPane.contentSection.bounds
                editorTrackArea.bounds.x.set(b.x.get())
                editorTrackArea.bounds.width.bind {
                    (trackPane.contentZone.width.useF()) - sidebarWidth.useF()
                }
            }
        }
        val trackAreaStart = totalHeight
        editorTrackArea.bounds.y.set(trackAreaStart)
        for (editorTrackPane in editorTrackSides) {
            editorTrackPane.bounds.height.set(editorTrackHeight)
            editorTrackPane.bounds.width.bind { sidebarWidth.useF() }
            editorTrackPane.bounds.y.set(totalHeight)
            totalHeight += editorTrackPane.bounds.height.get()
            this += editorTrackPane
        }
        editorTrackArea.bounds.height.set(totalHeight - trackAreaStart)
        this += editorTrackArea
        for (trackPane in bottomTracks) {
            trackPane.bindWidthToParent()
            trackPane.bounds.y.set(totalHeight)
            totalHeight += trackPane.bounds.height.get()
            this += trackPane
            if (firstTrackPane) {
                firstTrackPane = false
                val b = trackPane.contentSection.bounds
                editorTrackArea.bounds.x.set(b.x.get())
                editorTrackArea.bounds.width.bind {
                    (trackPane.contentZone.width.useF()) - sidebarWidth.useF()
                }
            }
        }


    }
}