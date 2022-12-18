package polyrhythmmania.soundsystem.sample

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALMusic
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.StreamUtils
import net.beadsproject.beads.core.AudioUtils
import net.beadsproject.beads.data.Sample
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.util.TempFileUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


object GdxAudioReader {
    
    const val TEMP_FILE_NAME: String = "GdxAudioReader-dec"
    
    fun interface AudioLoadListener {
        fun progress(bytesReadSoFar: Long, bytesReadThisChunk: Int)
        
        fun onFinished(totalBytesRead: Long) {
        }
    }
    
    class DecodingHandler(val sample: DecodingMusicSample, val music: OpenALMusic, val fileChannel: FileChannel,
                          val bufferSize: Int, val listener: AudioLoadListener?) {
        fun decode(): Long {
            val audioBytes = ByteArray(bufferSize)
            val byteBuffer = ByteBuffer.wrap(audioBytes)
            var currentLength = 0L
            
            music.reset()
            while (true) {
                byteBuffer.rewind()
                val length = music.read(audioBytes)
                if (length <= 0) break
                
                byteBuffer.rewind()
                byteBuffer.limit(length)
                
                synchronized(fileChannel) {
                    fileChannel.write(byteBuffer, currentLength)
                    currentLength += length
                    sample.updateFilesize(currentLength)
                }

                listener?.progress(currentLength, length)
            }
            
            listener?.onFinished(currentLength)
            
            return currentLength
        }
    }

    private fun musicToPCMFile(music: OpenALMusic, file: File, bufferSize: Int = 4096 * 4, listener: AudioLoadListener? = null): Long {
        file.createNewFile()

        val audioBytes = ByteArray(bufferSize)
        val fileOutStream = FileOutputStream(file)
        var currentLength = 0L

        music.reset()
        while (true) {
            val length = music.read(audioBytes)
            if (length <= 0) break
            
            currentLength += length
            listener?.progress(currentLength, length)
            fileOutStream.write(audioBytes, 0, length)
        }
        StreamUtils.closeQuietly(fileOutStream)
        listener?.onFinished(currentLength)

        return currentLength
    }

    private fun newDecodingMusic(music: OpenALMusic, file: File, bufferSize: Int = 4096 * 8,
                                 listener: AudioLoadListener? = null): Pair<DecodingMusicSample, DecodingHandler> {
        file.createNewFile()

        val randomAccessFile = RandomAccessFile(file, "rw")
        val fileChannel = randomAccessFile.channel
        val sample = DecodingMusicSample(randomAccessFile, fileChannel, music.rate.toFloat(), music.channels)
        val handler = DecodingHandler(sample, music, fileChannel, bufferSize, listener)

        return sample to handler
    }

    fun newSound(handle: FileHandle, listener: AudioLoadListener? = null): BeadsSound {
        val music = Gdx.audio.newMusic(handle) as OpenALMusic
        music.reset()
        val tempFile = TempFileUtils.createTempFile(TEMP_FILE_NAME)
        val bufferSize = 4096 * 4
        // TODO: Can we optimize this by immediately decoding and deinterleaving without writing to a tmp file first?
        val bytesRead = musicToPCMFile(music, tempFile, bufferSize, listener)

        val audioBytesBuffer = ByteArray(bufferSize)
        val nFrames = bytesRead / (2 * music.channels)
        val sample = Sample(0.0, music.channels, music.rate.toFloat())
        sample.resize(nFrames) // WARNING: this could throw OutOfMemoryError
        val interleaved = FloatArray(music.channels * (bufferSize / (2 * music.channels)))
        val sampleData = Array(music.channels) { FloatArray(interleaved.size / music.channels) }

        val bufStream = tempFile.inputStream()
        var currentFrame = 0
        var currentLength = 0L
        while (true) {
            val len = bufStream.read(audioBytesBuffer)
            if (len <= 0)
                break
            
            currentLength += len

            val framesOfDataRead = len / (2 * music.channels)

            AudioUtils.byteToFloat(interleaved, audioBytesBuffer, false, len / 2) // 2 bytes per 16 bit float
            AudioUtils.deinterleave(interleaved, music.channels, framesOfDataRead, sampleData)

            sample.putFrames(currentFrame, sampleData, 0, framesOfDataRead)

            currentFrame += framesOfDataRead
        }
        StreamUtils.closeQuietly(bufStream)
        listener?.onFinished(currentLength)

        try {
            tempFile.delete()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return BeadsSound(sample)
    }

    fun newMusic(handle: FileHandle, listener: AudioLoadListener? = null): BeadsMusic {
        val music = Gdx.audio.newMusic(handle) as OpenALMusic
        music.reset()
        val tempFile = TempFileUtils.createTempFile(TEMP_FILE_NAME)
        val bytesRead = musicToPCMFile(music, tempFile, listener = listener)
        val musicSample: MusicSample = FullMusicSample(tempFile.toPath(), music.rate.toFloat(), music.channels)

        return BeadsMusic(musicSample)
    }

    fun newDecodingMusicSample(handle: FileHandle, listener: AudioLoadListener? = null): Pair<DecodingMusicSample, DecodingHandler> {
        val music = Gdx.audio.newMusic(handle) as OpenALMusic
        music.reset()
        val tempFile = TempFileUtils.createTempFile(TEMP_FILE_NAME)
        
        return newDecodingMusic(music, tempFile, listener = listener)
    }
}