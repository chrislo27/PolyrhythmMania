package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import io.github.chrislo27.paintbox.ui.*
import io.github.chrislo27.paintbox.util.gdxutils.drawRect


class OverallWavePane(musicDialog: MusicDialog) : AbstrWavePane(musicDialog) {

    init {
        this.isFullWidth = true
        this.addChild(ImageNode(TextureRegion(editor.waveformWindow.overallBuffer.colorBufferTexture).apply {
            this.flip(false, true)
        }, renderingMode = ImageRenderingMode.FULL))
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

        batch.color = tmpColor
        batch.drawRect(x + windowX, y - h, windowW, h, 2f)

        ColorStack.pop()
        batch.packedColor = lastPackedColor
    }

    override fun onMouseMovedOrDragged(x: Float, y: Float) {
        super.onMouseMovedOrDragged(x, y)
        
        val relX = lastMouseRelative.x
        val window = musicDialog.window
        val durationSec = window.musicDurationSec.getOrCompute()
        val centreSec = (relX / this.contentZone.width.getOrCompute()) * durationSec
        
        if (isLeftClickDown) {
            val widthSec = window.widthSec.getOrCompute()
            var leftSec = centreSec - (widthSec / 2f)
            if (leftSec + widthSec > durationSec) leftSec = durationSec - widthSec
            if (leftSec < 0) leftSec = 0f

            window.x.set(leftSec)
        } else if (isRightClickDown) {
            window.playbackStart.set(centreSec.coerceIn(0f, durationSec))
        }
    }
}