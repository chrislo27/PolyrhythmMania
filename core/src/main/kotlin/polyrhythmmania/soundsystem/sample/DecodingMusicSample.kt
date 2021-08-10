package polyrhythmmania.soundsystem.sample

import com.badlogic.gdx.utils.StreamUtils
import java.io.RandomAccessFile
import java.nio.channels.FileChannel


class DecodingMusicSample(val randomAccessFile: RandomAccessFile,
                          fileChannel: FileChannel, sampleRate: Float = 44_100f, nChannels: Int = 2)
    : MusicSample(fileChannel, sampleRate, nChannels) {

    override val fileSize: Long
        get() = synchronized(fileChannel) { fileChannel.size() }

    override fun close() {
        super.close()
        StreamUtils.closeQuietly(randomAccessFile)
    }
}