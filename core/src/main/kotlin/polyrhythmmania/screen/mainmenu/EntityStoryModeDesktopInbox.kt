package polyrhythmmania.screen.mainmenu

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector3
import paintbox.registry.AssetRegistry
import paintbox.util.MathHelper
import paintbox.util.wave.WaveUtils
import polyrhythmmania.world.World
import polyrhythmmania.world.entity.SimpleRenderedEntity
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset


class EntityStoryModeDesktopInbox(world: World) : SimpleRenderedEntity(world) {
        
    override val renderWidth: Float get() = 96f / 32f
    override val renderHeight: Float get() = 48f / 32f

    override val renderSortOffsetX: Float get() = 0f
    override val renderSortOffsetY: Float get() = 1f
    override val renderSortOffsetZ: Float get() = 3f

    override fun renderSimple(renderer: WorldRenderer, batch: Batch, tileset: Tileset, vec: Vector3) {
        val oldPackedColor = batch.packedColor
        
        vec.y += 2f / 32f

        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(AssetRegistry.get<Texture>("mainmenu_bg_storymode_inbox_entity"), vec.x, vec.y, renderWidth, renderHeight)
        batch.packedColor = oldPackedColor
    }
}

class EntityStoryModeDesktopTube(world: World) : SimpleRenderedEntity(world) {
        
    override val renderWidth: Float get() = 114f / 32f
    override val renderHeight: Float get() = 82f / 32f

    override val renderSortOffsetX: Float get() = 0f
    override val renderSortOffsetY: Float get() = 1f
    override val renderSortOffsetZ: Float get() = 3f

    override fun renderSimple(renderer: WorldRenderer, batch: Batch, tileset: Tileset, vec: Vector3) {
        val oldPackedColor = batch.packedColor

        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(AssetRegistry.get<Texture>("mainmenu_bg_storymode_tube"), vec.x, vec.y, renderWidth, renderHeight)
        batch.packedColor = oldPackedColor
    }
}

class EntityStoryModeDesktopPistonHovering(world: World)
    : SimpleRenderedEntity(world) {
        
    override val renderWidth: Float get() = 1f
    override val renderHeight: Float get() = 1f

    override fun renderSimple(renderer: WorldRenderer, batch: Batch, tileset: Tileset, vec: Vector3) {
        val oldPackedColor = batch.packedColor
        
        val timeMs: Long = System.currentTimeMillis() + (this.position.x * 333 * 4).toLong() + (this.position.z * 333 * 4).toLong()
        vec.y += MathHelper.snapToNearest((WaveUtils.getSineWave(4f, timeMs) * 2f - 1f) * 0.2f, 1f / 32f * 0)

        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(AssetRegistry.get<Texture>("mainmenu_bg_storymode_piston"), vec.x, vec.y, renderWidth, renderHeight)
        batch.packedColor = oldPackedColor
    }
}
