package polyrhythmmania.util

import kotlin.math.abs


object TimeUtils {
    
    fun convertMsToTimestamp(ms: Float): String {
        val msAbs = abs(ms)
        val min = (msAbs / 60_000).toInt()
        val sec = (msAbs / 1000 % 60).toInt()
        val msPart = (msAbs % 1000).toInt()

        return "${if (ms < 0) "-" else ""}${DecimalFormats.format("00", min)}:${DecimalFormats.format("00", sec)}.${DecimalFormats.format("000", msPart)}"
    }
}