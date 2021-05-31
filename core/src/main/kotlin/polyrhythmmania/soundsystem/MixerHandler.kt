package polyrhythmmania.soundsystem

import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.*


/**
 * Tracks references for [Mixer]s to prevent issues with simultaneous closing/opening.
 */
object MixerHandler {
    
    private val datalineInfoMap: MutableMap<AudioFormat, DataLine.Info> = mutableMapOf()
    private val trackedMixers: MutableMap<Mixer, Int> = ConcurrentHashMap()
    
    @Synchronized
    fun getDataLineForFormat(audioFormat: AudioFormat): DataLine.Info = datalineInfoMap.getOrPut(audioFormat) { 
        DataLine.Info(SourceDataLine::class.java, audioFormat) 
    }
    
    fun getSupportedMixersForAudioFormat(audioFormat: AudioFormat): List<Mixer> {
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
        return supportedMixers
    }

    fun getDefaultMixer(list: List<Mixer>): Mixer {
        val first = list.firstOrNull {
            val name = it.mixerInfo.name
            // TODO doesn't work in non-English locales
            !name.startsWith("Port ") || name.contains("Primary Sound Driver")
        }
        return first ?: list.first()
    }
    
    fun trackMixer(mixer: Mixer) {
        val refs = trackedMixers[mixer] ?: 0
        trackedMixers[mixer] = refs + 1
    }
    
    fun untrackMixer(mixer: Mixer) {
        val refs = trackedMixers[mixer] ?: 1
        val newRefs = refs - 1
        if (newRefs <= 0) {
            trackedMixers.remove(mixer)
            mixer.close()
        } else {
            trackedMixers[mixer] = newRefs
        }
    }
    
}