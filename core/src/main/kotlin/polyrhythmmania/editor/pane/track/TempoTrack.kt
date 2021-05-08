package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.github.chrislo27.paintbox.ui.Pane
import polyrhythmmania.Localization


class TempoTrack(allTracksPane: AllTracksPane) : LongTrackPane(allTracksPane, true) {
    
    val tempoMarkerPane: TempoMarkerPane = this.TempoMarkerPane()
    
    init {
        this.sidePanel.sidebarBgColor.bind { editorPane.palette.trackPaneTempoBg.use() }
        this.sidePanel.titleLabel.markup.set(editorPane.palette.markupBordered)
        this.sidePanel.titleText.bind { Localization.getVar("editor.track.tempo").use() }
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