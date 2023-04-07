package polyrhythmmania.storymode.gamemode.boss

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import paintbox.util.ColorStack
import paintbox.util.MathHelper
import paintbox.util.wave.WaveUtils
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.world.World
import polyrhythmmania.world.entity.SimpleRenderedEntity
import polyrhythmmania.world.entity.TemporaryEntity
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset


abstract class AbstractEntityBossRobot(
    world: World,
    val bossGameMode: StoryBossGameMode,
    val textureID: String,
    initialPosition: Vector3,
    override val renderSortOffsetX: Float,
    override val renderSortOffsetY: Float,
    override val renderSortOffsetZ: Float,
) : SimpleRenderedEntity(world), TemporaryEntity {

    override val renderWidth: Float get() = 105f / 32f
    override val renderHeight: Float get() = 122f / 32f


    init {
        this.position.set(initialPosition)
    }

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        val oldPackedColor = batch.packedColor
        val tmpColor = ColorStack.getAndPush()
            .set(1f, 1f, 1f, 1f)
        
        val hurtFlash = bossGameMode.modifierModule.bossHealth.hurtFlash.get()
        tmpColor.lerp(1f, 0.35f, 0.35f, 1f, hurtFlash)

//        batch.setColor(1f, 0f, 0f, 0.25f)
//        batch.fillRect(vec.x, vec.y, renderWidth, renderHeight)

        // Movement bobbing
        vec.y += MathHelper.snapToNearest(
            (WaveUtils.getSineWave(
                getMovementPeriod(),
                offsetMs = getMovementTimeOffset()
            ) * 2f - 1f) * getMovementAmplitude(),
            if (isMovementPixelSnapped()) (1 / 32f) else 0f
        )
        
        batch.color = tmpColor
        batch.draw(StoryAssets.get<Texture>(textureID), vec.x, vec.y, renderWidth, renderHeight)
        batch.packedColor = oldPackedColor
        ColorStack.pop()
    }

    override fun shouldApplyRenderCulling(): Boolean = false

    protected open fun getMovementTimeOffset(): Long = 0L
    protected open fun getMovementPeriod(): Float = 4f
    protected open fun isMovementPixelSnapped(): Boolean = false
    protected open fun getMovementAmplitude(): Float = 0.05f
    
}

class EntityBossRobotUpside(world: World, bossGameMode: StoryBossGameMode,initialPosition: Vector3) :
    AbstractEntityBossRobot(world, bossGameMode, "boss_robot_upside", initialPosition, 0f, 1f, 0f) {

    override fun getMovementTimeOffset(): Long {
        return -500L
    }
}

class EntityBossRobotMiddle(world: World, bossGameMode: StoryBossGameMode,initialPosition: Vector3) :
    AbstractEntityBossRobot(world, bossGameMode, "boss_robot_middle", initialPosition, 1f, 2f, 2f) {

    override fun getMovementTimeOffset(): Long {
        return 0L
    }
}

class EntityBossRobotDownside(world: World, bossGameMode: StoryBossGameMode,initialPosition: Vector3) :
    AbstractEntityBossRobot(world, bossGameMode, "boss_robot_downside", initialPosition, 0f, 1f, 3f) {

    override fun getMovementTimeOffset(): Long {
        return 500L
    }
}
