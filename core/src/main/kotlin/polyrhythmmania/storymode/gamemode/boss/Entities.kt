package polyrhythmmania.storymode.gamemode.boss

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import paintbox.util.ColorStack
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.world.World
import polyrhythmmania.world.entity.SimpleRenderedEntity
import polyrhythmmania.world.entity.TemporaryEntity
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset


abstract class AbstractEntityBossRobot(
    world: World,
    val textureID: String,
    initialPosition: Vector3, val sortOffsetPx: Float, val sortOffsetPy: Float,
) : SimpleRenderedEntity(world), TemporaryEntity {

    override val renderWidth: Float get() = 105f / 32f
    override val renderHeight: Float get() = 122f / 32f

    override val renderSortOffsetX: Float
        get() = sortOffsetPx / 32f
    override val renderSortOffsetY: Float
        get() = sortOffsetPx / 32f + sortOffsetPy / 32f
    override val renderSortOffsetZ: Float
        get() = sortOffsetPx / 32f * 2f

    init {
        this.position.set(initialPosition)
    }

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        val oldPackedColor = batch.packedColor
        val tmpColor = ColorStack.getAndPush()
            .set(1f, 1f, 1f, 1f)

        batch.color = tmpColor
        batch.draw(StoryAssets.get<Texture>(textureID), vec.x, vec.y, renderWidth, renderHeight)
        batch.packedColor = oldPackedColor
        ColorStack.pop()
    }

    override fun shouldApplyRenderCulling(): Boolean = false

}

class EntityBossRobotUpside(world: World, initialPosition: Vector3) :
    AbstractEntityBossRobot(world, "boss_robot_upside", initialPosition, 0f, 21f)

class EntityBossRobotMiddle(world: World, initialPosition: Vector3) :
    AbstractEntityBossRobot(world, "boss_robot_middle", initialPosition, 32f, 32f)

class EntityBossRobotDownside(world: World, initialPosition: Vector3) :
    AbstractEntityBossRobot(world, "boss_robot_downside", initialPosition, 48f, 0f)

