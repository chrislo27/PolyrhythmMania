package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.ui.*
import kotlin.system.measureNanoTime


class ZoomedWavePane(musicDialog: MusicDialog, val overallPane: OverallWavePane) : AbstrWavePane(musicDialog) {

    private var suggestRenderZoomedLast: Boolean = false
    private var suggestRenderZoomedLastTimeMs: Long = 0L

    /**
     * A listener to the window values. NEVER change what it is set/bound to!
     * It should only be getOrCompute'd from the GL thread.
     */
    private val zoomedRefreshIndicator: Var<Int> = Var(0)

    init {
        this.isFullWidth = false
        zoomedRefreshIndicator.sideEffecting(0) { existing ->
            val window = musicDialog.window
            window.x.use()
            window.widthSec.use()
            window.musicDurationSec.use()

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
        val x = renderBounds.x.getOrCompute() + originX
        val y = originY - renderBounds.y.getOrCompute()
        val w = renderBounds.width.getOrCompute()
        val h = renderBounds.height.getOrCompute()
        val lastPackedColor = batch.packedColor

        val tmpColor: Color = ColorStack.getAndPush()
        tmpColor.set(Color.WHITE)

        val window = musicDialog.window
        val durationSec = window.musicDurationSec.getOrCompute()
        val windowX = (window.x.getOrCompute() / durationSec)
        val windowW = (window.widthSec.getOrCompute() / durationSec)

        if (overallPane.isLeftClickDown && suggestRenderZoomedLast && (System.currentTimeMillis() - suggestRenderZoomedLastTimeMs) < 250L) {
            batch.setColor(1f, 1f, 1f, 1f)
            val overallTex = editor.waveformWindow.overallBuffer.colorBufferTexture
            batch.draw(overallTex, x, y - h, w, h, windowX, 0f, windowX + windowW, 1f)
        } else {
            zoomedRefreshIndicator.getOrCompute() // Trigger a refresh if necessary

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
        val x = renderBounds.x.getOrCompute() + originX
        val y = originY - renderBounds.y.getOrCompute()
        val w = renderBounds.width.getOrCompute()
        val h = renderBounds.height.getOrCompute()
        val lastPackedColor = batch.packedColor

        val tmpColor: Color = ColorStack.getAndPush()
        tmpColor.set(Color.WHITE)

        val window = musicDialog.window
        val durationSec = window.musicDurationSec.getOrCompute()
        val windowX = (window.x.getOrCompute() / durationSec) * w
        val windowW = (window.widthSec.getOrCompute() / durationSec) * w

//        batch.color = tmpColor
//        batch.drawRect(x + windowX, y - h, windowW, h, 2f)

        ColorStack.pop()
        batch.packedColor = lastPackedColor
    }

    override fun onMouseMovedOrDragged(x: Float, y: Float) {
        super.onMouseMovedOrDragged(x, y)

        val relX = lastMouseRelative.x
        val window = musicDialog.window
        val durationSec = window.widthSec.getOrCompute()
        val fullDurationSec = window.musicDurationSec.getOrCompute()
        val windowX = window.x.getOrCompute()
        val centreSec = (relX / this.contentZone.width.getOrCompute()) * durationSec + windowX

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