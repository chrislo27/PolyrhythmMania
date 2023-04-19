package polyrhythmmania.screen.mainmenu

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import paintbox.registry.AssetRegistry
import paintbox.util.ColorStack
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

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        val oldPackedColor = batch.packedColor
        val tmpColor = ColorStack.getAndPush()
            .set(1f, 1f, 1f, 1f)
        
        vec.y += 2f / 32f

        batch.color = tmpColor
        batch.draw(AssetRegistry.get<Texture>("mainmenu_bg_storymode_inbox_entity"), vec.x, vec.y, renderWidth, renderHeight)
        batch.packedColor = oldPackedColor
        ColorStack.pop()
    }
}

class EntityStoryModeDesktopTube(world: World) : SimpleRenderedEntity(world) {
        
    override val renderWidth: Float get() = 114f / 32f
    override val renderHeight: Float get() = 82f / 32f

    override val renderSortOffsetX: Float get() = 0f
    override val renderSortOffsetY: Float get() = 1f
    override val renderSortOffsetZ: Float get() = 3f

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        val oldPackedColor = batch.packedColor
        val tmpColor = ColorStack.getAndPush()
            .set(1f, 1f, 1f, 1f)

        batch.color = tmpColor
        batch.draw(AssetRegistry.get<Texture>("mainmenu_bg_storymode_tube"), vec.x, vec.y, renderWidth, renderHeight)
        batch.packedColor = oldPackedColor
        ColorStack.pop()
    }
}

class EntityStoryModeDesktopPistonHovering(world: World)
    : SimpleRenderedEntity(world) {
        
    override val renderWidth: Float get() = 1f
    override val renderHeight: Float get() = 1f

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        val oldPackedColor = batch.packedColor
        val tmpColor = ColorStack.getAndPush()
            .set(1f, 1f, 1f, 1f)
        
        val timeMs: Long = System.currentTimeMillis() + (this.position.x * 333 * 4).toLong() + (this.position.z * 333 * 4).toLong()
        vec.y += MathHelper.snapToNearest((WaveUtils.getSineWave(4f, timeMs) * 2f - 1f) * 0.2f, 1f / 32f * 0)

        batch.color = tmpColor
        batch.draw(AssetRegistry.get<Texture>("mainmenu_bg_storymode_piston"), vec.x, vec.y, renderWidth, renderHeight)
        batch.packedColor = oldPackedColor
        ColorStack.pop()
    }
}
