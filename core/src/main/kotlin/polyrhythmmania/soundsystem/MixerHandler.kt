package polyrhythmmania.soundsystem

import javax.sound.sampled.*


/**
 * Handles the list of mixers for a particular [AudioFormat].
 */
class MixerHandler(val audioFormat: AudioFormat) {
    
    companion object {
        private val datalineInfoMap: MutableMap<AudioFormat, DataLine.Info> = mutableMapOf()

        @Synchronized
        fun getDataLineForFormat(audioFormat: AudioFormat): DataLine.Info = datalineInfoMap.getOrPut(audioFormat) {
            DataLine.Info(SourceDataLine::class.java, audioFormat)
        }

        private fun getSupportedMixersForAudioFormat(audioFormat: AudioFormat): List<Mixer> {
            val datalineInfo = getDataLineForFormat(audioFormat)
            val supportedMixers: List<Mixer> = AudioSystem.getMixerInfo().map { AudioSystem.getMixer(it) }.filter { mixer ->
                try {
                    // Attempt to get the line. If it is not supported it will throw an exception.
                    mixer.getLine(datalineInfo)
//                    Paintbox.LOGGER.debug("Mixer ${mixer.mixerInfo} is compatible for outputting.")
                    true
                } catch (e: Exception) {
//                    Paintbox.LOGGER.debug("Mixer ${mixer.mixerInfo} is NOT compatible for outputting!")
                    false
                }
            }
            val defaultMixer = AudioSystem.getMixer(null)
            if (defaultMixer in supportedMixers && supportedMixers.first() != defaultMixer) {
                return listOf(defaultMixer) + (supportedMixers - defaultMixer)
            }
            return supportedMixers
        }
        
        private fun getDefaultMixerFromList(list: List<Mixer>): Mixer {
//            val first = list.firstOrNull {
//                val name = it.mixerInfo.name
//                !name.startsWith("Port ") || name.contains("Primary Sound Driver")
//            }
//            return first ?: list.first()
            return list.first() // The first mixer is usually the default one. Maybe...
        }
    }
    
    val supportedMixers: List<Mixer> = getSupportedMixersForAudioFormat(audioFormat)
    val defaultMixer: Mixer = getDefaultMixerFromList(supportedMixers)
    
    @Volatile
    var recommendedMixer: Mixer = defaultMixer
    
}