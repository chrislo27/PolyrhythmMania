package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.github.chrislo27.paintbox.ui.Pane


class TempoTrack(allTracksPane: AllTracksPane) : LongTrackPane(allTracksPane, true) {
    
    val tempoMarkerPane: TempoMarkerPane = this.TempoMarkerPane()
    
    init {
        this.sidePanel.sidebarBgColor.bind { editorPane.palette.trackPaneTempoBg.use() }
        this.sidePanel.titleText.set("Tempo")
        this.contentBgColor.bind { editorPane.palette.trackPaneTimeBg.use() }
        this.bounds.height.set(36f)
        this.showContentBorder.set(true)
        
        this.contentSection += VerticalBeatLinesPane(editorPane)
    }


    inner class TempoMarkerPane : Pane() {
        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        }
    }
}