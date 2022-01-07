package polyrhythmmania.soundsystem.javasound

import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.Mixer


/**
 * Tracks references for [Mixer]s to prevent issues with simultaneous closing/opening.
 */
object MixerTracking {
    
    private val trackedMixers: MutableMap<Mixer, Int> = ConcurrentHashMap()
    
    fun trackMixer(mixer: Mixer) {
        val tracked = trackedMixers
        val refs = tracked[mixer] ?: 0
        tracked[mixer] = refs + 1
    }

    fun untrackMixer(mixer: Mixer) {
        val tracked = trackedMixers
        val refs = tracked[mixer] ?: 1
        val newRefs = refs - 1
        if (newRefs <= 0) {
            tracked.remove(mixer)
            mixer.close()
        } else {
            tracked[mixer] = newRefs
        }
    }
}