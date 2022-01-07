package polyrhythmmania.soundsystem

import net.beadsproject.beads.core.AudioIO
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import polyrhythmmania.Localization
import polyrhythmmania.soundsystem.beads.DaemonJavaSoundAudioIO
import polyrhythmmania.soundsystem.beads.OpenALAudioIO
import javax.sound.sampled.Mixer


sealed class RealtimeOutput {
    
    object OpenAL : RealtimeOutput() {
        override fun getName(): ReadOnlyVar<String> {
            return Localization.getVar("mainMenu.advancedAudio.outputInterface.openAL")
        }

        override fun createAudioIO(): AudioIO {
            return OpenALAudioIO()
        }
    }
    
    class JavaSound(val mixer: Mixer) : RealtimeOutput() {
        override fun getName(): ReadOnlyVar<String> {
            return Var(mixer.mixerInfo?.name ?: "<unknown name (${mixer})>")
        }

        override fun createAudioIO(): AudioIO {
            return DaemonJavaSoundAudioIO(mixer)
        }
    }
    
    abstract fun getName(): ReadOnlyVar<String>
    
    abstract fun createAudioIO(): AudioIO
}
