package polyrhythmmania.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.IntMap
import paintbox.util.gdxutils.NestedFrameBuffer
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.fillRect
import polyrhythmmania.editor.music.EditorMusicData
import polyrhythmmania.editor.music.Summary
import polyrhythmmania.editor.music.Waveform
import polyrhythmmania.editor.pane.dialog.MusicDialog
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min


/**
 * Handles the frame buffer of the waveform view.
 * This must be instantiated on the GL thread and all functions must be called on the GL thread.
 */
class WaveformWindow(val editor: Editor) : Disposable {
    
    companion object {
        const val SECONDS_BLOCK_WIDTH: Int = 128
        const val SECONDS_BLOCK_HEIGHT: Int = 48
    }
    
    private data class CacheBlock(val index: Int, val x: Int, val y: Int, val textureRegion: TextureRegion,
                                  var generatedAt: Long = 0L, var secondPos: Int = -1)

    private val tmpMatrix: Matrix4 = Matrix4()
    val width: Int = 1200
    val height: Int = MusicDialog.WAVEFORM_HEIGHT * 2 /* 2 channels */
    
    val overallBuffer: NestedFrameBuffer = NestedFrameBuffer(Pixmap.Format.RGBA8888, width, height, false)
    val windowedBuffer: NestedFrameBuffer = NestedFrameBuffer(Pixmap.Format.RGBA8888, width, height, false)
    val secondsBuffer: NestedFrameBuffer = NestedFrameBuffer(Pixmap.Format.RGBA8888, 512, 512, false)
    
    val camera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, width.toFloat(), height.toFloat())
        this.update()
    }
    val secondsCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, secondsBuffer.width.toFloat(), secondsBuffer.height.toFloat())
        this.update()
    }
    
    val minmaxColor: Color = Color(5 / 255f, 5 / 255f, 255 / 255f, 1f)
    val rmsColor: Color = Color(90 / 255f, 90 / 255f, 255 / 255f, 1f)
    
    private val secondsBlocksWidth: Int = secondsBuffer.width / SECONDS_BLOCK_WIDTH
    private val secondsBlocksHeight: Int = secondsBuffer.height / SECONDS_BLOCK_HEIGHT
    private val secondsCacheBlocks: List<CacheBlock> = List(secondsBlocksWidth * secondsBlocksHeight) { i ->
        val x = i % secondsBlocksWidth
        val y = i / secondsBlocksWidth
        val texReg = TextureRegion(secondsBuffer.colorBufferTexture,
                x * SECONDS_BLOCK_WIDTH, y * SECONDS_BLOCK_HEIGHT,
                SECONDS_BLOCK_WIDTH, SECONDS_BLOCK_HEIGHT)
        CacheBlock(i, x, y, texReg)
    }
    private val cacheByAge: MutableList<CacheBlock> = secondsCacheBlocks.toMutableList() // FIFO
    private val cacheBySeconds: IntMap<CacheBlock> = IntMap() // Seconds -> CacheBlock

    init {
        listOf(overallBuffer, windowedBuffer, secondsBuffer).forEach { 
            it.colorBufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
    }
    
    private fun getCacheBlockIndex(x: Int, y: Int): Int = y * secondsBlocksWidth + x % secondsBlocksWidth

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
                val i = (x * (summaryData[0].size) / width.toFloat()).toInt()
                if (i !in 0 until summaryData[0].size) continue
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
                val i = (x * (summaryData[0].size) / width.toFloat()).toInt()
                if (i !in 0 until summaryData[0].size) continue
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
            val sampleCount = (window.widthSec.get() * sampleRate).toInt()
            val startSample = (window.x.get() * sampleRate).toInt()
            val entireMusicSamples = waveform.musicSample.nFrames
            
            val samplesPerPx = sampleCount / width
            val granularity: Int = if (samplesPerPx >= 65536) 65536 else if (samplesPerPx >= 2048) 2048 else 256
            val summaryData: Array<Array<Summary>> = if (granularity == 65536) waveform.summary65k else if (granularity == 2048) waveform.summary2k else waveform.summary256
            val channels = summaryData.size.coerceAtMost(2)
            val heightPerCh = height / channels
            
            batch.color = minmaxColor
            for (x in 0 until width) {
                val i = (((x / width.toFloat() * sampleCount) + startSample) / entireMusicSamples.toFloat() * summaryData[0].size).toInt()
                if (i !in 0 until summaryData[0].size) continue
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
                val i = (((x / width.toFloat() * sampleCount) + startSample) / entireMusicSamples.toFloat() * summaryData[0].size).toInt()
                if (i !in 0 until summaryData[0].size) continue
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
    
    fun invalidateBlockCache() {
        cacheByAge.forEach { it.generatedAt = 0L }
        cacheBySeconds.clear()
    }
    
    fun getSecondsBlock(seconds: Int): TextureRegion? {
        val musicData = editor.musicData
        val waveform = musicData.waveform
        if (waveform != null) {
            val durationSec = (waveform.musicSample.lengthMs / 1000).toFloat()
            val durationSecCeil = ceil(durationSec).toInt()
            if (seconds in 0 until durationSecCeil) {
                // Find in cache if already exists.
                val cached = cacheBySeconds[seconds]
                if (cached != null)
                    return cached.textureRegion

                // Doesn't exist. Create it, and evict the oldest (first in cacheByAge) if needed
                val cacheBlock: CacheBlock = cacheByAge.firstOrNull { it.generatedAt <= 0L } ?: run {
                    val first = cacheByAge.removeFirst()
                    cacheByAge.add(first) // Move to the end of the list
                    first
                }
                cacheBySeconds.remove(cacheBlock.secondPos)
                cacheBlock.generatedAt = System.currentTimeMillis()
                cacheBlock.secondPos = seconds
                cacheBySeconds.put(seconds, cacheBlock)
                Gdx.app.postRunnable {
                    generateForCacheBlock(cacheBlock, musicData, waveform, seconds)
//                    println("[WaveformWindow] Generated cache block for seconds $seconds")
                }
                
                return cacheBlock.textureRegion
            } else {
                return null
            }
        } else {
            return null
        }
    }
    
    private fun generateForCacheBlock(cacheBlock: CacheBlock, musicData: EditorMusicData, waveform: Waveform, seconds: Int) {
        val batch = editor.main.batch
        val wasBatching = batch.isDrawing
        if (wasBatching) batch.end()

        val camera = secondsCamera
        val lastPackedColor = batch.packedColor
        tmpMatrix.set(batch.projectionMatrix)
        camera.update()
        batch.projectionMatrix = camera.combined
        secondsBuffer.begin()
        batch.begin()

        val sampleRate = waveform.musicSample.sampleRate
        val sampleCount = (1 * sampleRate).toInt()
        val startSample = (seconds * sampleRate).toInt()
        val entireMusicSamples = waveform.musicSample.nFrames
        
        val width = SECONDS_BLOCK_WIDTH

        val samplesPerPx = sampleCount / width
        val granularity: Int = if (samplesPerPx >= 65536) 65536 else if (samplesPerPx >= 2048) 2048 else 256
        val summaryData: Array<Array<Summary>> = if (granularity == 65536) waveform.summary65k else if (granularity == 2048) waveform.summary2k else waveform.summary256
        val channels = summaryData.size.coerceAtMost(2)
        val heightPerCh = SECONDS_BLOCK_HEIGHT// / channels // All channels merged
        
        val regionX = cacheBlock.x * SECONDS_BLOCK_WIDTH
        val regionY = cacheBlock.y * SECONDS_BLOCK_HEIGHT
        
        val oldSrcFunc = batch.blendSrcFunc
        val oldDstFunc = batch.blendDstFunc
        batch.setBlendFunction(GL20.GL_ZERO, GL20.GL_ZERO)
        batch.fillRect(regionX.toFloat(), regionY.toFloat(),
                ceil(regionX + SECONDS_BLOCK_WIDTH.toFloat()) - regionX /* reduces bleed over into next block */,
                SECONDS_BLOCK_HEIGHT.toFloat())
        batch.flush()
        batch.setBlendFunction(oldSrcFunc, oldDstFunc)
        
        batch.color = minmaxColor
        for (x in 0 until width) {
            val i = (((x / width.toFloat() * sampleCount) + startSample) / entireMusicSamples.toFloat() * summaryData[0].size).toInt()
            if (i !in 0 until summaryData[0].size) continue
            val min = (if (channels == 1) summaryData[0][i].min else min(summaryData[0][i].min, summaryData[1][i].min)).coerceIn(-1f, 1f)
            val max = (if (channels == 1) summaryData[0][i].max else max(summaryData[0][i].max, summaryData[1][i].max)).coerceIn(-1f, 1f)
            val h = ((max - min) * (heightPerCh / 2))
            batch.fillRect(regionX + x.toFloat(), regionY + (0 + 0.5f) * heightPerCh + (min * (heightPerCh / 2f)), 1f, h)
        }
        batch.color = rmsColor
        for (x in 0 until width) {
            val i = (((x / width.toFloat() * sampleCount) + startSample) / entireMusicSamples.toFloat() * summaryData[0].size).toInt()
            if (i !in 0 until summaryData[0].size) continue
            val rms = (if (channels == 1) summaryData[0][i].rms else ((summaryData[0][i].rms + summaryData[1][i].rms) / 2f)).coerceIn(0f, 1f)
            val h = (rms * (heightPerCh))
            batch.fillRect(regionX + x.toFloat(), regionY + (0 + 0.5f) * heightPerCh - (h / 2), 1f, h)
        }

        batch.end()
        secondsBuffer.end()
        batch.projectionMatrix.set(tmpMatrix)
        batch.packedColor = lastPackedColor

        if (wasBatching) batch.begin()
    }

    override fun dispose() {
        overallBuffer.disposeQuietly()
        windowedBuffer.disposeQuietly()
        secondsBuffer.disposeQuietly()
    }
}