package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.*
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.element.RectElement
import io.github.chrislo27.paintbox.util.MathHelper
import io.github.chrislo27.paintbox.util.gdxutils.*
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaColors
import polyrhythmmania.editor.Click
import polyrhythmmania.editor.PlayState
import polyrhythmmania.editor.Tool
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.undo.impl.AddTempoChangeAction
import polyrhythmmania.editor.undo.impl.ChangeStartingTempoAction
import polyrhythmmania.editor.undo.impl.ChangeTempoChangeAction
import polyrhythmmania.editor.undo.impl.DeleteTempoChangeAction
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.util.DecimalFormats
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign


class TempoTrack(allTracksPane: AllTracksPane) : LongTrackPane(allTracksPane, true) {

    companion object {
        const val MIN_TEMPO: Float = 1f
        const val MAX_TEMPO: Float = 600f
    }

    private val lastMouseAbsolute: Vector2 = Vector2()
    private val lastMouseRelative: Vector2 = Vector2()

    val tempoMarkerPane: TempoMarkerPane

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
                    this.textAlign.set(TextAlign.RIGHT)
                    this.textColor.set(Color.WHITE)
                }
            }
            this += Pane().apply {
                this.bounds.x.set(56f)
                this.bindWidthToParent(-this.bounds.x.getOrCompute())
                this += RectElement(Color().grey(0f, 0.5f))
                this += Pane().apply {
                    this.padding.set(Insets(2f))

                    this += TextLabel(binding = { DecimalFormats.format("0.0#", editor.startingTempo.use()) },
                            font = editorPane.palette.sidePanelFont).apply {
                        this.padding.set(Insets(2f))
                        this.renderAlign.set(Align.center)
                        this.textAlign.set(TextAlign.CENTRE)

                        val mousedOver = Var(false)
                        this.addInputEventListener { event ->
                            if (event is MouseEntered) {
                                mousedOver.set(true)
                            } else if (event is MouseExited) {
                                mousedOver.set(false)
                            }
                            false
                        }

                        val highlighted = Color(0.25f, 1f, 1f, 1f)
                        this.textColor.bind {
                            val tool = editor.tool.use()
                            val mouseOver = mousedOver.use()
                            if (tool == Tool.TEMPO_CHANGE && mouseOver)
                                highlighted
                            else Color.WHITE
                        }

                        this.addInputEventListener(createInputListener { amt ->
                            val originalTempo = editor.startingTempo.getOrCompute()
                            var futureTempo = originalTempo + amt
                            futureTempo = futureTempo.coerceIn(MIN_TEMPO, MAX_TEMPO)
                            if (futureTempo != originalTempo) {
                                val peek = editor.getUndoStack().peekFirst()
                                if (peek is ChangeStartingTempoAction) {
                                    peek.next = futureTempo
                                } else {
                                    editor.addActionWithoutMutating(ChangeStartingTempoAction(originalTempo, futureTempo))
                                }
                                editor.startingTempo.set(futureTempo)
                            }
                        })
                    }
                }
            }
        }

        this.tempoMarkerPane = this.TempoMarkerPane()
        contentSection += this.tempoMarkerPane
    }

    private fun getScrollAmount(scrollDirection: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Float {
        if (scrollDirection == 0) return 0f
        if (alt) return 0f
        return -scrollDirection.sign * (if (ctrl && shift) 0.05f else if (ctrl) 0.1f else if (shift) 5f else 1f)
    }

    private inline fun createInputListener(crossinline onScroll: (amt: Float) -> Unit): InputEventListener {
        return InputEventListener { event ->
            if (editor.tool.getOrCompute() == Tool.TEMPO_CHANGE && editor.click.getOrCompute() == Click.None) {
                if (event is Scrolled) {
                    val scrollAmt = getScrollAmount(event.amountY.roundToInt(),
                            Gdx.input.isControlDown(), Gdx.input.isShiftDown(), Gdx.input.isAltDown())
                    if (scrollAmt != 0f) {
                        onScroll(scrollAmt)
                    }
                    return@InputEventListener true
                }
            }
            false
        }
    }


    inner class TempoMarkerPane : Pane() {

        private val isMouseOver: Var<Boolean> = Var(false)
        private val currentHoveredTempoChange: Var<TempoChange?> = Var(null)

        init {
            this.doClipping.set(true)
        }

        init {
            addInputEventListener { event ->
                when (event) {
                    is MouseMoved -> {
                        onMouseMovedOrDragged(event.x, event.y)
                        true
                    }
                    is TouchDragged -> {
                        onMouseMovedOrDragged(event.x, event.y)
                        true
                    }
                    is MouseExited -> {
                        isMouseOver.set(false)
                        onMouseMovedOrDragged(event.x, event.y)
                        false
                    }
                    is MouseEntered -> {
                        isMouseOver.set(true)
                        onMouseMovedOrDragged(event.x, event.y)
                        false
                    }
                    // Scrolled is handled below
                    is TouchDown -> {
                        var inputConsumed = false
                        if (editor.playState.getOrCompute() == PlayState.STOPPED) {
                            if (editor.tool.getOrCompute() != Tool.TEMPO_CHANGE || editor.click.getOrCompute() != Click.None)
                                return@addInputEventListener false
                            val ctrl = Gdx.input.isControlDown()
                            val alt = Gdx.input.isAltDown()
                            val shift = Gdx.input.isShiftDown()
                            val tc = currentHoveredTempoChange.getOrCompute()
                            if (event.button == Input.Buttons.RIGHT && tc != null && (!ctrl && !alt && !shift)) {
                                // Remove tempo change
                                editor.mutate(DeleteTempoChangeAction(tc))
                                inputConsumed = true
                            } else if (event.button == Input.Buttons.LEFT) {
                                if (tc == null) {
                                    // Add tempo change
                                    val beat = MathHelper.snapToNearest(getBeatFromRelative(lastMouseRelative.x), editor.snapping.getOrCompute())
                                    if (beat > 0f) {
                                        val tempoChanges = editor.tempoChanges.getOrCompute().toMutableList()
                                        val tempos = editor.engine.tempos
                                        editor.compileEditorTempos()
                                        val newTempo = (tempos.tempoAtBeat(beat) * (if (ctrl) 0.5f else if (shift) 2f else 1f)).coerceIn(MIN_TEMPO, MAX_TEMPO)
                                        val newTc = TempoChange(beat, newTempo, tempos.swingAtBeat(beat))
                                        if (!alt && !(ctrl && shift) && !tempoChanges.any { it.beat == newTc.beat }) {
                                            editor.mutate(AddTempoChangeAction(newTc))
                                            currentHoveredTempoChange.set(newTc)
                                            inputConsumed = true
                                        }
                                    }
                                } else {
                                    // Drag tempo change
                                    editor.click.set(Click.MoveTempoChange(editor, tc))
                                    currentHoveredTempoChange.set(null)
                                    inputConsumed = true
                                }
                            }
                        }
                        inputConsumed
                    }
                    else -> false
                }
                
            }
            // Change tempo
            addInputEventListener(createInputListener { amt ->
                if (editor.playState.getOrCompute() == PlayState.STOPPED && editor.click.getOrCompute() == Click.None) {
                    val tc = currentHoveredTempoChange.getOrCompute()
                    if (tc != null) {
                        val originalTempo = tc.newTempo
                        var futureTempo = originalTempo + amt
                        futureTempo = futureTempo.coerceIn(MIN_TEMPO, MAX_TEMPO)
                        if (futureTempo != originalTempo) {
                            val peek = editor.getUndoStack().peekFirst()
                            val newTc = tc.copy(newTempo = futureTempo)
                            if (peek != null && peek is ChangeTempoChangeAction && peek.next === tc) {
                                peek.undo(editor)
                                peek.next = newTc
                                peek.redo(editor)
                            } else {
                                editor.mutate(ChangeTempoChangeAction(tc, newTc))
                            }
                            currentHoveredTempoChange.set(determineTempoChangeFromBeat(getBeatFromRelative(lastMouseRelative.x)))
                        }
                    }
                }
            })
            editor.trackView.beat.addListener {
                onMouseMovedOrDragged(lastMouseAbsolute.x, lastMouseAbsolute.y)
            }
        }

        private fun onMouseMovedOrDragged(x: Float, y: Float) {
            lastMouseAbsolute.set(x, y)
            val thisPos = this.getPosRelativeToRoot(lastMouseRelative)
            lastMouseRelative.x = x - thisPos.x
            lastMouseRelative.y = y - thisPos.y

            val currentBeat = getBeatFromRelative(lastMouseRelative.x)
            if (editor.click.getOrCompute() == Click.None) {
                currentHoveredTempoChange.set(determineTempoChangeFromBeat(currentBeat))
            } else {
                currentHoveredTempoChange.set(null)
            }

            val currentTool = editor.tool.getOrCompute()
            updateBeatLines(currentTool, currentBeat)

            editor.click.getOrCompute().onMouseMoved(currentBeat, 0, 0f)
        }

        private fun updateBeatLines(currentTool: Tool, currentBeat: Float) {
            val beatLines = editor.beatLines
            if (currentTool == Tool.TEMPO_CHANGE) {
                beatLines.active = true
                beatLines.fromBeat = floor(currentBeat).toInt()
                beatLines.toBeat = ceil(currentBeat).toInt()
            }
        }

        private fun getBeatFromRelative(relX: Float = lastMouseRelative.x): Float {
            return editor.trackView.translateXToBeat(relX)
        }

        private fun determineTempoChangeFromBeat(beat: Float): TempoChange? {
            if (beat < 0f) return null
            val snapping = editor.snapping.getOrCompute()
            val tempoChanges = editor.tempoChanges.getOrCompute()
            val snappingHalf = snapping * 0.5f
            return tempoChanges.firstOrNull { tc ->
                tc.beat > 0f && MathUtils.isEqual(beat, tc.beat, snappingHalf)
            }
        }

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
            val currentTool = editor.tool.getOrCompute()
            val lineWidth = 2f

            // Playback start
            val tempoColor = ColorStack.getAndPush().set(PRManiaColors.TEMPO)
            val triangle = AssetRegistry.get<Texture>("ui_triangle_equilateral")
            val triangleSize = 12f

            val tempoChanges = editor.tempoChanges.getOrCompute()
            val currentClick = editor.click.getOrCompute()
            val hoveredTempoChange: TempoChange? = if (currentTool == Tool.TEMPO_CHANGE) {
                if (currentClick is Click.MoveTempoChange) {
                    currentClick.lastValidTempoChangePos
                } else if (lastMouseRelative.y in 0f..this.bounds.height.getOrCompute()) {
                    val current = this.currentHoveredTempoChange.getOrCompute()
                    if (current in tempoChanges) {
                        current
                    } else {
                        this.currentHoveredTempoChange.set(null)
                        null
                    }
                } else null
            } else {
                null
            }

            editorPane.palette.beatMarkerFont.useFont { font ->
                font.scaleMul(0.75f)

                val clickOriginalTc: TempoChange? = if (currentClick is Click.MoveTempoChange) currentClick.tempoChange else null
                for (tc in tempoChanges) {
                    val beat = tc.beat
                    if (beat !in (leftBeat - 2)..(rightBeat + 1)) continue

                    if (tc === clickOriginalTc) {
                        val ghostColor = ColorStack.getAndPush().set(tempoColor)
                        val a = ghostColor.a
                        ghostColor.a = 1f
                        ghostColor.r *= 0.5f * a
                        ghostColor.g *= 0.5f * a
                        ghostColor.b *= 0.5f * a
                        drawTempoChange(batch, ghostColor, x, y, h, trackView, lineWidth, triangle, triangleSize, beat, tc.newTempo, null)
                        ColorStack.pop()
                    } else {
                        drawTempoChange(batch, tempoColor, x, y, h, trackView, lineWidth, triangle, triangleSize, beat, tc.newTempo, font)
                    }
                }
                if (hoveredTempoChange != null) {
                    tempoColor.set(1f, 1f, 1f, 1f)
                    val beat = hoveredTempoChange.beat
                    drawTempoChange(batch, tempoColor, x, y, h, trackView, lineWidth, triangle, triangleSize, beat, hoveredTempoChange.newTempo, font)
                }
            }
            ColorStack.pop()


            ColorStack.pop()
            batch.packedColor = lastPackedColor
        }

        private fun drawTempoChange(batch: SpriteBatch, color: Color, x: Float, y: Float, h: Float, trackView: TrackView, lineWidth: Float,
                                    triangle: Texture, triangleSize: Float, beat: Float, tempo: Float, font: BitmapFont?) {
            font?.color = color
            batch.color = color
            batch.fillRect(x + trackView.translateBeatToX(beat), y - h, lineWidth, h)
            batch.draw(triangle, x + trackView.translateBeatToX(beat) - triangleSize / 2 + lineWidth / 2,
                    y, triangleSize, -triangleSize)
            font?.draw(batch, Localization.getValue("editor.bpm", DecimalFormats.format("0.0#", tempo)),
                    x + trackView.translateBeatToX(beat) + triangleSize / 2 + 2f,
                    y - 4f, 0f, Align.left, false)
        }
    }
}