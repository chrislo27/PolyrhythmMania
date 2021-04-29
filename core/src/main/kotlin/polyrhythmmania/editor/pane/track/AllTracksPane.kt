package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.graphics.Color
import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.font.TextBlock
import io.github.chrislo27.paintbox.font.TextRun
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.element.RectElement
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.pane.EditorPane

class AllTracksPane(val editorPane: EditorPane) : Pane() {
    
    val trackView: TrackView = editorPane.editor.trackView
    
    val sidebarWidth: FloatVar = FloatVar(200f)
    
    val tracks: List<TrackPane>
    val beatTrack: BeatTrack
    val tempoTrack: TempoTrack
    
    init {
        tracks = mutableListOf()

        beatTrack = BeatTrack(this)
        tracks += beatTrack
        tempoTrack = TempoTrack(this)
        tracks += tempoTrack
        
        val TRACK_COLOURS: List<Color> = (0 until 8).map { Color(1f, 1f, 1f, 1f).fromHsv((it * 3f / 8f * 360f) % 360f, 2 / 3f, 0.75f) }
        tracks += TrackPane(this).apply { 
            this.bounds.height.set(64f)
            this.sidebarBgColor.set(Color().set(TRACK_COLOURS[0]))
            this.titleText.set("A side")
            this.titleLabel.internalTextBlock.set(TextBlock(listOf(
//                    TextRun(editorPane.main.fontRodinBordered, "\uE0A0"),
                    TextRun(editorPane.main.fontRodinBordered, "\uE0E0"),
                    TextRun(editorPane.main.mainFontBordered, " side"),
            )))
        }
        tracks += TrackPane(this).apply {
            this.bounds.height.set(64f)
            this.sidebarBgColor.set(Color().set(TRACK_COLOURS[1]))
            this.titleText.set("+ side")
            this.titleLabel.internalTextBlock.set(TextBlock(listOf(
//                    TextRun(editorPane.main.fontRodinBordered, "\uE0D0"),
                    TextRun(editorPane.main.fontRodinBordered, "\uE110"),
                    TextRun(editorPane.main.mainFontBordered, " side"),
            )))
        }

        var totalHeight = 0f
        for (trackPane in tracks) {
            trackPane.bindWidthToParent()
            trackPane.bounds.y.set(totalHeight)
            totalHeight += trackPane.bounds.height.getOrCompute()
            this += trackPane
        }
    }
}