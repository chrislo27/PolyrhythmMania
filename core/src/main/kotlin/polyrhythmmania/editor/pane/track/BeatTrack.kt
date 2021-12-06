package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import paintbox.binding.FloatVar
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.util.ColorStack
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.VBox
import paintbox.util.Vector2Stack
import paintbox.util.gdxutils.*
import polyrhythmmania.Localization
import polyrhythmmania.editor.Click
import polyrhythmmania.editor.PlayState
import polyrhythmmania.editor.Tool
import polyrhythmmania.editor.undo.impl.*
import polyrhythmmania.engine.timesignature.TimeSignature
import polyrhythmmania.util.DecimalFormats
import polyrhythmmania.util.LelandSpecialChars
import polyrhythmmania.util.TimeUtils
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt


class BeatTrack(allTracksPane: AllTracksPane) : LongTrackPane(allTracksPane, true) {

    val beatTimeLabel: TextLabel
    val secLabel: TextLabel
    val bpmLabel: TextLabel
    val beatMarkerPane: BeatMarkerPane
    
    private var currentTimeSigBeat: Int = -1

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
        beatTimeLabel = TextLabel(binding = {
            timeTextVar.use()
        }, font = editorPane.palette.beatTimeFont).apply {
            this.textAlign.set(TextAlign.RIGHT)
            this.renderAlign.set(Align.right)
            this.textColor.bind { editorPane.palette.trackPaneTimeText.use() }
            this.bgPadding.set(Insets.ZERO)
            this.padding.set(Insets(0f, 0f, 4f, 4f))
            this.bounds.height.set(28f)
            val tooltipVar = Localization.getVar("editor.track.beat.tooltip.currentBeat", Var {
                listOf(DecimalFormats.format("0.000", editor.playbackStart.use()), DecimalFormats.format("0.000", editor.musicFirstBeat.use()))
            })
            this.tooltipElement.set(editorPane.createDefaultTooltip(tooltipVar))
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
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.track.beat.tooltip.currentTime")))
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
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.track.beat.tooltip.currentBPM")))
        }
        secondsBox += bpmLabel

        vbox.temporarilyDisableLayouts {
            vbox += beatTimeLabel
            vbox += secondsBox
        }

        beatMarkerPane = this.BeatMarkerPane()
        this.contentSection += beatMarkerPane
    }

    init {
        beatMarkerPane.addInputEventListener { event ->
            var consumed = false
            val currentTool: Tool = editor.tool.getOrCompute()
            val control = Gdx.input.isControlDown()
            val shift = Gdx.input.isShiftDown()
            val alt = Gdx.input.isAltDown()
            if (event is MouseInputEvent) {
                val lastMouseRelative = Vector2Stack.getAndPush()
                val thisPos = beatMarkerPane.getPosRelativeToRoot(lastMouseRelative)
                lastMouseRelative.x = event.x - thisPos.x
                lastMouseRelative.y = event.y - thisPos.y
                if (editor.allowedToEdit.get()) {
                    when (event) {
                        is TouchDown -> {
                            if (currentTool == Tool.SELECTION) {
                                if (event.button == Input.Buttons.RIGHT) {
                                    if (!shift && !alt) {
                                        if (control) {
                                            editor.attemptMusicDelayMove(editor.trackView.translateXToBeat(lastMouseRelative.x))
                                        } else {
                                            editor.attemptPlaybackStartMove(editor.trackView.translateXToBeat(lastMouseRelative.x))
                                        }
                                        consumed = true
                                    }
                                }
                            } else if (currentTool == Tool.TIME_SIGNATURE && !control && !alt && !shift) {
                                val targetBeat = (editor.trackView.translateXToBeat(lastMouseRelative.x)).roundToInt()
                                val foundTs: TimeSignature? = editor.timeSignatures.getOrCompute().firstOrNull {
                                    it.beat.roundToInt() == targetBeat
                                }
                                this.currentTimeSigBeat = targetBeat
                                if (event.button == Input.Buttons.LEFT) {
                                    if (foundTs == null) {
                                        val lastTs: TimeSignature? = editor.engine.timeSignatures.getTimeSignature(targetBeat.toFloat())
                                        editor.mutate(AddTimeSignatureAction(TimeSignature(targetBeat.toFloat(),
                                                lastTs?.beatsPerMeasure ?: 4,
                                                lastTs?.beatUnit ?: TimeSignature.DEFAULT_NOTE_UNIT)))
                                        consumed = true
                                    }
                                } else if (event.button == Input.Buttons.RIGHT) {
                                    if (foundTs != null) {
                                        editor.mutate(DeleteTimeSignatureAction(foundTs))
                                        consumed = true
                                    }
                                }
                            }
                        }
                        is MouseMoved -> {
                            val targetBeat = (editor.trackView.translateXToBeat(lastMouseRelative.x)).roundToInt()
                            this.currentTimeSigBeat = targetBeat
                        }
                    }
                }

                Vector2Stack.pop()
            } else if (event is Scrolled) {
                if (currentTool == Tool.TIME_SIGNATURE && !control && !alt && !shift) {
                    val targetBeat = this.currentTimeSigBeat
                    val foundTs: TimeSignature? = editor.timeSignatures.getOrCompute().firstOrNull {
                        it.beat.roundToInt() == targetBeat
                    }
                    if (foundTs != null) {
                        val originalNumerator = foundTs.beatsPerMeasure
                        val amt = -event.amountY.roundToInt()
                        var futureNumerator = originalNumerator + amt
                        futureNumerator = futureNumerator.coerceIn(TimeSignature.LOWER_BEATS_PER_MEASURE,
                                TimeSignature.UPPER_BEATS_PER_MEASURE)
                        if (futureNumerator != originalNumerator) {
                            val peek = editor.peekAtUndoStack()
                            val newTc = foundTs.copy(beatsPerMeasure = futureNumerator)
                            if (peek != null && peek is ChangeTimeSignatureAction && peek.next === foundTs) {
                                peek.undo(editor)
                                peek.next = newTc
                                peek.redo(editor)
                            } else {
                                editor.mutate(ChangeTimeSignatureAction(foundTs, newTc))
                            }
                        }
                    }
                }
            }

            consumed
        }
    }

    inner class BeatMarkerPane : Pane() {
        private val timeSignaturesToRender: MutableList<TimeSignature> = mutableListOf()
        
        private fun getMeasurePart(beat: Int): Int {
            return editorPane.getMeasurePart(beat)
        }

        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
            val renderBounds = this.contentZone
            val x = renderBounds.x.get() + originX
            val y = originY - renderBounds.y.get()
            val w = renderBounds.width.get()
            val h = renderBounds.height.get()
            val lastPackedColor = batch.packedColor

            val tmpColor = ColorStack.getAndPush()
            val trackView = editor.trackView
            val trackViewBeat = trackView.beat.get()
            val trackViewScale = trackView.renderScale.get()
            val leftBeat = floor(trackViewBeat)
            val rightBeat = ceil(trackViewBeat + (w / trackView.pxPerBeat.get()))
            val timeSignatures = editor.engine.timeSignatures

            val lineWidth = 2f
            val tallLineProportion = 0.275f // 0.4f
            val shortLineProportion = tallLineProportion * 0.75f
            for (b in leftBeat.toInt()..rightBeat.toInt()) {
                val measurePart = getMeasurePart(b)
                if (!editorPane.shouldDrawBeatLine(trackViewScale, b, measurePart, false)) continue
                val mainRgb = if (measurePart > 0) 0.8f else 1f
                val mainWidth = if (measurePart == 0) (lineWidth * 3) else lineWidth
                tmpColor.set(mainRgb, mainRgb, mainRgb, 1f)
                batch.color = tmpColor
                batch.fillRect(x + trackView.translateBeatToX(b.toFloat()) - mainWidth / 2f, y - h, mainWidth, h * tallLineProportion)

                if (!editorPane.shouldDrawBeatLine(trackViewScale, b, measurePart, true)) continue
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
                val playbackStart = marker.beat.get()
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
                val pos = editor.engineBeat.get()
                val tmpColor3 = ColorStack.getAndPush().set(editorPane.palette.trackPlayback.getOrCompute())
                batch.color = tmpColor3
                batch.fillRect(x + trackView.translateBeatToX(pos), y - h, lineWidth, h)
                ColorStack.pop()
            }

            ColorStack.pop()

            // Draw beat numbers
            editorPane.palette.beatTrackFont.useFont { font ->
                for (b in leftBeat.toInt()..rightBeat.toInt()) {
                    val beatF = b.toFloat()
                    val xPos = x + trackView.translateBeatToX(beatF)
                    val timeSigAtBeat = timeSignatures.map[beatF]
                    if (timeSigAtBeat != null) {
                        timeSignaturesToRender.add(timeSigAtBeat)
                    } else {
                        val measurePart = getMeasurePart(b)
                        if (!editorPane.shouldDrawBeatNumber(trackViewScale, b, measurePart)) continue
                        
                        if (measurePart > 0) {
                            tmpColor.set(1f, 0.95f, 0.78f, 1f)
                        } else {
                            tmpColor.set(1f, 1f, 1f, 1f)
                        }
                        font.color = tmpColor
                        font.draw(batch, b.toString(), xPos, y - h + h * tallLineProportion + font.capHeight + 3f, 0f, Align.center, false)
                        if (measurePart == 0) {
                            val measureNum = timeSignatures.getMeasure(beatF)
                            tmpColor.set(0.8f, 0.8f, 0.8f, 1f)
                            font.color = tmpColor
                            val cap = font.capHeight
                            font.scaleMul(0.75f)
                            font.draw(batch, measureNum.toString(), xPos,
                                    y - h + h * tallLineProportion + cap + font.lineHeight + 2f,
                                    0f, Align.center, false)
                            font.scaleMul(1f / 0.75f)
                        }
                    }
                }
            }

            // Draw time signatures
            if (timeSignaturesToRender.isNotEmpty()) {
                editorPane.palette.musicScoreFont.useFont { font ->
                    timeSignaturesToRender.forEach { ts ->
                        val xPos = x + trackView.translateBeatToX(ts.beat)
                        val offY = 26f
                        val lineHeight = 16f
                        
                        if (editor.tool.getOrCompute() == Tool.TIME_SIGNATURE && ts.beat.roundToInt() == currentTimeSigBeat) {
                            tmpColor.set(0f, 1f, 1f, 1f)
                        } else {
                            tmpColor.set(1f, 1f, 1f, 1f)
                        }
                        font.color = tmpColor
                        val textAlign = if (ts.beat == 0f) Align.left else Align.center
                        font.draw(batch, LelandSpecialChars.intToString(ts.beatUnit),
                                xPos, y - h + h * tallLineProportion + offY, 0f, textAlign, false)
                        font.draw(batch, LelandSpecialChars.intToString(ts.beatsPerMeasure),
                                xPos, y - h + h * tallLineProportion + offY + lineHeight, 0f, textAlign, false)
                    }
                }

                timeSignaturesToRender.clear()
            }

            ColorStack.pop()
            batch.packedColor = lastPackedColor
        }
    }
}