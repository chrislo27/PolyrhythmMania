package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.ui.ColorStack
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import kotlin.math.ceil
import kotlin.math.floor


class TempoTrack(allTracksPane: AllTracksPane) : TrackPane(allTracksPane) {
    
    val tempoMarkerPane: TempoMarkerPane = this.TempoMarkerPane()
    
    init {
        this.sidebarBgColor.bind { editorPane.palette.trackPaneTempoBg.use() }
        this.contentBgColor.bind { editorPane.palette.trackPaneTimeBg.use() }
        this.bounds.height.set(36f)
        this.titleText.set("Tempo")
        
        this.contentSection += VerticalBeatLinesPane(editorPane)
    }


    inner class TempoMarkerPane : Pane() {
        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        }
    }
}