package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.graphics.Color
import paintbox.binding.FloatVar
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.ui.PRManiaSkins

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
                    (trackPane.contentZone.width.use()) - sidebarWidth.use()
                }
            }
        }
        val trackAreaStart = totalHeight
        
        val trackAreaScrollPane = ScrollPane().apply {
            this.vBar.skinID.set(PRManiaSkins.EDITOR_SCROLLBAR_SKIN)
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)
            this.vBar.blockIncrement.set(editorTrackHeight)
            this.vBar.unitIncrement.set(editorTrackHeight)
            this.border.set(Insets(0f, 2f, 0f, 0f))
            this.borderStyle.set(SolidBorder(Color.WHITE))
        }
        val trackAreaContent = Pane()
        trackAreaScrollPane.bounds.y.set(trackAreaStart)
        
        var sidesTotalHeight = 0f
        for (editorTrackPane in editorTrackSides) {
            editorTrackPane.bounds.height.set(editorTrackHeight)
            editorTrackPane.bounds.width.bind { sidebarWidth.use() }
            editorTrackPane.bounds.y.set(sidesTotalHeight)
            val h = editorTrackPane.bounds.height.get()
            sidesTotalHeight += h
            totalHeight += h
            trackAreaContent += editorTrackPane
        }
        trackAreaContent += editorTrackArea
        trackAreaContent.bounds.height.set(sidesTotalHeight)
        trackAreaScrollPane.setContent(trackAreaContent)
        this += trackAreaScrollPane


        var bottomTotalHeight = 0f
        for (trackPane in bottomTracks) {
            trackPane.bindWidthToParent()
            val h = trackPane.bounds.height.get()
            bottomTotalHeight += h
            totalHeight += h
            this += trackPane
            if (firstTrackPane) {
                firstTrackPane = false
                val b = trackPane.contentSection.bounds
                editorTrackArea.bounds.x.set(b.x.get())
                editorTrackArea.bounds.width.bind {
                    (trackPane.contentZone.width.use()) - sidebarWidth.use()
                }
            }
        }

        val suggestedMidHeight = totalHeight - trackAreaStart - bottomTotalHeight
        
        
        var bottomTotalHeight2 = 0f
        for (trackPane in bottomTracks) {
            val h = trackPane.bounds.height.get()
            
            val tpParent = trackPane.parent
            val offY = -(bottomTotalHeight - h - bottomTotalHeight2)
            val currentH = bottomTotalHeight2
            trackPane.bounds.y.bind {
                val anchoredBottom = (tpParent.use()?.contentZone?.height?.use() ?: 0f) - (trackPane.bounds.height.use()) + offY
                
                (anchoredBottom).coerceAtMost(trackAreaScrollPane.bounds.y.use() + trackAreaScrollPane.bounds.height.use() + currentH)
            }
            
            bottomTotalHeight2 += h
        }

        trackAreaScrollPane.bounds.height.bind {
            val parentHeight = (trackAreaScrollPane.parent.use()?.bounds?.height?.use() ?: 1f)
            (parentHeight - trackAreaStart - bottomTotalHeight).coerceAtMost(suggestedMidHeight)
        }
    }
}