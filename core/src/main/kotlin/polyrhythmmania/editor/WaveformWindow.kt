package polyrhythmmania.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.fillRect
import polyrhythmmania.editor.music.Summary
import polyrhythmmania.editor.pane.dialog.MusicDialog
import kotlin.math.roundToInt


/**
 * Handles the frame buffer of the waveform view.
 * This must be instantiated on the GL thread and all functions must be called on the GL thread.
 */
class WaveformWindow(val editor: Editor) : Disposable {

    private val tmpMatrix = Matrix4()
    val width: Int = 1200
    val height: Int = MusicDialog.WAVEFORM_HEIGHT * 2 /* 2 channels */
    val overallBuffer: FrameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, width, height, false)
    val windowedBuffer: FrameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, width, height, false)
    val camera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, width.toFloat(), height.toFloat())
        this.update()
    }
    val minmaxColor: Color = Color(5 / 255f, 5 / 255f, 255 / 255f, 1f)
    val rmsColor: Color = Color(90 / 255f, 90 / 255f, 255 / 255f, 1f)

    init {
        overallBuffer.colorBufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        windowedBuffer.colorBufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }

    fun generateOverall() {
        val batch = editor.main.batch
        val wasBatching = batch.isDrawing
        if (wasBatching) batch.end()

        val lastPackedColor = batch.packedColor
        tmpMatrix.set(batch.projectionMatrix)
        camera.update()
        batch.projectionMatrix = camera.combined
        overallBuffer.begin()
        batch.begin()

        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        
        val waveform = editor.musicData.waveform
        if (waveform != null) {
            val samplesPerPx = waveform.nSamples / width
            val granularity: Int = if (samplesPerPx >= 65536) 65536 else if (samplesPerPx >= 2048) 2048 else 256
            val summaryData: Array<Array<Summary>> = if (granularity == 65536) waveform.summary65k else if (granularity == 2048) waveform.summary2k else waveform.summary256
            val channels = summaryData.size.coerceAtMost(2)
            val heightPerCh = height / channels
            
            batch.color = minmaxColor
            for (x in 0 until width) {
                val i = (x * (summaryData[0].size) / width.toFloat()).toInt().coerceIn(0, summaryData[0].size - 1)
                for (ch in 0 until channels) {
                    val summary = summaryData[ch][i]
                    val min = summary.min.coerceIn(-1f, 1f)
                    val max = summary.max.coerceIn(-1f, 1f)
                    val h = ((max - min) * (heightPerCh / 2))
                    batch.fillRect(x.toFloat(), (ch + 0.5f) * heightPerCh + (min * (heightPerCh / 2f)), 1f, h)
                }
            }
            batch.color = rmsColor
            for (x in 0 until width) {
                val i = (x * (summaryData[0].size) / width.toFloat()).toInt().coerceIn(0, summaryData[0].size - 1)
                for (ch in 0 until channels) {
                    val summary = summaryData[ch][i]
                    val rms = summary.rms.coerceIn(0f, 1f)
                    val h = (rms * (heightPerCh))
                    batch.fillRect(x.toFloat(), (ch + 0.5f) * heightPerCh - (h / 2), 1f, h)
                }
            }
        }

        batch.end()
        overallBuffer.end()
        batch.projectionMatrix.set(tmpMatrix)

        batch.packedColor = lastPackedColor

        if (wasBatching) batch.begin()
    }
    
    fun generateZoomed(window: MusicDialog.Window) {
        val batch = editor.main.batch
        val wasBatching = batch.isDrawing
        if (wasBatching) batch.end()

        val lastPackedColor = batch.packedColor
        tmpMatrix.set(batch.projectionMatrix)
        camera.update()
        batch.projectionMatrix = camera.combined
        windowedBuffer.begin()
        batch.begin()

        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val waveform = editor.musicData.waveform
        if (waveform != null) {
            val sampleRate = waveform.musicSample.sampleRate
            val sampleCount = (window.widthSec.getOrCompute() * sampleRate).toInt()
            val startSample = (window.x.getOrCompute() * sampleRate).toInt()
            val entireMusicSamples = waveform.musicSample.nFrames
            
            val samplesPerPx = sampleCount / width
            val granularity: Int = if (samplesPerPx >= 65536) 65536 else if (samplesPerPx >= 2048) 2048 else 256
            val summaryData: Array<Array<Summary>> = if (granularity == 65536) waveform.summary65k else if (granularity == 2048) waveform.summary2k else waveform.summary256
            val channels = summaryData.size.coerceAtMost(2)
            val heightPerCh = height / channels
            
            batch.color = minmaxColor
            for (x in 0 until width) {
                val i = (((x / width.toFloat() * sampleCount) + startSample) / entireMusicSamples.toFloat() * summaryData[0].size).toInt().coerceIn(0, summaryData[0].size - 1)
                for (ch in 0 until channels) {
                    val summary = summaryData[ch][i]
                    val min = summary.min.coerceIn(-1f, 1f)
                    val max = summary.max.coerceIn(-1f, 1f)
                    val h = ((max - min) * (heightPerCh / 2))
                    batch.fillRect(x.toFloat(), (ch + 0.5f) * heightPerCh + (min * (heightPerCh / 2f)), 1f, h)
                }
            }
            batch.color = rmsColor
            for (x in 0 until width) {
                val i = (((x / width.toFloat() * sampleCount) + startSample) / entireMusicSamples.toFloat() * summaryData[0].size).toInt().coerceIn(0, summaryData[0].size - 1)
                for (ch in 0 until channels) {
                    val summary = summaryData[ch][i]
                    val rms = summary.rms.coerceIn(0f, 1f)
                    val h = (rms * (heightPerCh))
                    batch.fillRect(x.toFloat(), (ch + 0.5f) * heightPerCh - (h / 2), 1f, h)
                }
            }
        }

        batch.end()
        windowedBuffer.end()
        batch.projectionMatrix.set(tmpMatrix)

        batch.packedColor = lastPackedColor

        if (wasBatching) batch.begin()
    }

    override fun dispose() {
        overallBuffer.disposeQuietly()
        windowedBuffer.disposeQuietly()
    }
}