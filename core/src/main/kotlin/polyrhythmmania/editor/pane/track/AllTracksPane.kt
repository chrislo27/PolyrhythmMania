package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.graphics.Color
import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.font.TextBlock
import io.github.chrislo27.paintbox.font.TextRun
import io.github.chrislo27.paintbox.ui.Pane
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.pane.EditorPane

class AllTracksPane(val editorPane: EditorPane) : Pane() {
    
    val trackView: TrackView = editorPane.editor.trackView
    
    val sidebarWidth: FloatVar = FloatVar(200f)
    val editorTrackHeight: Float = 64f
    
    val beatTrack: BeatTrack
    val tempoTrack: TempoTrack
    val editorTrackSides: List<EditorTrackSidePane>
    
    val editorTrackArea: EditorTrackArea
    
    init {
        editorTrackSides = mutableListOf()

        val tracks = mutableListOf<LongTrackPane>()
        beatTrack = BeatTrack(this)
        tracks += beatTrack
        tempoTrack = TempoTrack(this)
        tracks += tempoTrack
        
        val trackColours: List<Color> = (0 until 8).map { Color(1f, 1f, 1f, 1f).fromHsv((it * 3f / 8f * 360f) % 360f, 2 / 3f, 0.75f) }
        editorTrackSides += EditorTrackSidePane(this, Editor.TRACK_INPUT_A).apply {
            this.sidePanel.sidebarBgColor.set(Color().set(trackColours[0]))
            this.sidePanel.titleText.set("A side")
            this.sidePanel.titleLabel.internalTextBlock.set(TextBlock(listOf(
//                    TextRun(editorPane.main.fontRodinBordered, "\uE0A0"),
                    TextRun(editorPane.main.fontRodinBordered, "\uE0E0"),
                    TextRun(editorPane.main.mainFontBordered, " side"),
            )))
        }
        editorTrackSides += EditorTrackSidePane(this, Editor.TRACK_INPUT_DPAD).apply {
            this.sidePanel.sidebarBgColor.set(Color().set(trackColours[1]))
            this.sidePanel.titleText.set("+ side")
            this.sidePanel.titleLabel.internalTextBlock.set(TextBlock(listOf(
//                    TextRun(editorPane.main.fontRodinBordered, "\uE0D0"),
                    TextRun(editorPane.main.fontRodinBordered, "\uE110"),
                    TextRun(editorPane.main.mainFontBordered, " side"),
            )))
        }
        editorTrackSides += EditorTrackSidePane(this, Editor.TRACK_VFX0).apply {
            this.sidePanel.sidebarBgColor.set(Color().set(trackColours[2]))
            this.sidePanel.titleText.set("VFX 0")
        }
        
        editorTrackArea = EditorTrackArea(this)

        var totalHeight = 0f
        var firstTrackPane = true
        for (trackPane in tracks) {
            trackPane.bindWidthToParent()
            trackPane.bounds.y.set(totalHeight)
            totalHeight += trackPane.bounds.height.getOrCompute()
            this += trackPane
            if (firstTrackPane) {
                firstTrackPane = false
                val b = trackPane.contentSection.bounds
                editorTrackArea.bounds.x.set(b.x.getOrCompute())
                editorTrackArea.bounds.width.bind {
                    (trackPane.contentZone.width.use()) - sidebarWidth.use()
                }
            }
        }
        val trackAreaStart = totalHeight
        editorTrackArea.bounds.y.set(trackAreaStart)
        for (editorTrackPane in editorTrackSides) {
            editorTrackPane.bounds.height.set(editorTrackHeight)
            editorTrackPane.bounds.width.bind { sidebarWidth.use() }
            editorTrackPane.bounds.y.set(totalHeight)
            totalHeight += editorTrackPane.bounds.height.getOrCompute()
            this += editorTrackPane
        }
        editorTrackArea.bounds.height.set(totalHeight - trackAreaStart)
        this += editorTrackArea
    }
}