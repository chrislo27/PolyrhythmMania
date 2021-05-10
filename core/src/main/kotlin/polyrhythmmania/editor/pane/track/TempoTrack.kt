package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.element.RectElement
import io.github.chrislo27.paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.util.DecimalFormats


class TempoTrack(allTracksPane: AllTracksPane) : LongTrackPane(allTracksPane, true) {

    val tempoMarkerPane: TempoMarkerPane = this.TempoMarkerPane()

    init {
        this.sidePanel.sidebarBgColor.bind { editorPane.palette.trackPaneTempoBg.use() }
        val titleLabel = this.sidePanel.titleLabel
        titleLabel.markup.set(editorPane.palette.markupBordered)
        titleLabel.bounds.width.set(80f)
        titleLabel.renderAlign.set(Align.topLeft)
        this.sidePanel.titleText.bind { Localization.getVar("editor.track.tempo").use() }
        this.contentBgColor.bind { editorPane.palette.trackPaneTimeBg.use() }
        this.bounds.height.set(36f)
        this.showContentBorder.set(true)

        this.contentSection += VerticalBeatLinesPane(editorPane)

        this.sidePanel.sidebarSection += Pane().apply {
            this.bounds.x.set(80f)
            this.bindWidthToParent(-this.bounds.x.getOrCompute())
            this += Pane().apply {
                this.bounds.x.set(0f)
                this.bounds.width.set(56f)
                this.padding.set(Insets(0f, 0f, 0f, 3f))
                this += TextLabel(binding = { Localization.getVar("editor.tempo.startingTempo").use() },
                        font = editorPane.palette.sidePanelFont).apply {
//                    this.setScaleXY(0.75f)
                    this.scaleY.set(0.75f)
//                this.renderBackground.set(true); this.bgPadding.set(2f)
                    this.markup.set(editorPane.palette.markupBordered)
                    this.renderAlign.set(Align.right)
                    this.textColor.set(Color.WHITE)
                    this.textAlign.set(TextAlign.RIGHT)
                }
            }
            this += Pane().apply {
                this.bounds.x.set(56f)
                this.bindWidthToParent(-this.bounds.x.getOrCompute())
//                this.bounds.width.set(64f)
                this += RectElement(Color().grey(0f, 0.5f))
                this += TextLabel(binding = { DecimalFormats.format("0.0#", editor.startingTempo.use() * 2) },
                        font = editorPane.palette.sidePanelFont).apply {
                    this.padding.set(Insets(2f))
                    this.renderAlign.set(Align.center)
                    this.textColor.set(Color.WHITE)
                    this.textAlign.set(TextAlign.CENTRE)
                }
            }
        }
    }


    inner class TempoMarkerPane : Pane() {
        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        }
    }
}