package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import io.github.chrislo27.paintbox.ui.*
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.pane.EditorPane


abstract class AbstrWavePane(val musicDialog: MusicDialog) : Pane() {
    protected val editorPane: EditorPane = musicDialog.editorPane
    protected val editor: Editor = editorPane.editor

    var isLeftClickDown: Boolean = false
        private set
    var isRightClickDown: Boolean = false
        private set

    protected val lastMouseAbsolute: Vector2 = Vector2()
    protected val lastMouseRelative: Vector2 = Vector2()
    
    protected var isFullWidth: Boolean = true

    init {
//        this.doClipping.set(true)
        addInputEventListener { event ->
            var inputConsumed = false
            if (event is TouchDragged && (isLeftClickDown || isRightClickDown)) {
                onMouseMovedOrDragged(event.x, event.y)
            } else if (event is TouchDown) {
                if (event.button == Input.Buttons.LEFT && !isLeftClickDown) {
                    isLeftClickDown = true
                    onMouseMovedOrDragged(event.x, event.y)
                    inputConsumed = true
                } else if (event.button == Input.Buttons.RIGHT && !isRightClickDown) {
                    isRightClickDown = true
                    onMouseMovedOrDragged(event.x, event.y)
                    inputConsumed = true
                }
            } else if (event is ClickReleased) {
                if (event.button == Input.Buttons.LEFT && isLeftClickDown) {
                    isLeftClickDown = false
                    inputConsumed = true
                } else if (event.button == Input.Buttons.RIGHT && isRightClickDown) {
                    isRightClickDown = false
                    inputConsumed = true
                }
            }

            inputConsumed
        }
    }


    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelfAfterChildren(originX, originY, batch)
        val renderBounds = this.contentZone
        val x = renderBounds.x.getOrCompute() + originX
        val y = originY - renderBounds.y.getOrCompute()
        val w = renderBounds.width.getOrCompute()
        val h = renderBounds.height.getOrCompute()
        val lastPackedColor = batch.packedColor

        val tmpColor: Color = ColorStack.getAndPush()
        tmpColor.set(Color.WHITE)

        val window = musicDialog.window
        val durationSec = window.musicDurationSec.getOrCompute()

        val visibleX = if (isFullWidth) 0f else (window.x.getOrCompute() / durationSec)
        val visibleWidth = if (isFullWidth) 1f else (window.widthSec.getOrCompute() / durationSec)
        
        val playbackStart = window.playbackStart.getOrCompute()
        if (playbackStart / durationSec in visibleX..(visibleX + visibleWidth)) {
            batch.color = musicDialog.markerPlaybackStart
            batch.fillRect(x + ((playbackStart / durationSec) - visibleX) / visibleWidth * w, y - h, 1f, h)
        }
        val loopStart = window.loopStart.getOrCompute()
        if (loopStart / durationSec in visibleX..(visibleX + visibleWidth)) {
            batch.color = musicDialog.markerLoopStart
            batch.fillRect(x + ((loopStart / durationSec) - visibleX) / visibleWidth * w, y - h, 1f, h)
        }
        val loopEnd = window.loopEnd.getOrCompute()
        if (loopEnd / durationSec in visibleX..(visibleX + visibleWidth)) {
            batch.color = musicDialog.markerLoopEnd
            batch.fillRect(x + ((loopEnd / durationSec) - visibleX) / visibleWidth * w, y - h, 1f, h)
        }
        val firstBeat = window.firstBeat.getOrCompute()
        if (firstBeat / durationSec in visibleX..(visibleX + visibleWidth)) {
            batch.color = musicDialog.markerFirstBeat
            batch.fillRect(x + ((firstBeat / durationSec) - visibleX) / visibleWidth * w, y - h, 1f, h)
        }
        val musicPlayback = musicDialog.currentMusicPosition.getOrCompute()
        if (musicPlayback / durationSec in visibleX..(visibleX + visibleWidth)) {
            batch.color = musicDialog.markerPlaybackStart
            batch.fillRect(x + ((musicPlayback / durationSec) - visibleX) / visibleWidth * w, y - h, 1f, h)
        }

        ColorStack.pop()
        batch.packedColor = lastPackedColor
    }

    protected open fun onMouseMovedOrDragged(x: Float, y: Float) {
        updateMousePos(x, y)
    }
    
    protected fun updateMousePos(x: Float, y: Float) {
        lastMouseAbsolute.set(x, y)
        val thisPos = this.getPosRelativeToRoot(lastMouseRelative)
        lastMouseRelative.x = x - thisPos.x
        lastMouseRelative.y = y - thisPos.y
    }
}