package polyrhythmmania.engine.tempo

import java.util.*


/**
 * Represents a mapping of seconds to beats and vice versa, based on [TempoChange]s.
 * 
 * Values below 0 for both beats and seconds are mapped 1:1 (-1 seconds = -1 beats, etc).
 */
class TempoMap(startingGlobalTempo: Float = DEFAULT_STARTING_GLOBAL_TEMPO) {
    
    companion object {
        const val DEFAULT_STARTING_GLOBAL_TEMPO: Float = 120f
    }

    /**
     * The global tempo is always at beat 0, seconds 0.
     */
    private var globalTempo: TempoChange = TempoChange(0f, startingGlobalTempo)
    private var globalTempoData: TempoChangeData = TempoChangeData(globalTempo, 0f)

    private var allTempoChanges: List<TempoChange> = emptyList()
    private val beatMap: NavigableMap<Float, TempoChange> = TreeMap()
    private val beatMapData: NavigableMap<Float, TempoChangeData> = TreeMap()
    private val secondsMapData: NavigableMap<Float, TempoChangeData> = TreeMap()

    init {
        addTempoChange(globalTempo)
    }
    
    fun getGlobalTempo(): TempoChange = globalTempo
    fun getAllTempoChanges(): List<TempoChange> = allTempoChanges
    
    fun addTempoChange(tempoChange: TempoChange): Boolean =
            addTempoChange(tempoChange, true)
    
    fun removeTempoChange(tempoChange: TempoChange): Boolean =
            removeTempoChange(tempoChange, true)
    
    private fun addTempoChange(tempoChange: TempoChange, update: Boolean): Boolean {
        if (tempoChange.beat < 0f) return false
        
        beatMap[tempoChange.beat] = tempoChange
        if (tempoChange.beat == 0f) {
            globalTempo = tempoChange
            globalTempoData = TempoChangeData(tempoChange, 0f)
        }
        if (update) update()
        
        return true
    }
    
    private fun removeTempoChange(tempoChange: TempoChange, update: Boolean): Boolean {
        val removed = beatMap.remove(tempoChange.beat, tempoChange)
        if (update) update()
        
        return removed
    }
    
    fun addTempoChangesBulk(tempoChanges: List<TempoChange>) {
        tempoChanges.forEach { tc ->
            addTempoChange(tc, false)
        }
        update()
    }
    
    fun removeTempoChangesBulk(tempoChanges: List<TempoChange>) {
        tempoChanges.forEach { tc ->
            removeTempoChange(tc, false)
        }
        update()
    }
    
    private fun update() {
        beatMapData.clear()
        secondsMapData.clear()
        
        var previous: TempoChangeData? = null
        beatMap.values.forEach { tc ->
            val prev = previous
            val tcData: TempoChangeData
            if (prev == null) {
                // This should be global tempo. Only add the TempoChangeData to the appropriate maps
                tcData = globalTempoData
            } else {
                tcData = TempoChangeData(tc, beatsToSeconds(prev, tc.beat, false))
            }
            
            beatMapData[tcData.tempoChange.beat] = tcData
            secondsMapData[tcData.secondsStart] = tcData
            previous = tcData
        }
        
        allTempoChanges = beatMap.values.toList()
    }
    
    private fun getTempoChangeDataAtBeat(beat: Float): TempoChangeData {
        return beatMapData.floorEntry(beat)?.value ?: globalTempoData
    }

    private fun getTempoChangeDataAtSeconds(beat: Float): TempoChangeData {
        return secondsMapData.floorEntry(beat)?.value ?: globalTempoData
    }

    fun beatsToSeconds(beats: Float, disregardSwing: Boolean = false): Float {
        if (beats < 0f) return beats
        val tc = getTempoChangeDataAtBeat(beats)
        return beatsToSeconds(tc, beats, disregardSwing)
    }

    fun secondsToBeats(seconds: Float, disregardSwing: Boolean = false): Float {
        if (seconds < 0f) return seconds
        val tc = getTempoChangeDataAtSeconds(seconds)
        return secondsToBeats(tc, seconds, disregardSwing)
    }

    /**
     * Returns average tempo from beat 0 to [untilBeat].
     */
    fun computeAverageTempo(untilBeat: Float): Float {
        if (untilBeat <= 0f) return globalTempo.newTempo
        
        var previousTempo: TempoChange = globalTempo
        var sum = 0f
        for ((index, tc) in allTempoChanges.withIndex()) {
            if (index == 0) continue
            if (tc.beat >= untilBeat) break
            sum += previousTempo.newTempo * (tc.beat - previousTempo.beat)
            previousTempo = tc
        }
        sum += previousTempo.newTempo * (untilBeat - previousTempo.beat)
        
        return sum / (untilBeat)
    }

    private fun beatsToSeconds(tc: TempoChangeData, beats: Float, disregardSwing: Boolean = false): Float {
        if (beats < 0f) return beats
        val baseSeconds = tc.secondsStart
        val beatsToConvert = beats - tc.tempoChange.beat
        val addition = TempoUtils.beatsToSeconds(if (disregardSwing) beatsToConvert else
            SwingUtils.swingToLinear(beatsToConvert, tc.tempoChange.newSwing), tc.tempoChange.newTempo)

        return baseSeconds + addition
    }

    private fun secondsToBeats(tc: TempoChangeData, seconds: Float, disregardSwing: Boolean = false): Float {
        if (seconds < 0f) return seconds
        val baseBeats = tc.tempoChange.beat
        val secondsToConvert = seconds - tc.secondsStart
        val addition = TempoUtils.secondsToBeats(secondsToConvert, tc.tempoChange.newTempo)

        return baseBeats + (if (disregardSwing) addition else SwingUtils.linearToSwing(addition, tc.tempoChange.newSwing))
    }

    fun tempoAtBeat(beat: Float): Float {
        if (beat < 0f) return 60f // 1:1 mapping of seconds and beats at this point
        return getTempoChangeDataAtBeat(beat).tempoChange.newTempo
    }

    fun tempoAtSeconds(seconds: Float): Float {
        if (seconds < 0f) return 60f // 1:1 mapping of seconds and beats at this point
        return getTempoChangeDataAtSeconds(seconds).tempoChange.newTempo
    }

    fun swingAtBeat(beat: Float): Swing {
        if (beat < 0f) return Swing.STRAIGHT
        return getTempoChangeDataAtBeat(beat).tempoChange.newSwing
    }

    fun swingAtSeconds(seconds: Float): Swing {
        if (seconds < 0f) return Swing.STRAIGHT
        return getTempoChangeDataAtSeconds(seconds).tempoChange.newSwing
    }

    private data class TempoChangeData(val tempoChange: TempoChange,
                               val secondsStart: Float)
}