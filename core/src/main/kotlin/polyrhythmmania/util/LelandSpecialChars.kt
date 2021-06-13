package polyrhythmmania.util

import kotlin.math.abs

object LelandSpecialChars {
    const val TIMESIG_0 = "\uE080"
    const val TIMESIG_1 = "\uE081"
    const val TIMESIG_2 = "\uE082"
    const val TIMESIG_3 = "\uE083"
    const val TIMESIG_4 = "\uE084"
    const val TIMESIG_5 = "\uE085"
    const val TIMESIG_6 = "\uE086"
    const val TIMESIG_7 = "\uE087"
    const val TIMESIG_8 = "\uE088"
    const val TIMESIG_9 = "\uE089"
    const val TIMESIG_COMMON = "\uE08A"
    const val TIMESIG_CUT = "\uE08B"
    
    private val digitsMapping: Array<String> = arrayOf(TIMESIG_0, TIMESIG_1, TIMESIG_2, TIMESIG_3, TIMESIG_4,
            TIMESIG_5, TIMESIG_6, TIMESIG_7, TIMESIG_8, TIMESIG_9)
    
    fun intToString(value: Int): String {
        return abs(value).toString().map { if (it in '0'..'9') digitsMapping[it - '0'] else it }.joinToString()
    }
}