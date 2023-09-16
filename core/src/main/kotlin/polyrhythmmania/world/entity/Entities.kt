package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.world.World
import polyrhythmmania.world.render.WorldRenderer
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

class EntitySign(world: World, val type: Type, val renderCriteria: RenderCriteria) : SpriteEntity(world) {

    companion object {
        
        private const val SHADOW_REGION_INDEX: Int = 0

        // Should have an X, Z offset of 12/32, 8/32
        private const val APPARENT_WORLD_OFFSET_X_JP: Float = 12 / 32f
        private const val APPARENT_WORLD_OFFSET_Z_JP: Float = 8 / 32f
        
        // EN letters are 32x32 aligned to a platform
        private const val APPARENT_WORLD_OFFSET_X_EN: Float = 0f 
        private const val APPARENT_WORLD_OFFSET_Z_EN: Float = 0f 
    }
    
    enum class RenderCriteria(val check: (World) -> Boolean) {
        ALWAYS({ true }),
        ONLY_JP({ w -> !w.shouldShowEnglishSigns() }),
        ONLY_EN({ w -> w.shouldShowEnglishSigns() }),
    }

    enum class Type(val renderWidth: Float, val renderHeight: Float, val useEnOffset: Boolean) {
        SYMBOL_A(0.5f, 0.5f, false),
        SYMBOL_DPAD(0.5f, 0.5f, false),
        
        JP_BO(0.5f, 0.5f, false),
        JP_TA(0.5f, 0.5f, false),
        JP_N(0.5f, 0.5f, false),
        
        EN_P(1f, 1f, true),
        EN_R(1f, 1f, true),
        EN_E(1f, 1f, true),
        EN_S(1f, 1f, true),
    }

    override val numLayers: Int get() = 2
    override val renderWidth: Float get() = type.renderWidth
    override val renderHeight: Float get() = type.renderHeight

    override val pxOffsetX: Float = (getApparentWorldXOffset() + getApparentWorldZOffset()) / 2f
    override val pxOffsetY: Float = (getApparentWorldXOffset() - getApparentWorldZOffset()) / 4f
    
    private fun getApparentWorldXOffset(): Float =
        if (type.useEnOffset) APPARENT_WORLD_OFFSET_X_EN else APPARENT_WORLD_OFFSET_X_JP
    
    private fun getApparentWorldZOffset(): Float =
        if (type.useEnOffset) APPARENT_WORLD_OFFSET_Z_EN else APPARENT_WORLD_OFFSET_Z_JP

    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return when (type) {
            Type.SYMBOL_A -> if (index == SHADOW_REGION_INDEX) tileset.signAShadow else tileset.signA
            Type.SYMBOL_DPAD -> if (index == SHADOW_REGION_INDEX) tileset.signDpadShadow else tileset.signDpad
            Type.JP_BO -> if (index == SHADOW_REGION_INDEX) tileset.signBoShadow else tileset.signBo
            Type.JP_TA -> if (index == SHADOW_REGION_INDEX) tileset.signTaShadow else tileset.signTa
            Type.JP_N -> if (index == SHADOW_REGION_INDEX) tileset.signNShadow else tileset.signN
            Type.EN_P -> if (index == SHADOW_REGION_INDEX) tileset.signPShadow else tileset.signP
            Type.EN_R -> if (index == SHADOW_REGION_INDEX) tileset.signRShadow else tileset.signR
            Type.EN_E -> if (index == SHADOW_REGION_INDEX) tileset.signEShadow else tileset.signE
            Type.EN_S -> if (index == SHADOW_REGION_INDEX) tileset.signSShadow else tileset.signS
        }
    }

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        if (renderCriteria.check(world)) {
            super.renderSimple(renderer, batch, tileset, vec)
        }
    }
}
