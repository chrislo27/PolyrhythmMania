package polyrhythmmania.engine.timesignature


class TimeSignature(val beat: Float, beatsPerMeasure: Int, beatUnit: Int = DEFAULT_NOTE_UNIT) {

    companion object {
        val LOWER_BEATS_PER_MEASURE = 1
        val UPPER_BEATS_PER_MEASURE = 64
//        val NOTE_UNITS = listOf(2, 4, 8, 16).sorted()
        val DEFAULT_NOTE_UNIT = 4
    }

    val beatsPerMeasure: Int = beatsPerMeasure.coerceAtLeast(1)
    val beatUnit: Int = beatUnit.coerceAtLeast(1)
    val noteFraction: Float get() = 4f / beatUnit

    var measure: Int = 0

    val lowerText: String = this.beatUnit.toString()
    val upperText: String = "${this.beatsPerMeasure}"
    
    fun copy(beat: Float = this.beat, beatsPerMeasure: Int = this.beatsPerMeasure, beatUnit: Int = this.beatUnit): TimeSignature {
        return TimeSignature(beat, beatsPerMeasure, beatUnit)
    }
}