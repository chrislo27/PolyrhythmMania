package polyrhythmmania.screen.mainmenu

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3
import paintbox.util.MathHelper
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.SimpleRenderedEntity
import polyrhythmmania.world.World
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.WorldRenderer


class EntityCubeHovering(world: World, val withLine: Boolean = false)
    : SimpleRenderedEntity(world) {

    companion object {
        private val tmpVec = Vector3()
    }

    override fun getTextureRegionFromTileset(tileset: Tileset): TextureRegion {
        return if (withLine) tileset.cubeWithLine else tileset.cube
    }
    
    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine) {
        val texReg = getTextureRegionFromTileset(tileset)
        val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(this.position))
        batch.draw(texReg, convertedVec.x,
                convertedVec.y +
                        MathHelper.snapToNearest(((MathHelper.getSineWave(System.currentTimeMillis() + (this.position.x * 333 * 4).toLong() + (this.position.z * 333 * 4).toLong(), 4f)) * 2f - 1f) * 0.2f, 1f / 32f * 0),
                getRenderWidth(), getRenderHeight())
    }
}