package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.WorldRenderer


class EntityRowBlock(world: World, val baseY: Float) : SimpleRenderedEntity(world) {
    
    enum class Type(val renderHeight: Float) {
        PLATFORM(1f),
        PISTON_A(1.25f),
        PISTON_DPAD(1.25f)
    }
    
    enum class PistonState {
        FULLY_EXTENDED,
        PARTIALLY_EXTENDED,
        RETRACTED
    }
    
    var type: Type = Type.PLATFORM
    var pistonState: PistonState = PistonState.RETRACTED
    var visible: Boolean = true
    
    init {
        this.position.y = baseY
    }
    
    override fun getRenderWidth(): Float = 1f
    override fun getRenderHeight(): Float = this.type.renderHeight
    override fun getTextureRegionFromTileset(tileset: Tileset): TextureRegion {
        return when (type) {
            Type.PLATFORM -> tileset.platform
            Type.PISTON_A -> when (pistonState) {
                PistonState.FULLY_EXTENDED -> tileset.padAExtended
                PistonState.PARTIALLY_EXTENDED -> tileset.padAPartial
                PistonState.RETRACTED -> tileset.padARetracted
            }
            Type.PISTON_DPAD -> when (pistonState) {
                PistonState.FULLY_EXTENDED -> tileset.padDExtended
                PistonState.PARTIALLY_EXTENDED -> tileset.padDPartial
                PistonState.RETRACTED -> tileset.padDRetracted
            }
        }
    }

    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset) {
        if (visible) {
            super.render(renderer, batch, tileset)
        }
    }
}