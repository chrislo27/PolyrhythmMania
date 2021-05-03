package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.graphics.Color
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.border.SolidBorder
import io.github.chrislo27.paintbox.ui.element.RectElement
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.pane.EditorPane


class EditorTrackArea(val allTracksPane: AllTracksPane) : Pane() {
    val editorPane: EditorPane = allTracksPane.editorPane
    val trackView: TrackView = allTracksPane.trackView
    val editor: Editor = editorPane.editor
    
    init {
        this.border.set(Insets(0f, 2f, 0f, 0f))
        this.borderStyle.set(SolidBorder().apply { this.color.bind { editorPane.palette.trackPaneBorder.use() }})
        addChild(RectElement(Color.RED))
    }
}