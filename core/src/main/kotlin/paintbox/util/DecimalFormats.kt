package paintbox.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols


object DecimalFormats {
    private val formats: MutableMap<String, DecimalFormat> = mutableMapOf()
    
    fun createDecimalFormatSymbols(): DecimalFormatSymbols = DecimalFormatSymbols()
    
    operator fun get(format: String): DecimalFormat =
            formats.getOrPut(format) { DecimalFormat(format, createDecimalFormatSymbols()) }
    
    fun format(format: String, value: Float): String = get(format).format(value.toDouble())
    fun format(format: String, value: Double): String = get(format).format(value)
    fun format(format: String, value: Int): String = get(format).format(value.toLong())
    fun format(format: String, value: Long): String = get(format).format(value)
}