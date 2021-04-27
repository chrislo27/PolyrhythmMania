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


class BeatTrack(allTracksPane: AllTracksPane) : TrackPane(allTracksPane) {

    val timeLabel: TextLabel
    val beatMarkerPane: BeatMarkerPane = this.BeatMarkerPane()

    init {
        this.sidebarBgColor.bind { editorPane.palette.trackPaneTimeBg.use() }
        this.contentBgColor.bind { editorPane.palette.trackPaneTimeBg.use() }
        this.bounds.height.set(48f)

        timeLabel = TextLabel("00:00:00.000", font = editorPane.main.fontEditorBeatTime).apply {
            this.textAlign.set(TextAlign.RIGHT)
            this.renderAlign.set(Align.right)
            this.textColor.bind { editorPane.palette.trackPaneTimeText.use() }
            this.bgPadding.set(0f)
            this.padding.set(Insets(0f, 0f, 4f, 4f))
        }
        this.sidebarSection += timeLabel
        
        beatMarkerPane.apply { 
            
        }
        this.contentSection += beatMarkerPane
    }
    
    inner class BeatMarkerPane : Pane() {
        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
            val renderBounds = this.contentZone
            val x = renderBounds.x.getOrCompute() + originX
            val y = originY - renderBounds.y.getOrCompute()
            val w = renderBounds.width.getOrCompute()
            val h = renderBounds.height.getOrCompute()
            val lastPackedColor = batch.packedColor

            val tmpColor = ColorStack.getAndPush()
            val trackView = editorPane.editor.trackView
            val leftBeat = floor(trackView.beat)
            val rightBeat = ceil(trackView.beat + (w / trackView.pxPerBeat))

            tmpColor.set(1f, 1f, 1f, 1f)
            batch.color = tmpColor
            val lineWidth = 2f
            for (b in leftBeat.toInt()..rightBeat.toInt()) {
                batch.fillRect(x + (b - trackView.beat) * trackView.pxPerBeat - lineWidth / 2f, y - h, lineWidth, h * 0.5f)
                batch.fillRect(x + (b - trackView.beat + 0.5f) * trackView.pxPerBeat - lineWidth / 2f, y - h, lineWidth, h * 0.25f)
            }
            editorPane.main.mainFont.useFont { font ->
                font.color = tmpColor
                for (b in leftBeat.toInt()..rightBeat.toInt()) {
                    val xPos = x + (b - trackView.beat) * trackView.pxPerBeat
                    font.draw(batch, b.toString(), xPos, y - h + h * 0.5f + font.capHeight + 3f, 0f, Align.center, false)
                    batch.fillRect(xPos, y - h, 2f, h / 2f)
                }
            }

            ColorStack.pop()
            batch.packedColor = lastPackedColor
        }
    }
}