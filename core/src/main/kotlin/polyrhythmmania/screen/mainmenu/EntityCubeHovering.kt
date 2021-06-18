package polyrhythmmania.screen.mainmenu

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.util.MathHelper
import paintbox.util.Vector3Stack
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.SimpleRenderedEntity
import polyrhythmmania.world.World
import polyrhythmmania.world.render.OldTileset
import polyrhythmmania.world.render.WorldRenderer


class EntityCubeHovering(world: World, val withLine: Boolean = false)
    : SimpleRenderedEntity(world) {
    override fun getTextureRegionFromTileset(tileset: OldTileset): TextureRegion {
        return if (withLine) tileset.cubeWithLine else tileset.cube
    }
    
    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: OldTileset, engine: Engine) {
        val texReg = getTextureRegionFromTileset(tileset)
        val tmpVec = Vector3Stack.getAndPush()
        val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(this.position))
        batch.draw(texReg, convertedVec.x,
                convertedVec.y +
                        MathHelper.snapToNearest(((MathHelper.getSineWave(System.currentTimeMillis() + (this.position.x * 333 * 4).toLong() + (this.position.z * 333 * 4).toLong(), 4f)) * 2f - 1f) * 0.2f, 1f / 32f * 0),
                getRenderWidth(), getRenderHeight())
        Vector3Stack.pop()
    }
}