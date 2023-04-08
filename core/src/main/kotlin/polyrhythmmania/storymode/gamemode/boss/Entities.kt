package polyrhythmmania.storymode.gamemode.boss

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3
import paintbox.packing.PackedSheet
import paintbox.util.ColorStack
import paintbox.util.MathHelper
import paintbox.util.Vector3Stack
import paintbox.util.wave.WaveUtils
import polyrhythmmania.engine.Engine
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.world.World
import polyrhythmmania.world.entity.HasLightingRender
import polyrhythmmania.world.entity.SimpleRenderedEntity
import polyrhythmmania.world.entity.TemporaryEntity
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset
import kotlin.random.Random


abstract class AbstractEntityBossRobot(
    world: World,
    val bossGameMode: StoryBossGameMode,
    initialPosition: Vector3,
    override val renderSortOffsetX: Float,
    override val renderSortOffsetY: Float,
    override val renderSortOffsetZ: Float,
    val jitterIndex: Int
) : SimpleRenderedEntity(world), TemporaryEntity {
    
    companion object {
        const val JITTER_FPS: Int = 60
    }

    override val renderWidth: Float get() = 105f / 32f
    override val renderHeight: Float get() = 122f / 32f

    var stopBobbing: Boolean = false
    private var lastBobYOffset: Float = 0f
    
    var jitterAmplitude: Float = 0f
    private var timeSinceJitterUpdate: Float = 0f
    private val jitterRandom: Random = Random(13212921L + jitterIndex)
    private val jitterVec: Vector3 = Vector3()

    init {
        this.position.set(initialPosition)
    }
    
    private fun getRandomJitter(): Float = jitterRandom.nextFloat() * jitterAmplitude * (if (jitterRandom.nextBoolean()) -1 else 1) * (1f / 32)

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        renderSimple(renderer, batch, tileset, vec, updateOffsets = true)
    }
    
    protected fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3, updateOffsets: Boolean) {
        val oldPackedColor = batch.packedColor
        val tmpColor = ColorStack.getAndPush()
            .set(1f, 1f, 1f, 1f)
        
        val hurtFlash = bossGameMode.modifierModule.bossHealth.hurtFlash.get()
        tmpColor.lerp(1f, 0.35f, 0.35f, 1f, hurtFlash)

//        batch.setColor(1f, 0f, 0f, 0.25f)
//        batch.fillRect(vec.x, vec.y, renderWidth, renderHeight)

        if (updateOffsets) {
            // Movement bobbing
            if (!stopBobbing) {
                lastBobYOffset = MathHelper.snapToNearest(
                    (WaveUtils.getSineWave(
                        getMovementPeriod(),
                        offsetMs = getMovementTimeOffset()
                    ) * 2f - 1f) * getMovementAmplitude(),
                    if (isMovementPixelSnapped()) (1 / 32f) else 0f
                )
            }

            // Jitter
            if (jitterAmplitude > 0f) {
                if (timeSinceJitterUpdate <= 0f) {
                    timeSinceJitterUpdate = 1f / JITTER_FPS
                    jitterVec.x = getRandomJitter()
                    jitterVec.y = getRandomJitter() * 0.25f
                    jitterVec.z = getRandomJitter()
                } else {
                    timeSinceJitterUpdate -= Gdx.graphics.deltaTime
                }
            }
        }
        
        vec.y += lastBobYOffset
        if (jitterAmplitude > 0f) vec.add(jitterVec)
        
        batch.color = tmpColor
        val textureID = getTextureID()
        if (textureID != null) {
            batch.draw(StoryAssets.get<Texture>(textureID), vec.x, vec.y, renderWidth, renderHeight)
        }
        batch.packedColor = oldPackedColor
        ColorStack.pop()
    }

    override fun shouldApplyRenderCulling(): Boolean = false
    
    protected abstract fun getTextureID(): String?

    protected open fun getMovementTimeOffset(): Long = 0L
    protected open fun getMovementPeriod(): Float = 4f
    protected open fun isMovementPixelSnapped(): Boolean = false
    protected open fun getMovementAmplitude(): Float = 0.05f

}

open class EntityBossRobotStaticTexture(
    world: World,
    bossGameMode: StoryBossGameMode,
    var currentTextureID: String,
    initialPosition: Vector3,
    renderSortOffsetX: Float, renderSortOffsetY: Float, renderSortOffsetZ: Float,
    jitterIndex: Int
) : AbstractEntityBossRobot(
    world,
    bossGameMode,
    initialPosition,
    renderSortOffsetX,
    renderSortOffsetY,
    renderSortOffsetZ,
    jitterIndex
) {

    override fun getTextureID(): String = this.currentTextureID
}

class EntityBossRobotUpside(world: World, bossGameMode: StoryBossGameMode, initialPosition: Vector3) :
    EntityBossRobotStaticTexture(world, bossGameMode, "boss_robot_upside", initialPosition, 0f, 1f, 0f, jitterIndex = 1) {

    override fun getMovementTimeOffset(): Long {
        return -500L
    }
}

class EntityBossRobotMiddle(world: World, bossGameMode: StoryBossGameMode, initialPosition: Vector3) :
    EntityBossRobotStaticTexture(world, bossGameMode, "boss_robot_middle", initialPosition, 1f, 2f, 2f, jitterIndex = 0) {

    override fun getMovementTimeOffset(): Long {
        return 0L
    }
}

class EntityBossRobotFace(world: World, bossGameMode: StoryBossGameMode, initialPosition: Vector3) :
    AbstractEntityBossRobot(world, bossGameMode, initialPosition, 1f, 2f, 2f, jitterIndex = 0), HasLightingRender {
    
    enum class Face(val textureID: String?) {
        NONE(null),
        NEUTRAL("boss_robot_face_neutral"),
        BLUE_SCREEN("boss_robot_face_bsod"),
    }
    
    var currentFace: Face = Face.NEUTRAL

    override fun renderLightingEffect(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset) {
        val tmpVec = Vector3Stack.getAndPush()
        val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(getRenderVec()))
        val packedColor = batch.packedColor

        // Glow test
//        batch.flush()
//        val texture = StoryAssets.get<Texture>(getTextureID())
//        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
//        renderSimple(renderer, batch, tileset, convertedVec)
//        batch.flush()
//        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        
        renderSimple(renderer, batch, tileset, convertedVec, updateOffsets = false)

        Vector3Stack.pop()
        batch.packedColor = packedColor
    }

    override fun getTextureID(): String? {
        return currentFace.textureID
    }

    override fun getMovementTimeOffset(): Long {
        return 0L
    }
}

class EntityBossRobotDownside(world: World, bossGameMode: StoryBossGameMode, initialPosition: Vector3) :
    EntityBossRobotStaticTexture(world, bossGameMode, "boss_robot_downside", initialPosition, 0f, 1f, 3f, jitterIndex = 2) {

    override fun getMovementTimeOffset(): Long {
        return 500L
    }
}

class EntityBossExplosion(
    world: World, val secondsStarted: Float,
    initialPosition: Vector3
) : SimpleRenderedEntity(world), HasLightingRender, TemporaryEntity {

    companion object {
        const val NUM_FRAMES: Int = 17
        const val EXPLOSION_DURATION: Float = 120 / 60f
    }
    
    var duration: Float = EXPLOSION_DURATION

    override val renderWidth: Float get() = 71f / 32f
    override val renderHeight: Float get() = 100f / 32f

    override val renderSortOffsetX: Float get() = 0f
    override val renderSortOffsetY: Float get() = 1f
    override val renderSortOffsetZ: Float get() = 3f

    private var percentageLife: Float = 0f
    
    init {
        this.position.set(initialPosition)
    }

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        if (isKilled) return
        
        val oldPackedColor = batch.packedColor
        val tmpColor = ColorStack.getAndPush()
            .set(1f, 1f, 1f, 1f)

        batch.color = tmpColor
        batch.draw(getTextureRegion(), vec.x, vec.y, renderWidth, renderHeight)
        batch.packedColor = oldPackedColor
        ColorStack.pop()
    }
    
    private fun getCurrentIndex(): Int = (percentageLife * NUM_FRAMES).toInt().coerceIn(0, NUM_FRAMES - 1)
    
    private fun getTextureRegion(): TextureRegion {
        return StoryAssets.get<PackedSheet>("boss_explosion").getIndexedRegions("explosion").getValue(getCurrentIndex())
    }

    override fun renderLightingEffect(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset) {
        val tmpVec = Vector3Stack.getAndPush()
        val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(getRenderVec()))
        val packedColor = batch.packedColor

        renderSimple(renderer, batch, tileset, convertedVec)

        Vector3Stack.pop()
        batch.packedColor = packedColor
    }


    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)

        if (isKilled) return

        val secondsElapsed = engine.seconds - secondsStarted
        val percentage = (secondsElapsed / duration).coerceIn(0f, 1f)
        this.percentageLife = percentage
        if (percentage >= 1f) {
            kill()
        }
    }
}