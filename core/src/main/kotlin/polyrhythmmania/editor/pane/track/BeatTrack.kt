package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.ColorStack
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.TouchDown
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.layout.HBox
import io.github.chrislo27.paintbox.ui.layout.VBox
import io.github.chrislo27.paintbox.util.gdxutils.*
import polyrhythmmania.Localization
import polyrhythmmania.editor.Click
import polyrhythmmania.editor.PlayState
import polyrhythmmania.editor.Tool
import polyrhythmmania.util.DecimalFormats
import polyrhythmmania.util.TimeUtils
import kotlin.math.ceil
import kotlin.math.floor


class BeatTrack(allTracksPane: AllTracksPane) : LongTrackPane(allTracksPane, true) {

    val timeLabel: TextLabel
    val secLabel: TextLabel
    val bpmLabel: TextLabel
    val beatMarkerPane: BeatMarkerPane

    init {
        this.sidePanel.sidebarBgColor.bind { editorPane.palette.trackPaneTimeBg.use() }
        this.contentBgColor.bind { editorPane.palette.trackPaneTimeBg.use() }
        this.bounds.height.set(54f)
        this.showContentBorder.set(true)

        val vbox = VBox().apply { 
            Anchor.Centre.configure(this)
            this.spacing.set(0f)
            this.align.set(VBox.Align.CENTRE)
        }
        this.sidePanel.sidebarSection += vbox
        
        val timeTextVar = Localization.getVar("editor.currentTime", Var.bind {
            listOf(DecimalFormats.format("0.000", editor.engineBeat.use()))
        })
        val secondsTextVar = Var {
            editor.engineBeat.use()
            TimeUtils.convertMsToTimestamp(editor.engine.seconds * 1000)
        }
        val bpmVar = FloatVar {
            editor.engine.tempos.tempoAtBeat(editor.engineBeat.use())
        }
        val bpmTextVar = Var {
            "♩=${DecimalFormats.format("0.0", bpmVar.use())}"
        }
        timeLabel = TextLabel(binding = {
            timeTextVar.use()
        }, font = editorPane.palette.beatTimeFont).apply {
            this.textAlign.set(TextAlign.RIGHT)
            this.renderAlign.set(Align.right)
            this.textColor.bind { editorPane.palette.trackPaneTimeText.use() }
            this.bgPadding.set(Insets.ZERO)
            this.padding.set(Insets(0f, 0f, 4f, 4f))
            this.bounds.height.set(28f)
        }
        val secondsBox = Pane().apply {
            this.bounds.height.set(16f)
        }
        secLabel = TextLabel(binding = {
            secondsTextVar.use()
        }, font = editorPane.palette.beatSecondsFont).apply {
            Anchor.CentreRight.configure(this)
            this.textAlign.set(TextAlign.RIGHT)
            this.renderAlign.set(Align.right)
            this.textColor.bind { editorPane.palette.trackPaneTimeText.use() }
            this.bgPadding.set(Insets.ZERO)
            this.padding.set(Insets(0f, 0f, 4f, 4f))
            this.bounds.width.bind {
                (parent.use()?.let { p -> p.contentZone.width.use() } ?: 0f) * 0.5f
            }
        }
        secondsBox += secLabel
        bpmLabel = TextLabel(binding = {
            bpmTextVar.use()
        }, font = editorPane.main.fontRodinFixed).apply {
            Anchor.CentreLeft.configure(this)
            this.textAlign.set(TextAlign.LEFT)
            this.renderAlign.set(Align.left)
            this.textColor.bind { editorPane.palette.trackPaneTimeText.use() }
            this.bgPadding.set(Insets.ZERO)
            this.padding.set(Insets(0f, 0f, 4f, 4f))
            this.bounds.width.bind {
                (parent.use()?.let { p -> p.contentZone.width.use() } ?: 0f) * 0.35f
            }
            this.setScaleXY(0.75f)
        }
        secondsBox += bpmLabel
        
        vbox.temporarilyDisableLayouts { 
            vbox += timeLabel
            vbox += secondsBox
        }

        beatMarkerPane = this.BeatMarkerPane()
        this.contentSection += beatMarkerPane
    }

    init {
        beatMarkerPane.addInputEventListener { event ->
            var consumed = false
            when (event) {
                is TouchDown -> {
                    val currentTool: Tool = editor.tool.getOrCompute()
                    val control = Gdx.input.isControlDown()
                    val shift = Gdx.input.isShiftDown()
                    val alt = Gdx.input.isAltDown()

                    if (editor.playState.getOrCompute() == PlayState.STOPPED && editor.click.getOrCompute() == Click.None) {
                        if (currentTool == Tool.SELECTION) {
                            if (event.button == Input.Buttons.RIGHT) {
                                val lastMouseRelative = Vector2(0f, 0f)
                                val thisPos = beatMarkerPane.getPosRelativeToRoot(lastMouseRelative)
                                lastMouseRelative.x = event.x - thisPos.x
                                lastMouseRelative.y = event.y - thisPos.y

                                if (!shift && !alt) {
                                    if (control) {
                                        editor.attemptMusicDelayMove(editor.trackView.translateXToBeat(lastMouseRelative.x))
                                    } else {
                                        editor.attemptPlaybackStartMove(editor.trackView.translateXToBeat(lastMouseRelative.x))
                                    }
                                    consumed = true
                                }
                            }
                        }
                    }
                }
            }

            consumed
        }
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
            val trackViewBeat = trackView.beat.getOrCompute()
            val leftBeat = floor(trackViewBeat)
            val rightBeat = ceil(trackViewBeat + (w / trackView.pxPerBeat.getOrCompute()))

            val lineWidth = 2f
            val tallLineProportion = 0.275f // 0.4f
            val shortLineProportion = tallLineProportion * 0.75f
            for (b in leftBeat.toInt()..rightBeat.toInt()) {
                tmpColor.set(1f, 1f, 1f, 1f)
                batch.color = tmpColor
                batch.fillRect(x + trackView.translateBeatToX(b.toFloat()) - lineWidth / 2f, y - h, lineWidth, h * tallLineProportion)
                tmpColor.set(0.7f, 0.7f, 0.7f, 1f)
                batch.color = tmpColor
                batch.fillRect(x + trackView.translateBeatToX(b.toFloat() + 0.5f) - lineWidth / 2f, y - h, lineWidth, h * shortLineProportion)
            }

            // Markers
            val triangle = AssetRegistry.get<Texture>("ui_triangle_equilateral")
            val triangleSize = 12f
            val currentClick = editor.click.getOrCompute()
            if (currentClick is Click.MoveMarker) {
                val tmpColor2 = ColorStack.getAndPush().set(currentClick.type.color)
                val playbackStartOld = currentClick.originalPosition
                val premul = 0.25f * tmpColor2.a
                batch.setColor(tmpColor2.r * premul, tmpColor2.g * premul, tmpColor2.b * premul, 1f)
                batch.fillRect(x + trackView.translateBeatToX(playbackStartOld), y - h, lineWidth, h)
                batch.draw(triangle, x + trackView.translateBeatToX(playbackStartOld) - triangleSize / 2 + lineWidth / 2,
                        y, triangleSize, -triangleSize)
            }

            val tmpColor2 = ColorStack.getAndPush()

            editor.markerMap.values.forEach { marker ->
                val playbackStart = marker.beat.getOrCompute()
                tmpColor2.set(marker.type.color)
                batch.color = tmpColor2
                batch.fillRect(x + trackView.translateBeatToX(playbackStart), y - h, lineWidth, h)
                batch.draw(triangle, x + trackView.translateBeatToX(playbackStart) - triangleSize / 2 + lineWidth / 2,
                        y, triangleSize, -triangleSize)
                editorPane.palette.beatMarkerFont.useFont { font ->
                    font.scaleMul(0.75f)
                    font.color = tmpColor2
                    font.draw(batch, Localization.getValue("editor.beatTime", DecimalFormats.format("0.000", playbackStart)),
                            x + trackView.translateBeatToX(playbackStart) + triangleSize / 2 + 2f,
                            y - 4f, 0f, Align.left, false)
                    font.draw(batch, Localization.getValue(marker.type.localizationKey),
                            x + trackView.translateBeatToX(playbackStart) - triangleSize / 2 - 2f,
                            y - 4f, 0f, Align.right, false)
                }
            }

            if (editor.playState.getOrCompute() != PlayState.STOPPED) {
                val pos = editor.engineBeat.getOrCompute()
                val tmpColor3 = ColorStack.getAndPush().set(editorPane.palette.trackPlayback.getOrCompute())
                batch.color = tmpColor3
                batch.fillRect(x + trackView.translateBeatToX(pos), y - h, lineWidth, h)
                ColorStack.pop()
            }

            ColorStack.pop()

            // Draw beat numbers
            editorPane.palette.beatTrackFont.useFont { font ->
                tmpColor.set(1f, 1f, 1f, 1f)
                font.color = tmpColor
                for (b in leftBeat.toInt()..rightBeat.toInt()) {
                    val xPos = x + trackView.translateBeatToX(b.toFloat())
                    font.draw(batch, b.toString(), xPos, y - h + h * tallLineProportion + font.capHeight + 3f, 0f, Align.center, false)
                }
            }

            ColorStack.pop()
            batch.packedColor = lastPackedColor
        }
    }
}