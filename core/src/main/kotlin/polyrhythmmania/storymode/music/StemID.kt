package polyrhythmmania.storymode.music

import java.util.*


data class StemID(val baseID: String, val variants: Int = 0) {
    
    companion object {

        const val NO_VARIANT: Int = 0
    }
    
    val hasNoVariants: Boolean = variants <= 0

    fun getID(variantNum: Int): String {
        if (variantNum == NO_VARIANT) return baseID
        if (variantNum !in 1..variants) throw IllegalArgumentException("variantNum not in range of [1, ${variants}], got $variantNum")

        return "${baseID}_var${variantNum}"
    }
    
    fun getRandomID(random: Random): String = if (hasNoVariants) getID(NO_VARIANT) else getID(1 + random.nextInt(variants))

    fun getAllIDs(): List<String> =
        if (hasNoVariants) listOf(getID(NO_VARIANT)) else (1..variants).map { variant ->
            getID(variant)
        }

    fun getAllIDsWithVariant(): List<Pair<Int, String>> =
        if (hasNoVariants) listOf(NO_VARIANT to getID(NO_VARIANT)) else (1..variants).map { variant ->
            variant to getID(variant)
        }
}
