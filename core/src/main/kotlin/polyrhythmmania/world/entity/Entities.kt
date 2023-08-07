package polyrhythmmania.world.entity

import polyrhythmmania.world.World
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TintedRegion


class EntityPlatform(world: World, val withLine: Boolean = false) : SpriteEntity(world) {

    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return if (withLine) tileset.platformWithLine else tileset.platform
    }
}

open class EntityCube(
    world: World,
    val withLine: Boolean = false,
    val withBorder: Boolean = false,
) : SpriteEntity(world) {

    override val numLayers: Int = 6

    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        return when (index) {
            0 -> if (withBorder) tileset.cubeBorderPlatform else tileset.cubeBorder
            1 -> tileset.cubeBorderZ
            2 -> tileset.cubeFaceX
            3 -> tileset.cubeFaceY
            4 -> tileset.cubeFaceZ
            5 -> if (withLine) tileset.redLine else null
            else -> null
        }
    }
}

class EntitySign(world: World, val type: Type) : SpriteEntity(world) {

    companion object {

        // Should have an X, Z offset of 12/32, 8/32
        private val apparentWorldXOffset: Float = 12 / 32f
        private val apparentWorldZOffset: Float = 8 / 32f
    }

    enum class Type {
        SYMBOL_A,
        SYMBOL_DPAD,
        JP_BO,
        JP_TA,
        JP_N,
    }

    override val numLayers: Int = 2
    override val renderWidth: Float = 0.5f
    override val renderHeight: Float = 0.5f

    override val pxOffsetX: Float = (apparentWorldXOffset + apparentWorldZOffset) / 2f
    override val pxOffsetY: Float = (apparentWorldXOffset - apparentWorldZOffset) / 4f

    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return when (type) {
            Type.SYMBOL_A -> if (index == 0) tileset.signAShadow else tileset.signA
            Type.SYMBOL_DPAD -> if (index == 0) tileset.signDpadShadow else tileset.signDpad
            Type.JP_BO -> if (index == 0) tileset.signBoShadow else tileset.signBo
            Type.JP_TA -> if (index == 0) tileset.signTaShadow else tileset.signTa
            Type.JP_N -> if (index == 0) tileset.signNShadow else tileset.signN
        }
    }
}
