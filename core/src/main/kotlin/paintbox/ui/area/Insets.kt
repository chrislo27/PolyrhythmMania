package paintbox.ui.area

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min


data class Insets(val top: Float, val bottom: Float, val left: Float, val right: Float) {
    companion object {
        val ZERO: Insets = Insets(0f, 0f, 0f, 0f)

        private val intCache: Array<Insets> by lazy {
            Array(1 + 20) { i ->
                if (i == 0) {
                    ZERO
                } else {
                    val f = i.toFloat()
                    Insets(f, f, f, f)
                }
            }
        }

        operator fun invoke(all: Float): Insets {
            val toInt = all.toInt()
            if (floor(all) == all && toInt in intCache.indices) {
                return intCache[toInt]
            }
            return Insets(all, all, all, all)
        }
    }
    
    fun maximize(other: Insets): Insets =
            Insets(max(this.top, other.top), max(this.bottom, other.bottom), max(this.left, other.left), max(this.right, other.right))
    fun minimize(other: Insets): Insets = 
            Insets(min(this.top, other.top), min(this.bottom, other.bottom), min(this.left, other.left), min(this.right, other.right))
}
