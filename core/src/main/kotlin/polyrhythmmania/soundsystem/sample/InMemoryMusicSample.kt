package polyrhythmmania.soundsystem.sample


/**
 * An [InMemoryMusicSample] has its PCM data fully decoded in memory as a [ByteArray].
 */
class InMemoryMusicSample(
        val data: ByteArray,
        sampleRate: Float = 44_100f, nChannels: Int = 2
) : MusicSample(ByteArraySeekableByteChannel(data), sampleRate, nChannels) {

    override val fileSize: Long = synchronized(byteChannel) { byteChannel.size() }
    
    init {
        startBuffer.populate(0)
    }
}
