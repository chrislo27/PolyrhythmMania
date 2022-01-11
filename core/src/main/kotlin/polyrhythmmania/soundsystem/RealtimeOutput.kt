package polyrhythmmania.soundsystem

import net.beadsproject.beads.core.AudioIO
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import polyrhythmmania.Localization
import polyrhythmmania.soundsystem.beads.DaemonJavaSoundAudioIO
import polyrhythmmania.soundsystem.beads.OpenALAudioIO
import javax.sound.sampled.Mixer


sealed class RealtimeOutput {
    
    class OpenAL(val audioDeviceSettings: AudioDeviceSettings) : RealtimeOutput() {
        override fun getName(): ReadOnlyVar<String> {
            return Var("OpenAL{$audioDeviceSettings}")
        }

        override fun createAudioIO(): AudioIO {
            return OpenALAudioIO(audioDeviceSettings)
        }
    }
    
    class JavaSound(val mixer: Mixer) : RealtimeOutput() {
        override fun getName(): ReadOnlyVar<String> {
            return Var(mixer.mixerInfo?.name ?: "<unknown mixer name (${mixer})>")
        }

        override fun createAudioIO(): AudioIO {
            return DaemonJavaSoundAudioIO(mixer)
        }
    }
    
    abstract fun getName(): ReadOnlyVar<String>
    
    abstract fun createAudioIO(): AudioIO
}
