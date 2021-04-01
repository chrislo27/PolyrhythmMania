package polyrhythmmania.engine

import polyrhythmmania.beads.TimingProvider
import polyrhythmmania.beads.TimingListener
import polyrhythmmania.engine.tempo.TempoMap


open class Clock(val timingProvider: TimingProvider) {

    val tempos: TempoMap = TempoMap(120f)
    
    @Volatile
    var seconds: Float = 0f
        set(value) {
            val capped = value.coerceAtLeast(0f)
            field = capped
            beat = tempos.secondsToBeats(capped)
        }
    @Volatile
    var beat: Float = 0f
        private set
    
//    @Volatile
//    var doUpdateSeconds: Boolean = false
    
    init {
        timingProvider.listeners += TimingListener { oldSeconds, newSeconds -> 
//            if (doUpdateSeconds) {
//                val delta = newSeconds - oldSeconds
//                updateSeconds(delta)
//            }
            val delta = newSeconds - oldSeconds
            updateSeconds(delta)
        }
    }
    
    protected open fun updateSeconds(delta: Float) {
        seconds += delta
    }
}