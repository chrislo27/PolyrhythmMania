package polyrhythmmania.soundsystem.javasound

import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.beads.toJavaAudioFormat
import java.lang.IllegalArgumentException
import javax.sound.sampled.*


/**
 * Handles the list of mixers for a particular [AudioFormat].
 */
class MixerHandler(val audioFormat: AudioFormat) {
    
    companion object {
        
        private val datalineInfoMap: MutableMap<AudioFormat, DataLine.Info> = mutableMapOf()
        
        val defaultMixerHandler: MixerHandler by lazy { MixerHandler(SoundSystem.DEFAULT_AUDIO_FORMAT.toJavaAudioFormat()) }

        @Synchronized
        private fun getDataLineForFormat(audioFormat: AudioFormat): DataLine.Info = datalineInfoMap.getOrPut(audioFormat) {
            DataLine.Info(SourceDataLine::class.java, audioFormat)
        }

        private fun getSupportedMixersForAudioFormat(audioFormat: AudioFormat): SupportedMixers {
            val datalineInfo = getDataLineForFormat(audioFormat)
            var supportedMixers: List<Mixer> = AudioSystem.getMixerInfo().map { AudioSystem.getMixer(it) }.filter { mixer ->
                try {
                    // Attempt to get the line. If it is not supported it will throw an exception.
                    mixer.getLine(datalineInfo)
//                    Paintbox.LOGGER.debug("Mixer ${mixer.mixerInfo} is compatible for outputting.")
                    true
                } catch (e: LineUnavailableException) {
//                    Paintbox.LOGGER.debug("Mixer ${mixer.mixerInfo} is NOT compatible for outputting!")
                    false
                } catch (e: IllegalArgumentException) {
//                    Paintbox.LOGGER.debug("Mixer ${mixer.mixerInfo} is NOT compatible for outputting!")
                    false
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            val defaultMixer = AudioSystem.getMixer(null)
            
            if (defaultMixer in supportedMixers && supportedMixers.first() != defaultMixer) {
                supportedMixers = listOf(defaultMixer) + (supportedMixers - defaultMixer)
            }
            
            return SupportedMixers(supportedMixers, defaultMixer)
        }
    }
    
    data class SupportedMixers(val mixers: List<Mixer>, val defaultMixer: Mixer)
    
    val supportedMixers: SupportedMixers = getSupportedMixersForAudioFormat(audioFormat)
    val defaultMixer: Mixer = supportedMixers.defaultMixer
    
    @Volatile
    var recommendedMixer: Mixer = defaultMixer
    
}