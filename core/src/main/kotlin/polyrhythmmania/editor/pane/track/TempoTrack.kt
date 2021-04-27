package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.TextLabel


class TempoTrack(allTracksPane: AllTracksPane) : TrackPane(allTracksPane) {
    
    init {
        this.sidebarBgColor.bind { editorPane.palette.trackPaneTempoBg.use() }
        this.contentBgColor.bind { editorPane.palette.trackPaneTimeBg.use() }
        this.bounds.height.set(36f)
        this.titleText.set("Tempo")
    }
    
}