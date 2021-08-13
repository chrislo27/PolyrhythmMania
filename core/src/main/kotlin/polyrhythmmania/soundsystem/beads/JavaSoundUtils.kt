package polyrhythmmania.soundsystem.beads

import net.beadsproject.beads.core.IOAudioFormat
import net.beadsproject.beads.core.io.JavaSoundAudioIO
import javax.sound.sampled.*


object JavaSoundUtils {
    
    fun getSourceDataLineInfo(audioFormat: AudioFormat): DataLine.Info =
            DataLine.Info(SourceDataLine::class.java, audioFormat)
    
    fun getSourceDataLineInfo(ioAudioFormat: IOAudioFormat): DataLine.Info = 
            getSourceDataLineInfo(ioAudioFormat.toJavaAudioFormat())
    
    fun getAllSupportedMixers(datalineInfo: DataLine.Info): List<MixerInfo> {
        return AudioSystem.getMixerInfo().map { 
            AudioSystem.getMixer(it) 
        }.map { mixer ->
            val outputtingSupported = try {
                // Attempt to get the line. If it is not supported it will throw an exception.
                mixer.getLine(datalineInfo)
                true
            } catch (e: Exception) {
                false
            }
            MixerInfo(mixer, outputtingSupported)
        }
    }
    
    fun getDefaultMixer(mixers: List<Mixer>): Mixer {
        val first = mixers.firstOrNull {
            val name = it.mixerInfo.name
            !name.startsWith("Port ") || name.contains("Primary Sound Driver")
        }
        return first ?: mixers.first()
    }
    
}

data class MixerInfo(val mixer: Mixer, val canOutput: Boolean)

fun IOAudioFormat.toJavaAudioFormat(): AudioFormat = AudioFormat(sampleRate, bitDepth, outputs, signed, bigEndian)
fun AudioFormat.toBeadsAudioFormat(): IOAudioFormat = IOAudioFormat(sampleRate, sampleSizeInBits, channels, channels,
        encoding != AudioFormat.Encoding.PCM_UNSIGNED, isBigEndian)

fun JavaSoundAudioIO.selectMixer(mixer: Mixer) {
    val clazz = JavaSoundAudioIO::class.java
    clazz.getDeclaredField("mixer").also { mxr ->
        mxr.isAccessible = true
        mxr.set(this@selectMixer, mixer)
    }
    println("JavaSoundAudioIO: Chosen mixer is ${mixer.mixerInfo.name}.")
}
