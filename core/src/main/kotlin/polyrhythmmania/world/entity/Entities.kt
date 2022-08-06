package polyrhythmmania.world.entity

import polyrhythmmania.world.World
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TintedRegion
import kotlin.math.roundToInt


class EntityPlatform(world: World, val withLine: Boolean = false) : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return if (withLine) tileset.platformWithLine else tileset.platform
    }
}

open class EntityCube(world: World, val withLine: Boolean = false, val withBorder: Boolean = false)
    : SpriteEntity(world) {
    
    companion object {
        fun createCubemapIndex(x: Int, y: Int, z: Int): Long {
            val overflow = x !in Short.MIN_VALUE..Short.MAX_VALUE || y !in Short.MIN_VALUE..Short.MAX_VALUE || z !in Short.MIN_VALUE..Short.MAX_VALUE

            return 0L or (if (overflow) (1L shl 63) else 0L).toLong() or (
                    x.toShort().toLong() or (y.toShort().toLong() shl 16) or (z.toShort().toLong() shl 32)
                    )
        }
    }

    override val numLayers: Int = 6

    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        // Uncomment if cube map culling is to be used
//        val cubeOccludesX = world.cubeMap[createCubemapIndex(this.position.x.roundToInt() - 1, this.position.y.roundToInt(), this.position.z.roundToInt())] != null
//        val cubeOccludesY = world.cubeMap[createCubemapIndex(this.position.x.roundToInt(), this.position.y.roundToInt() + 1, this.position.z.roundToInt())] != null
//        val cubeOccludesZ = world.cubeMap[createCubemapIndex(this.position.x.roundToInt(), this.position.y.roundToInt(), this.position.z.roundToInt() + 1)] != null
//
//        return when (index) { // Update when with non-culling
//            0 -> tileset.cubeBorder
//            1 -> if (cubeOccludesZ) null else tileset.cubeBorderZ
//            2 -> if (cubeOccludesX) null else tileset.cubeFaceX
//            3 -> if (cubeOccludesY) null else tileset.cubeFaceY
//            4 -> if (cubeOccludesZ) null else tileset.cubeFaceZ
//            5 -> if (withLine) tileset.redLine else null
//            6 -> if (withBorder) tileset.platformBorder else null
//            else -> null
//        }
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
    
    fun getCubemapIndex(): Long {
        return createCubemapIndex(this.position.x.roundToInt(), this.position.y.roundToInt(), this.position.z.roundToInt())
    }
}

class EntitySign(world: World, val type: Type) : SpriteEntity(world) {
    enum class Type {
        A, DPAD, BO, TA, N;
    }

    override val numLayers: Int = 2
    override val renderWidth: Float = 0.5f
    override val renderHeight: Float = 0.5f
    
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return when (type) {
            Type.A -> if (index == 0) tileset.signAShadow else tileset.signA
            Type.DPAD -> if (index == 0) tileset.signDpadShadow else tileset.signDpad
            Type.BO -> if (index == 0) tileset.signBoShadow else tileset.signBo
            Type.TA -> if (index == 0) tileset.signTaShadow else tileset.signTa
            Type.N -> if (index == 0) tileset.signNShadow else tileset.signN
        }
    }
}
