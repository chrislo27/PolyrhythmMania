package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import paintbox.registry.AssetRegistry
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.InputResult
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.util.WaveUtils
import polyrhythmmania.world.EntityRowBlock
import polyrhythmmania.world.Row
import polyrhythmmania.world.World
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.TintedRegion
import polyrhythmmania.world.render.WorldRenderer
import kotlin.math.absoluteValue
import kotlin.math.floor

open class EntityRodDecor(world: World, isInAir: Boolean = false) : SimpleRenderedEntity(world) {
    
    val xUnitsPerBeat: Float = 2f
    open val isInAir: Boolean = isInAir

    override val renderWidth: Float = 0.75f
    override val renderHeight: Float = 0.5f
    
    constructor(world: World) : this(world, false)
    
    protected open fun getAnimationAlpha(): Float {
        return 0f
    }
    
    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine, vec: Vector3) {
        val animationAlpha = getAnimationAlpha().coerceIn(0f, 1f)

        val renderW = renderWidth
        val renderH = renderHeight
        val offsetX = -(1 / 32f) * 0
        val offsetY = 1f / 32f * 0
        val regionBorder: TintedRegion = if (!isInAir) {
            tileset.rodGroundBorderAnimations[(animationAlpha * Tileset.rodFrameCount).toInt().coerceIn(0, Tileset.rodFrameCount - 1)]
        } else {
            tileset.rodAerialBorderAnimations[(animationAlpha * Tileset.rodFrameCount).toInt().coerceIn(0, Tileset.rodFrameCount - 1)]
        }
        drawTintedRegion(batch, vec, regionBorder, offsetX, offsetY, renderW, renderH)
        val regionFill: TintedRegion = if (!isInAir) {
            tileset.rodGroundFillAnimations[(animationAlpha * Tileset.rodFrameCount).toInt().coerceIn(0, Tileset.rodFrameCount - 1)]
        } else {
            tileset.rodAerialFillAnimations[(animationAlpha * Tileset.rodFrameCount).toInt().coerceIn(0, Tileset.rodFrameCount - 1)]
        }
        drawTintedRegion(batch, vec, regionFill, offsetX, offsetY, renderW, renderH)

        batch.setColor(1f, 1f, 1f, 1f)
    }
}

abstract class EntityRod(world: World, val deployBeat: Float)
    : EntityRodDecor(world), TemporaryEntity {

    companion object {
        const val EXPLODE_DELAY_SEC: Float = 1f / 3f
        const val GRAVITY: Float = -52f
        const val MIN_COLLISION_UPDATE_RATE: Int = 50
    }

    data class CollisionData(
            var collidedWithWall: Boolean = false,
            var velocityY: Float = 0f,
            var isInAir: Boolean = false,
            var bounce: Bounce? = null,
    )

    data class Bounce(val rod: EntityRod, val peakHeight: Float,
                      val startX: Float, val startY: Float, val endX: Float, val endY: Float,
                      val previousBounce: Bounce?) {

        fun getYFromX(x: Float): Float {
            if (previousBounce != null && x < startX) {
                return previousBounce.getYFromX(x)
            }

            val alpha = ((x - startX) / (endX - startX)).coerceIn(0f, 1f)
//            return if (endY < startY) {
//                // One continuous arc down from startY to endY
//                MathUtils.lerp(startY, endY, WaveUtils.getBounceWave(0.5f + alpha * 0.5f))
//            } else if (endY > peakHeight) {
//                // One continuous arc up from startY (bottom) to top
//                MathUtils.lerp(startY, endY, WaveUtils.getBounceWave(alpha * 0.5f))
//            } else {
            return if (alpha <= 0.5f) {
                MathUtils.lerp(startY, peakHeight, WaveUtils.getBounceWave(alpha))
            } else {
                MathUtils.lerp(peakHeight, endY, 1f - WaveUtils.getBounceWave(alpha))
            }
//            }
        }
    }

    protected val collision: CollisionData = CollisionData()
    override val isInAir: Boolean
        get() = collision.isInAir
    
    protected var engineUpdateLastSec: Float = Float.MAX_VALUE
    protected var collisionUpdateLastBeat: Float = Float.MAX_VALUE
    
    override fun getAnimationAlpha(): Float {
        val beatsFullAnimation = 60f / 128f
        val posX = this.position.x
        return ((((if (posX < 0f) (posX + floor(posX).absoluteValue) else posX) / xUnitsPerBeat) % beatsFullAnimation) / beatsFullAnimation)
    }


    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)

        if (engineUpdateLastSec == Float.MAX_VALUE) {
            engineUpdateLastSec = seconds
        }
        if (collisionUpdateLastBeat == Float.MAX_VALUE) {
            val beatDelta = beat - deployBeat
            collisionUpdateLastBeat = if (beatDelta > 0f) {
                deployBeat
            } else {
                beat
            }
        }

//        val engineUpdateDelta = seconds - engineUpdateLastSec

        val minCollisionUpdateInterval = 1f / MIN_COLLISION_UPDATE_RATE
        val collisionUpdateDeltaBeat = beat - collisionUpdateLastBeat
        var iterationCount = 0
        var updateCurrentBeat = collisionUpdateLastBeat
        var updateCurrentSec = engine.tempos.beatsToSeconds(updateCurrentBeat)
        var updateBeatTimeRemaining = collisionUpdateDeltaBeat
        while (updateBeatTimeRemaining > 0f) {
            val deltaBeat = updateBeatTimeRemaining.coerceAtMost(minCollisionUpdateInterval).coerceAtLeast(0f)
            val deltaSec = (engine.tempos.beatsToSeconds(updateCurrentBeat + deltaBeat) - engine.tempos.beatsToSeconds(updateCurrentBeat)).coerceAtLeast(0f)
            updateCurrentBeat += deltaBeat
            updateCurrentSec += deltaSec

            collisionCheck(engine, updateCurrentBeat, updateCurrentSec, deltaSec)

            updateBeatTimeRemaining -= minCollisionUpdateInterval
            iterationCount++
        }

        engineUpdateLastSec = seconds
        collisionUpdateLastBeat = beat
    }

    protected open fun collisionCheck(engine: Engine, beat: Float, seconds: Float, deltaSec: Float) {
    }

    protected fun playSfxLand(engine: Engine) {
        engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_land"))
    }

    protected fun playSfxSideCollision(engine: Engine) {
        engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_side_collision"))
    }

    protected fun playSfxExplosion(engine: Engine) {
        engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_explosion"))
    }
}

