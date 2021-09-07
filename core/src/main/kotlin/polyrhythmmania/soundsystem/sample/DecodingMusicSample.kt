package polyrhythmmania.soundsystem.sample

import com.badlogic.gdx.utils.StreamUtils
import java.io.RandomAccessFile
import java.nio.channels.FileChannel


/**
 * A music sample that may be asynchronously decoded while playback is occurring. This will only work if the rate
 * of decoding is faster than the rate of reading, which is the vast majority of cases.
 */
class DecodingMusicSample(val randomAccessFile: RandomAccessFile,
                          fileChannel: FileChannel, sampleRate: Float = 44_100f, nChannels: Int = 2)
    : MusicSample(fileChannel, sampleRate, nChannels) {
    
    private var _fileSize: Long = synchronized(fileChannel) { fileChannel.size() }
    override val fileSize: Long
        get() = this._fileSize

    /**
     * Updates the backing [fileSize] value using [FileChannel.size].
     * Note that [FileChannel.size] is a relatively slow operation.
     * 
     * This should be called if the backing [randomAccessFile] or [fileChannel] has new data added.
     */
    fun updateFilesize() {
        this._fileSize = synchronized(fileChannel) { fileChannel.size() }
    }
    
    /**
     * Updates the backing [fileSize] value. This avoids a call to [FileChannel.size], but should only be used
     * if the filesize is guaranteed to be correct.
     *
     * This should be called if the backing [randomAccessFile] or [fileChannel] has new data added.
     */
    fun updateFilesize(newSize: Long) {
        this._fileSize = newSize
    }

    override fun close() {
        super.close()
        StreamUtils.closeQuietly(randomAccessFile)
    }
}