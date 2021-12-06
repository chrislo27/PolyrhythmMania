package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.binding.IntVar
import paintbox.binding.Var
import paintbox.util.ColorStack
import kotlin.system.measureNanoTime


class ZoomedWavePane(musicDialog: MusicDialog, val overallPane: OverallWavePane) : AbstrWavePane(musicDialog) {

    private var suggestRenderZoomedLast: Boolean = false
    private var suggestRenderZoomedLastTimeMs: Long = 0L

    /**
     * A listener to the window values. NEVER change what it is set/bound to!
     * It should only be getOrCompute'd from the GL thread.
     */
    private val zoomedRefreshIndicator: IntVar = IntVar(0)

    init {
        this.isFullWidth = false
        zoomedRefreshIndicator.sideEffecting(0) { existing ->
            val window = musicDialog.window
            window.x.useF()
            window.widthSec.useF()
            window.musicDurationSec.useF()

            val nano = measureNanoTime {
                editor.waveformWindow.generateZoomed(window)
            }
//            println("Took ${nano / 1_000_000.0} ms to generate zoomed window")
            suggestRenderZoomedLast = (nano / 1_000_000f) > 10

            existing + 1
        }
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelf(originX, originY, batch)
        val renderBounds = this.contentZone
        val x = renderBounds.x.get() + originX
        val y = originY - renderBounds.y.get()
        val w = renderBounds.width.get()
        val h = renderBounds.height.get()
        val lastPackedColor = batch.packedColor

        val tmpColor: Color = ColorStack.getAndPush()
        tmpColor.set(Color.WHITE)

        val window = musicDialog.window
        val durationSec = window.musicDurationSec.get()
        val windowX = (window.x.get() / durationSec)
        val windowW = (window.widthSec.get() / durationSec)

        if (overallPane.isLeftClickDown && suggestRenderZoomedLast && (System.currentTimeMillis() - suggestRenderZoomedLastTimeMs) < 250L) {
            batch.setColor(1f, 1f, 1f, 1f)
            val overallTex = editor.waveformWindow.overallBuffer.colorBufferTexture
            batch.draw(overallTex, x, y - h, w, h, windowX, 0f, windowX + windowW, 1f)
        } else {
            zoomedRefreshIndicator.get() // Trigger a refresh if necessary

            batch.setColor(1f, 1f, 1f, 1f)
            val tex = editor.waveformWindow.windowedBuffer.colorBufferTexture
            batch.draw(tex, x, y - h, w, h)
        }

        ColorStack.pop()
        batch.packedColor = lastPackedColor
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelfAfterChildren(originX, originY, batch)
        val renderBounds = this.contentZone
        val x = renderBounds.x.get() + originX
        val y = originY - renderBounds.y.get()
        val w = renderBounds.width.get()
        val h = renderBounds.height.get()
        val lastPackedColor = batch.packedColor

        val tmpColor: Color = ColorStack.getAndPush()
        tmpColor.set(Color.WHITE)

        val window = musicDialog.window
        val durationSec = window.musicDurationSec.get()
        val windowX = (window.x.get() / durationSec) * w
        val windowW = (window.widthSec.get() / durationSec) * w

//        batch.color = tmpColor
//        batch.drawRect(x + windowX, y - h, windowW, h, 2f)

        ColorStack.pop()
        batch.packedColor = lastPackedColor
    }

    override fun onMouseMovedOrDragged(x: Float, y: Float) {
        super.onMouseMovedOrDragged(x, y)

        val relX = lastMouseRelative.x
        val window = musicDialog.window
        val durationSec = window.widthSec.get()
        val fullDurationSec = window.musicDurationSec.get()
        val windowX = window.x.get()
        val centreSec = (relX / this.contentZone.width.get()) * durationSec + windowX

        if (isLeftClickDown) {
            val newPos = centreSec.coerceIn(0f, fullDurationSec)
            val variable = when (musicDialog.currentMarker.getOrCompute()) {
                MusicDialog.MarkerType.FIRST_BEAT -> window.firstBeat
                MusicDialog.MarkerType.LOOP_START -> window.loopStart
                MusicDialog.MarkerType.LOOP_END -> window.loopEnd
            }
            variable.set(newPos)
        } else if (isRightClickDown) {
            window.playbackStart.set(centreSec.coerceIn(0f, fullDurationSec))
        }
    }
}