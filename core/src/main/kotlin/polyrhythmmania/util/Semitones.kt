package polyrhythmmania.util

import kotlin.math.pow


object Semitones {

    const val SEMITONES_IN_OCTAVE: Int = 12
    private const val SEMITONE_VALUE: Float = 1f / SEMITONES_IN_OCTAVE

    fun getALPitch(semitone: Int): Float {
        return 2.0.pow((semitone * SEMITONE_VALUE).toDouble()).toFloat()
    }

    fun getALPitch(semitone: Float): Float {
        return 2.0.pow((semitone * SEMITONE_VALUE).toDouble()).toFloat()
    }
}
