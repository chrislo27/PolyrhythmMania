package polyrhythmmania.soundsystem.sample

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALMusic
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.StreamUtils
import net.beadsproject.beads.core.AudioUtils
import net.beadsproject.beads.data.Sample
import polyrhythmmania.soundsystem.BeadsAudio
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.BeadsSound
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


object GdxAudioReader {

    private fun musicToPCMFile(music: OpenALMusic, file: File, bufferSize: Int = 4096 * 4): Long {
        file.createNewFile()

        val audioBytes = ByteArray(bufferSize)
        val fileOutStream = FileOutputStream(file)
        var currentLength = 0L

        music.reset()
        while (true) {
            val length = music.read(audioBytes)
            if (length <= 0) {
                break
            }

            currentLength += length
            fileOutStream.write(audioBytes, 0, length)
        }
        StreamUtils.closeQuietly(fileOutStream)

        return currentLength
    }

    fun newSound(handle: FileHandle): BeadsSound {
        val music = Gdx.audio.newMusic(handle) as OpenALMusic
        music.reset()
        val tempFile = File.createTempFile("GdxAudioReader-decode", ".tmp").apply {
            deleteOnExit()
        }
        val bufferSize = 4096 * 4
        // TODO: Can we optimize this by immediately decoding and deinterleaving without writing to a tmp file first?
        val bytesRead = musicToPCMFile(music, tempFile, bufferSize)

        val audioBytesBuffer = ByteArray(bufferSize)
        val nFrames = bytesRead / (2 * music.channels)
        val sample = Sample(0.0, music.channels, music.rate.toFloat())
        sample.resize(nFrames) // WARNING: this could throw OutOfMemoryError
        val interleaved = FloatArray(music.channels * (bufferSize / (2 * music.channels)))
        val sampleData = Array(music.channels) { FloatArray(interleaved.size / music.channels) }

        val bufStream = tempFile.inputStream()
        var currentFrame = 0
        while (true) {
            val len = bufStream.read(audioBytesBuffer)
            if (len <= 0)
                break

            val framesOfDataRead = len / (2 * music.channels)

            AudioUtils.byteToFloat(interleaved, audioBytesBuffer, false, len / 2) // 2 bytes per 16 bit float
            AudioUtils.deinterleave(interleaved, music.channels, framesOfDataRead, sampleData)

            sample.putFrames(currentFrame, sampleData, 0, framesOfDataRead)

            currentFrame += framesOfDataRead
        }
        StreamUtils.closeQuietly(bufStream)

        try {
            tempFile.delete()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return BeadsSound(sample)
    }

    fun newMusic(handle: FileHandle): BeadsMusic {
        val music = Gdx.audio.newMusic(handle) as OpenALMusic
        music.reset()
        val tempFile = File.createTempFile("GdxAudioReader-decode", ".tmp").apply {
            deleteOnExit()
        }
        val bytesRead = musicToPCMFile(music, tempFile)
        val musicSample = MusicSample(tempFile.toPath(), music.rate.toFloat(), music.channels)

        return BeadsMusic(musicSample)
    }
}