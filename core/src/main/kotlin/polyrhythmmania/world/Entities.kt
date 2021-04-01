package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3
import io.github.chrislo27.paintbox.util.MathHelper
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.WorldRenderer


class EntityPlatform(world: World, val withLine: Boolean = false) : SimpleRenderedEntity(world) {
    override fun getTextureRegionFromTileset(tileset: Tileset): TextureRegion {
        return if (withLine) tileset.platformWithLine else tileset.platform
    }
}

class EntityCube(world: World, val withLine: Boolean = false) : SimpleRenderedEntity(world) {
    override fun getTextureRegionFromTileset(tileset: Tileset): TextureRegion {
        return if (withLine) tileset.cubeWithLine else tileset.cube
    }
}


class EntityRod(world: World) : Entity(world) {

    companion object {
        private val tmpVec = Vector3()
    }
    
    var isInAir: Boolean = false

    override fun getRenderWidth(): Float = 0.75f
    override fun getRenderHeight(): Float = 0.5f

    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset) {
        val convertedVec = renderer.convertWorldToScreen(tmpVec.set(this.position))
        
        // FIXME use world time/beats instead of System.currentTimeMillis
        val animationAlpha = MathHelper.getSawtoothWave(System.currentTimeMillis(), 0.2f)
        val texReg: TextureRegion = if (!isInAir) {
            tileset.rodGroundAnimations[(animationAlpha * tileset.rodGroundFrames).toInt().coerceIn(0, tileset.rodGroundFrames - 1)]
        } else {
            tileset.rodAerialAnimations[(animationAlpha * tileset.rodAerialFrames).toInt().coerceIn(0, tileset.rodAerialFrames - 1)]
        }
        
        batch.draw(texReg, convertedVec.x - (1 / 32f), convertedVec.y, getRenderWidth(), getRenderHeight())
    }

}