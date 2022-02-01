package polyrhythmmania.soundsystem.beads

import net.beadsproject.beads.core.IOAudioFormat
import net.beadsproject.beads.core.io.JavaSoundAudioIO
import javax.sound.sampled.*


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
