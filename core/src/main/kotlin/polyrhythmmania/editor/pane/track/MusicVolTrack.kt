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
import paintbox.binding.Var
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.util.ColorStack
import paintbox.util.MathHelper
import paintbox.util.gdxutils.*
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaColors
import polyrhythmmania.editor.Click
import polyrhythmmania.editor.PlayState
import polyrhythmmania.editor.Tool
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.undo.impl.AddMusicVolumeAction
import polyrhythmmania.editor.undo.impl.ChangeMusicVolumeAction
import polyrhythmmania.editor.undo.impl.DeleteMusicVolumeAction
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.util.DecimalFormats
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign


class MusicVolTrack(allTracksPane: AllTracksPane) : LongTrackPane(allTracksPane, true) {

    private val lastMouseAbsolute: Vector2 = Vector2()
    private val lastMouseRelative: Vector2 = Vector2()

    val musicVolMarkerPane: MusicVolMarkerPane

    init {
        this.sidePanel.sidebarBgColor.bind { editorPane.palette.trackPaneMusicVolBg.use() }
        val titleLabel = this.sidePanel.titleLabel
        titleLabel.markup.set(editorPane.palette.markupBordered)
//        titleLabel.bounds.width.set(80f)
        titleLabel.renderAlign.set(Align.topLeft)
        this.sidePanel.titleText.bind { Localization.getVar("editor.track.musicVol").use() }
        this.contentBgColor.bind { editorPane.palette.trackPaneTimeBg.use() }
        this.bounds.height.set(32f)
        this.showContentBorder.set(true)

        this.contentSection += MusicWaveformPane(editorPane).apply { 
            this.opacity.bind { (editor.settings.editorMusicWaveformOpacity.use() / 10f).coerceIn(0f, 1f) }
            this.visible.bind { editor.settings.editorMusicWaveformOpacity.use() > 0 }
        }
        this.contentSection += VerticalBeatLinesPane(editorPane)

        this.musicVolMarkerPane = this.MusicVolMarkerPane()
        contentSection += this.musicVolMarkerPane
    }

    private fun getScrollAmount(scrollDirection: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Int {
        if (scrollDirection == 0) return 0
        if (alt) return 0
        return -scrollDirection.sign * (if (ctrl && shift) 0 else if (ctrl) 1 else if (shift) 25 else 5)
    }

    private inline fun createInputListener(crossinline onScroll: (amt: Int) -> Unit): InputEventListener {
        return InputEventListener { event ->
            if (editor.tool.getOrCompute() == Tool.MUSIC_VOLUME && editor.click.getOrCompute() == Click.None) {
                if (event is Scrolled) {
                    val scrollAmt = getScrollAmount(event.amountY.roundToInt(),
                            Gdx.input.isControlDown(), Gdx.input.isShiftDown(), Gdx.input.isAltDown())
                    if (scrollAmt != 0) {
                        onScroll(scrollAmt)
                    }
                    return@InputEventListener true
                }
            }
            false
        }
    }

    inner class MusicVolMarkerPane : Pane() {

        private val isMouseOver: Var<Boolean> = Var(false)
        private val currentHoveredMusicVol: Var<MusicVolume?> = Var(null)

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
                            if (editor.tool.getOrCompute() != Tool.MUSIC_VOLUME || editor.click.getOrCompute() != Click.None)
                                return@addInputEventListener false
                            val ctrl = Gdx.input.isControlDown()
                            val alt = Gdx.input.isAltDown()
                            val shift = Gdx.input.isShiftDown()
                            val mv = currentHoveredMusicVol.getOrCompute()
                            if (event.button == Input.Buttons.RIGHT && mv != null && (!ctrl && !alt && !shift)) {
                                // Remove tempo change
                                editor.mutate(DeleteMusicVolumeAction(mv))
                                inputConsumed = true
                            } else if (event.button == Input.Buttons.LEFT) {
                                if (mv == null) {
                                    // Add music volume change
                                    val beat = MathHelper.snapToNearest(getBeatFromRelative(lastMouseRelative.x), editor.snapping.get())
                                    if (beat >= 0f) {
                                        val musicVolumes = editor.musicVolumes.getOrCompute().toMutableList()
                                        val volumes = editor.engine.musicData.volumeMap
                                        editor.compileEditorMusicVolumes()
                                        val newVolume = (volumes.volumeAtBeat(beat) * (if (ctrl) 0.5f else if (shift) 2f else 1f)).roundToInt().coerceIn(MusicVolume.MIN_VOLUME, MusicVolume.MAX_VOLUME)
                                        val newMv = MusicVolume(beat, 0f, newVolume)
                                        if (!alt && !(ctrl && shift) && !musicVolumes.any { newMv.beat in it.beat..(it.beat + it.width) }) {
                                            editor.mutate(AddMusicVolumeAction(newMv))
                                            currentHoveredMusicVol.set(newMv)
                                            inputConsumed = true
                                        }
                                    }
                                } else {
                                    val snap = editor.snapping.get()
                                    val mouseX = getBeatFromRelative(lastMouseRelative.x)
                                    val left = MathUtils.isEqual(mv.beat, mouseX, snap * 0.5f)
                                    val right = MathUtils.isEqual(mv.beat + mv.width, mouseX, snap * 0.5f)
                                    if (left || right) {
                                        editor.click.set(Click.DragMusicVolume(editor, mv, left))
                                    }
                                    
                                    currentHoveredMusicVol.set(null)
                                    inputConsumed = true
                                }
                            }
                        }
                        inputConsumed
                    }
                    else -> false
                }

            }
            // Change volume
            addInputEventListener(createInputListener { amt ->
                if (editor.allowedToEdit.getOrCompute()) {
                    val mv = currentHoveredMusicVol.getOrCompute()
                    if (mv != null) {
                        val originalVol = mv.newVolume
                        var futureVol: Int = (originalVol + amt)
                        futureVol = futureVol.coerceIn(MusicVolume.MIN_VOLUME, MusicVolume.MAX_VOLUME)
                        if (futureVol != originalVol) {
                            val peek = editor.peekAtUndoStack()
                            val newMv = mv.copy(newVolume = futureVol)
                            if (peek != null && peek is ChangeMusicVolumeAction && peek.next === mv) {
                                peek.undo(editor)
                                peek.next = newMv
                                peek.redo(editor)
                            } else {
                                editor.mutate(ChangeMusicVolumeAction(mv, newMv))
                            }
                            currentHoveredMusicVol.set(determineMusicVolFromBeat(getBeatFromRelative(lastMouseRelative.x)))
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
                currentHoveredMusicVol.set(determineMusicVolFromBeat(currentBeat))
            } else {
                currentHoveredMusicVol.set(null)
            }

            val currentTool = editor.tool.getOrCompute()
            updateBeatLines(currentTool, currentBeat)

            editor.click.getOrCompute().onMouseMoved(currentBeat, 0, 0f)
        }

        private fun updateBeatLines(currentTool: Tool, currentBeat: Float) {
            val beatLines = editor.beatLines
            if (currentTool == Tool.MUSIC_VOLUME) {
                beatLines.active = true
                beatLines.fromBeat = floor(currentBeat).toInt()
                beatLines.toBeat = ceil(currentBeat).toInt()
            }
        }

        private fun getBeatFromRelative(relX: Float = lastMouseRelative.x): Float {
            return editor.trackView.translateXToBeat(relX)
        }

        private fun determineMusicVolFromBeat(beat: Float): MusicVolume? {
            if (beat < 0f) return null
            val snapping = editor.snapping.get()
            val volumeMap = editor.musicVolumes.getOrCompute()
            val snappingHalf = snapping * 0.5f
            return volumeMap.firstOrNull { mv ->
                mv.beat >= 0f && (if (mv.width <= 0f) (MathUtils.isEqual(beat, mv.beat, snappingHalf)) else (beat in mv.beat..(mv.beat + mv.width)))
            }
        }

        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
            val renderBounds = this.contentZone
            val x = renderBounds.x.get() + originX
            val y = originY - renderBounds.y.get()
            val w = renderBounds.width.get()
            val h = renderBounds.height.get()
            val lastPackedColor = batch.packedColor

            val tmpColor = ColorStack.getAndPush()
            val trackView = editorPane.editor.trackView
            val trackViewBeat = trackView.beat.get()
            val leftBeat = floor(trackViewBeat)
            val rightBeat = ceil(trackViewBeat + (w / trackView.pxPerBeat.get()))
            val currentTool = editor.tool.getOrCompute()
            val lineWidth = 2f

            // Playback start
            val musicVolColor = ColorStack.getAndPush().set(PRManiaColors.MUSIC_VOLUME)
            val triangle = AssetRegistry.get<Texture>("ui_triangle_equilateral")
            val triangleSize = 12f

            val musicVolumes = editor.musicVolumes.getOrCompute()
            val currentClick = editor.click.getOrCompute()
            val hoveredMusicVol: MusicVolume? = if (currentTool == Tool.MUSIC_VOLUME) {
                if (currentClick is Click.DragMusicVolume) {
                    MusicVolume(currentClick.beat, currentClick.width, currentClick.musicVol.newVolume)
                } else if (lastMouseRelative.y in 0f..this.bounds.height.get()) {
                    val current = this.currentHoveredMusicVol.getOrCompute()
                    if (current in musicVolumes) {
                        current
                    } else {
                        this.currentHoveredMusicVol.set(null)
                        null
                    }
                } else null
            } else {
                null
            }

            editorPane.palette.beatMarkerFont.useFont { font ->
                font.scaleMul(0.75f)

                val clickOriginalTc: MusicVolume? = if (currentClick is Click.DragMusicVolume) currentClick.musicVol else null
                for (mv in musicVolumes) {
                    val beat = mv.beat
                    if (beat <= (rightBeat + 1) && (beat + mv.width) >= (leftBeat - 2)) {
                        if (mv === clickOriginalTc) {
                            val ghostColor = ColorStack.getAndPush().set(musicVolColor)
                            val a = ghostColor.a
                            ghostColor.a = 1f
                            ghostColor.r *= 0.5f * a
                            ghostColor.g *= 0.5f * a
                            ghostColor.b *= 0.5f * a
                            drawMusicVol(batch, ghostColor, x, y, h, trackView, lineWidth, triangle, triangleSize, beat, mv.width, mv.newVolume, null)
                            ColorStack.pop()
                        } else {
                            drawMusicVol(batch, musicVolColor, x, y, h, trackView, lineWidth, triangle, triangleSize, beat, mv.width, mv.newVolume, font)
                        }
                    }
                }
                if (hoveredMusicVol != null) {
                    if (currentClick is Click.DragMusicVolume && !currentClick.isCurrentlyValid.getOrCompute()) {
                        musicVolColor.set(1f, 0f, 0f, 1f)
                    } else {
                        musicVolColor.set(1f, 1f, 1f, 1f)
                    }
                    val beat = hoveredMusicVol.beat
                    drawMusicVol(batch, musicVolColor, x, y, h, trackView, lineWidth, triangle, triangleSize, beat, hoveredMusicVol.width, hoveredMusicVol.newVolume, font)
                }
            }
            ColorStack.pop()


            ColorStack.pop()
            batch.packedColor = lastPackedColor
        }

        private fun drawMusicVol(batch: SpriteBatch, color: Color, x: Float, y: Float, h: Float, trackView: TrackView, lineWidth: Float,
                                 triangle: Texture, triangleSize: Float, beat: Float, mvWidth: Float, volume: Int, font: BitmapFont?) {

            font?.color = color
            batch.color = color

            if (mvWidth <= 0f) {
                batch.fillRect(x + trackView.translateBeatToX(beat), y - h, lineWidth, h)
                batch.draw(triangle, x + trackView.translateBeatToX(beat) - triangleSize / 2 + lineWidth / 2,
                        y - h, triangleSize, triangleSize)
            } else {
                // Gradient
                val leftColor = ColorStack.getAndPush()
                val rightColor = ColorStack.getAndPush()
                val leftX = x + trackView.translateBeatToX(beat)
                val rightX = x + trackView.translateBeatToX(beat + mvWidth)

                leftColor.set(color)
                rightColor.set(color)
                leftColor.a *= 0.6f
                rightColor.a *= 0.25f

                batch.drawQuad(leftX, y - h, leftColor,
                        rightX, y - h, rightColor,
                        rightX, y, rightColor,
                        leftX, y, leftColor)

                ColorStack.pop()
                ColorStack.pop()

                // Borders
                batch.color = color
                batch.fillRect(leftX, y - h, lineWidth, h)
                batch.fillRect(rightX, y - h, lineWidth, h)

                batch.draw(triangle, leftX + lineWidth / 2,
                        y - h, triangleSize / 2, triangleSize, 0.5f, 1f, 1f, 0f)
                batch.draw(triangle, rightX - triangleSize / 2 + lineWidth / 2,
                        y - h, triangleSize / 2, triangleSize, 0f, 1f, 0.5f, 0f)
            }

            font?.draw(batch, Localization.getValue("editor.musicVolume", DecimalFormats.format("#", volume)),
                    x + trackView.translateBeatToX(beat + mvWidth) + triangleSize / 2 + 2f,
                    y - h + font.capHeight + 6f, 0f, Align.left, false)
        }
    }
}