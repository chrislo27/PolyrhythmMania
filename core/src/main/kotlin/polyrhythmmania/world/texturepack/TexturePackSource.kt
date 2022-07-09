package polyrhythmmania.world.texturepack

import kotlin.math.absoluteValue


/**
 * Represents the options for a texture pack source.
 */
sealed class TexturePackSource(val jsonId: Int, val isCustom: Boolean) {

    companion object {
        val CUSTOM_RANGE: IntRange = 1..5
        
        val VALUES_NON_CUSTOM: List<TexturePackSource> = listOf(StockGBA, StockHD, StockArcade)
        private val INDEX_MAP: Map<Int, TexturePackSource> = VALUES_NON_CUSTOM.associateBy { it.jsonId }
        val VALUES_WITH_CUSTOM: List<TexturePackSource> = VALUES_NON_CUSTOM + CUSTOM_RANGE.map { Custom(it) }
        
        fun idToSource(jsonId: Int): TexturePackSource? {
            return if (jsonId < 0) {
                val absID = jsonId.absoluteValue
                if (absID in CUSTOM_RANGE) {
                    Custom(absID)
                } else null
            } else INDEX_MAP[jsonId]
        }
    }

    object StockGBA : TexturePackSource(0, false)
    object StockHD : TexturePackSource(1, false)
    object StockArcade : TexturePackSource(2, false)
    
    class Custom(/** 1-indexed. */ val id: Int) : TexturePackSource(-id, true)
}
