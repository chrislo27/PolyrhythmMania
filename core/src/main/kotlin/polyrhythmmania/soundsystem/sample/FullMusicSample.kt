package polyrhythmmania.soundsystem.sample

import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption


/**
 * A [FullMusicSample] has its PCM data fully decoded already and can benefit from some static information.
 * The [FileChannel] used will be in read-only mode.
 */
class FullMusicSample(
        val pcmDataFile: Path,
        sampleRate: Float = 44_100f, nChannels: Int = 2
) : MusicSample(FileChannel.open(pcmDataFile, StandardOpenOption.READ), sampleRate, nChannels) {

    override val fileSize: Long = synchronized(byteChannel) { byteChannel.size() }
    
    init {
        startBuffer.populate(0)
    }
}
