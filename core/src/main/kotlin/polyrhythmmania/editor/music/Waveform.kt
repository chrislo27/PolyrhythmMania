package polyrhythmmania.editor.music

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.soundsystem.sample.MusicSample
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sqrt


/**
 * Reads a [MusicSample] and caches waveform information.
 */
class Waveform(val musicSample: MusicSample) {
    
    val nChannels: Int = musicSample.nChannels
    val nSamples: Long = musicSample.nFrames
    val numSummaries256: Int = (musicSample.nFrames / 256 + 1).toInt()
    val numSummaries2k: Int = (musicSample.nFrames / 2048 + 1).toInt()
    val numSummaries65k: Int = (musicSample.nFrames / 65536 + 1).toInt()
    var summary256: Array<Array<Summary>> = Array(nChannels) { Array(numSummaries256) { Summary.ZERO } }
        private set
    var summary2k: Array<Array<Summary>> = Array(nChannels) { Array(numSummaries2k) { Summary.ZERO } }
        private set
    var summary65k: Array<Array<Summary>> = Array(nChannels) { Array(numSummaries65k) { Summary.ZERO } }
        private set

    fun generateSummaries() {
        val buffer = MusicSample.Buffer(musicSample, samples = 65536)

        val newSummary256: Array<Array<Summary>> = Array(nChannels) { Array(numSummaries256) { Summary.ZERO } }
        val newSummary2k: Array<Array<Summary>> = Array(nChannels) { Array(numSummaries2k) { Summary.ZERO } }
        val newSummary65k: Array<Array<Summary>> = Array(nChannels) { Array(numSummaries65k) { Summary.ZERO } }

        var index256 = 0
        var index2k = 0
        var index65k = 0
        buffer.populate(0)
        while (buffer.size > 0) {
            var consumed256 = buffer.size
            val startIndex256 = index256
            while (consumed256 > 0 && index256 < numSummaries256) {
                val amt = consumed256.coerceAtMost(256)
                for (ch in 0..<nChannels) {
                    val summary = createSummary(buffer, ch, (index256 - startIndex256) * 256, amt)
                    newSummary256[ch][index256] = summary
                }
                consumed256 -= amt
                index256++
            }
            
            var consumed2k = buffer.size
            val startIndex2k = index2k
            while (consumed2k > 0 && index2k < numSummaries2k) {
                val amt = consumed2k.coerceAtMost(2048)
                for (ch in 0..<nChannels) {
                    val summary = createSummary(buffer, ch, (index2k - startIndex2k) * 2048, amt)
                    newSummary2k[ch][index2k] = summary
                }
                consumed2k -= amt
                index2k++
            }

            val startIndex65k = index65k
            var consumed65k = buffer.size
            while (consumed65k > 0 && index65k < numSummaries65k) {
                val amt = consumed65k.coerceAtMost(65536)
                for (ch in 0..<nChannels) {
                    val summary = createSummary(buffer, ch, (index65k - startIndex65k) * 65536, amt)
                    newSummary65k[ch][index65k] = summary
                }
                consumed65k -= amt
                index65k++
            }
            
            // Populate with largest granularity
            buffer.populate(index65k * 65536)
        }

        this.summary256 = newSummary256
        this.summary2k = newSummary2k
        this.summary65k = newSummary65k
    }

    private fun createSummary(buffer: MusicSample.Buffer, channel: Int, start: Int, amt: Int): Summary {
        if (amt <= 0) return Summary.ZERO
        
        var min = Float.MAX_VALUE
        var max = Float.MIN_VALUE
        var rms = 0.0 // Accumulates the sum of the squares first.
        
        for (i in start..<(start + amt)) {
            val x = buffer.data[channel][i]
            if (x < min) min = x
            if (x > max) max = x
            val d = x.toDouble()
            rms += (d * d)
        }
        
        // Compute root mean square (RMS).
        // x_RMS = sqrt( (1/n) * (x_1^2 + x_2^2 + ... + x_n^2) )
        rms /= amt
        rms = sqrt(rms)
        
        return Summary(min, max, rms.toFloat())
    }
    
    fun generateTestImage(file: FileHandle) {
        // Generate output imgs
        val waveform = this
        val num256 = waveform.numSummaries256
        val channels = waveform.nChannels
        val height = 256
        val pixmap = Pixmap(num256, height * channels * 3 + 2, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.BLACK)
        pixmap.fillRectangle(0, 0, pixmap.width, pixmap.height)

        val mainColor = Color(5 / 255f, 5 / 255f, 255 / 255f, 1f)
        val rmsColor = Color(90 / 255f, 90 / 255f, 255 / 255f, 1f)

        val summary256 = waveform.summary256
        pixmap.setColor(mainColor)
        for (i in 0..<waveform.numSummaries256) {
            for (ch in 0..<channels) {
                val summary = summary256[ch][i]
                val min = summary.min.coerceIn(-1f, 1f)
                val max = summary.max.coerceIn(-1f, 1f)
                val h = ((max - min) * (height / 2)).roundToInt() + 1
                pixmap.fillRectangle(i, ch * height + (height / 2) + ((-min) * (height / 2)).roundToInt() - h, 1, h)
            }
        }
        pixmap.setColor(rmsColor)
        for (i in 0..<waveform.numSummaries256) {
            for (ch in 0..<channels) {
                val summary = summary256[ch][i]
                val rms = summary.rms.coerceIn(0f, 1f)
                val h = (rms * height).roundToInt() + 1
                pixmap.fillRectangle(i, ch * height + (height / 2) - (h / 2), 1, h)
            }
        }
        val summary2k = waveform.summary2k
        pixmap.setColor(mainColor)
        for (i in 0..<waveform.numSummaries2k) {
            for (ch in 0..<channels) {
                val summary = summary2k[ch][i]
                val min = summary.min.coerceIn(-1f, 1f)
                val max = summary.max.coerceIn(-1f, 1f)
                val h = ((max - min) * (height / 2)).roundToInt() + 1
                pixmap.fillRectangle(i, (height * channels + 1) + ch * height + (height / 2) + ((-min) * (height / 2)).roundToInt() - h, 1, h)
            }
        }
        pixmap.setColor(rmsColor)
        for (i in 0..<waveform.numSummaries2k) {
            for (ch in 0..<channels) {
                val summary = summary2k[ch][i]
                val rms = summary.rms.coerceIn(0f, 1f)
                val h = (rms * height).roundToInt() + 1
                pixmap.fillRectangle(i, (height * channels + 1) + ch * height + (height / 2) - (h / 2), 1, h)
            }
        }
        val summary65k = waveform.summary65k
        pixmap.setColor(mainColor)
        for (i in 0..<waveform.numSummaries65k) {
            for (ch in 0..<channels) {
                val summary = summary65k[ch][i]
                val min = summary.min.coerceIn(-1f, 1f)
                val max = summary.max.coerceIn(-1f, 1f)
                val h = ((max - min) * (height / 2)).roundToInt() + 1
                pixmap.fillRectangle(i, (height * channels * 2 + 2) + ch * height + (height / 2) + ((-min) * (height / 2)).roundToInt() - h, 1, h)
            }
        }
        pixmap.setColor(rmsColor)
        for (i in 0..<waveform.numSummaries65k) {
            for (ch in 0..<channels) {
                val summary = summary65k[ch][i]
                val rms = summary.rms.coerceIn(0f, 1f)
                val h = (rms * height).roundToInt() + 1
                pixmap.fillRectangle(i, (height * channels * 2 + 2) + ch * height + (height / 2) - (h / 2), 1, h)
            }
        }

        pixmap.setColor(1f, 0f, 1f, 1f)
        pixmap.fillRectangle(0, height * channels, pixmap.width, 1)
        pixmap.fillRectangle(0, height * channels * 2 + 1, pixmap.width, 1)
        PixmapIO.writePNG(file, pixmap)
        pixmap.disposeQuietly()
    }

}