package polyrhythmmania.screen.mainmenu

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import paintbox.util.MathHelper
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.EntityAsmWidgetComplete
import polyrhythmmania.world.entity.EntityCube
import polyrhythmmania.world.World
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.render.WorldRenderer


class EntityCubeHovering(world: World, withLine: Boolean = false, withBorder: Boolean = false)
    : EntityCube(world, withLine, withBorder) {
    
    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine, vec: Vector3) {
        val timeMs: Long = System.currentTimeMillis() + (this.position.x * 333 * 4).toLong() + (this.position.z * 333 * 4).toLong()
        vec.y += MathHelper.snapToNearest((MathHelper.getSineWave(timeMs, 4f) * 2f - 1f) * 0.2f, 1f / 32f * 0)
        super.renderSimple(renderer, batch, tileset, engine, vec)
    }
}

class EntityAsmWidgetHovering(world: World)
    : EntityAsmWidgetComplete(world, 100000f) {
    
    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine, vec: Vector3) {
        val timeMs: Long = System.currentTimeMillis() + (this.position.x * 333 * 4).toLong() + (this.position.z * 333 * 4).toLong()
        vec.y += MathHelper.snapToNearest((MathHelper.getSineWave(timeMs, 4f) * 2f - 1f) * 0.2f, 1f / 32f * 0)
        super.renderSimple(renderer, batch, tileset, engine, vec)
    }
}
