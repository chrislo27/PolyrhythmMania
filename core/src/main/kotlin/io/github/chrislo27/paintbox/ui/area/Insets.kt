package io.github.chrislo27.paintbox.ui.area

import kotlin.math.floor


data class Insets(val top: Float, val bottom: Float, val left: Float, val right: Float) {
    companion object {
        val ZERO: Insets = Insets(0f, 0f, 0f, 0f)

        private val intCache: Array<Insets> by lazy {
            Array(11) { i ->
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
}
