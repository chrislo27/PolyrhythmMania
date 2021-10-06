package polyrhythmmania.util

import java.text.DecimalFormatSymbols
import kotlin.math.abs


object TimeUtils {
    
    private val decimalFormatSymbols: DecimalFormatSymbols = DecimalFormats.createDecimalFormatSymbols()
    
    fun convertMsToTimestamp(ms: Float, noMs: Boolean = false): String {
        val msAbs = abs(ms)
        val min = (msAbs / 60_000).toInt()
        val sec = (msAbs / 1000 % 60).toInt()
        val msPart = (msAbs % 1000).toInt()

        return "${if (ms < 0) "${decimalFormatSymbols.minusSign}" else ""}${DecimalFormats.format("00", min)}:${DecimalFormats.format("00", sec)}${if (noMs) "" else ("${decimalFormatSymbols.decimalSeparator}${DecimalFormats.format("000", msPart)}")}"
    }
}