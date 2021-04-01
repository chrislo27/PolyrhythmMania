package polyrhythmmania.engine.tempo

import java.util.*


class TempoMap(startingGlobalTempo: Float) {

    /**
     * The global tempo is always at beat 0, seconds 0.
     */
    private var globalTempo: TempoChange = TempoChange(0f, startingGlobalTempo)
    private var globalTempoData: TempoChangeData = TempoChangeData(globalTempo, 0f)

    private val beatMap: NavigableMap<Float, TempoChange> = TreeMap()
    private val beatMapData: NavigableMap<Float, TempoChangeData> = TreeMap()
    private val secondsMapData: NavigableMap<Float, TempoChangeData> = TreeMap()

    init {
        addTempoChange(globalTempo)
    }
    
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
    }
    
    private fun getTempoChangeDataAtBeat(beat: Float): TempoChangeData {
        return beatMapData.floorEntry(beat)?.value ?: globalTempoData
    }

    private fun getTempoChangeDataAtSeconds(beat: Float): TempoChangeData {
        return secondsMapData.floorEntry(beat)?.value ?: globalTempoData
    }

    fun beatsToSeconds(beats: Float, disregardSwing: Boolean = false): Float {
        val tc = getTempoChangeDataAtBeat(beats)
        return beatsToSeconds(tc, beats, disregardSwing)
    }

    fun secondsToBeats(seconds: Float, disregardSwing: Boolean = false): Float {
        val tc = getTempoChangeDataAtSeconds(seconds)
        return secondsToBeats(tc, seconds, disregardSwing)
    }

    private fun beatsToSeconds(tc: TempoChangeData, beats: Float, disregardSwing: Boolean = false): Float {
        val baseSeconds = tc.secondsStart
        val beatsToConvert = beats - tc.tempoChange.beat
        val addition = TempoUtils.beatsToSeconds(if (disregardSwing) beatsToConvert else
            SwingUtils.swingToLinear(beatsToConvert, tc.tempoChange.newSwing), tc.tempoChange.newTempo)

        return baseSeconds + addition
    }

    private fun secondsToBeats(tc: TempoChangeData, seconds: Float, disregardSwing: Boolean = false): Float {
        val baseBeats = tc.tempoChange.beat
        val secondsToConvert = seconds - tc.secondsStart
        val addition = TempoUtils.secondsToBeats(secondsToConvert, tc.tempoChange.newTempo)

        return baseBeats + (if (disregardSwing) addition else SwingUtils.linearToSwing(addition, tc.tempoChange.newSwing))
    }

    fun tempoAtBeat(beat: Float): Float {
        return getTempoChangeDataAtBeat(beat).tempoChange.newTempo
    }

    fun tempoAtSeconds(seconds: Float): Float {
        return getTempoChangeDataAtSeconds(seconds).tempoChange.newTempo
    }

    data class TempoChangeData(val tempoChange: TempoChange,
                               val secondsStart: Float)
}