package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import paintbox.registry.AssetRegistry
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.SoundInterface
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.util.WaveUtils
import polyrhythmmania.world.World
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.texturepack.TexturePack
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TintedRegion
import kotlin.math.absoluteValue
import kotlin.math.floor

open class EntityRodDecor(world: World, isInAir: Boolean = false) : SimpleRenderedEntity(world) {
    
    companion object {
        const val DEFAULT_X_UNITS_PER_BEAT: Float = 2f
    }
    
    var xUnitsPerBeat: Float = DEFAULT_X_UNITS_PER_BEAT
    open val isInAir: Boolean = isInAir
    
    open val renderScale: Float get() = 1f

    open val unscaledRenderWidth: Float = 0.75f
    open val unscaledRenderHeight: Float = 0.5f
    
    final override val renderWidth: Float get() = unscaledRenderWidth * renderScale
    final override val renderHeight: Float get() = unscaledRenderHeight * renderScale
    
    protected open val offsetX: Float get() = -((renderWidth - unscaledRenderWidth) / 2)
    protected open val offsetY: Float get() = -((renderHeight - unscaledRenderHeight) / 2)

    override val renderSortOffsetX: Float get() = 0f //sqrt((renderWidth * renderWidth) + (renderHeight * renderHeight))
    override val renderSortOffsetY: Float get() = 0f
    override val renderSortOffsetZ: Float get() = 0f // 0.5f * renderScale
    
    constructor(world: World) : this(world, false)
    
    protected open fun getAnimationAlpha(): Float {
        return 0f
    }
    
    protected open fun getTintColorOverrideBorder(region: TintedRegion): Color? = null
    protected open fun getTintColorOverrideFill(region: TintedRegion): Color? = null
    
    protected open fun getGroundBorderAnimations(tileset: Tileset): List<TintedRegion> {
        return tileset.rodAGroundBorderAnimations
    }
    protected open fun getAerialBorderAnimations(tileset: Tileset): List<TintedRegion> {
        return tileset.rodAAerialBorderAnimations
    }
    protected open fun getGroundFillAnimations(tileset: Tileset): List<TintedRegion> {
        return tileset.rodAGroundFillAnimations
    }
    protected open fun getAerialFillAnimations(tileset: Tileset): List<TintedRegion> {
        return tileset.rodAAerialFillAnimations
    }
    
    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        val animationAlpha = getAnimationAlpha().coerceIn(0f, 1f)

        val renderW = renderWidth
        val renderH = renderHeight
        val offsetX = this.offsetX
        val offsetY = this.offsetY
        val regionFill: TintedRegion = if (!isInAir) {
            getGroundFillAnimations(tileset)[(animationAlpha * TexturePack.rodFrameCount).toInt().coerceIn(0, TexturePack.rodFrameCount - 1)]
        } else {
            getAerialFillAnimations(tileset)[(animationAlpha * TexturePack.rodFrameCount).toInt().coerceIn(0, TexturePack.rodFrameCount - 1)]
        }
        drawTintedRegion(batch, vec, tileset, regionFill, offsetX, offsetY, renderW, renderH, tintColor = getTintColorOverrideFill(regionFill))
        val regionBorder: TintedRegion = if (!isInAir) {
            getGroundBorderAnimations(tileset)[(animationAlpha * TexturePack.rodFrameCount).toInt().coerceIn(0, TexturePack.rodFrameCount - 1)]
        } else {
            getAerialBorderAnimations(tileset)[(animationAlpha * TexturePack.rodFrameCount).toInt().coerceIn(0, TexturePack.rodFrameCount - 1)]
        }
        drawTintedRegion(batch, vec, tileset, regionBorder, offsetX, offsetY, renderW, renderH, tintColor = getTintColorOverrideBorder(regionBorder))

        batch.setColor(1f, 1f, 1f, 1f)

        // Debug bounds rendering
//        batch.setColor(0.8f, 0.8f, 1f, 0.75f)
//        batch.fillRect(vec.x, vec.y, renderW, renderH)
//        batch.setColor(0f, 0f, 1f, 0.75f)
//        batch.fillRect(vec.x, vec.y, 0.1f, 0.1f)
//        batch.fillRect(vec.x + unscaledRenderWidth / 2, vec.y + unscaledRenderHeight / 2, 0.1f, 0.1f)
//        batch.setColor(1f, 1f, 1f, 1f)
    }

    override fun setCullingRect(rect: Rectangle, tmpVec3: Vector3) {
        super.setCullingRect(rect, tmpVec3)
        rect.x += this.offsetX
        rect.y += this.offsetY
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
//            var isInAir: Boolean = false,
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
                MathUtils.lerp(endY, peakHeight, WaveUtils.getBounceWave(alpha))
            }
//            }
        }
    }

    protected open val collision: CollisionData = CollisionData()
    override val isInAir: Boolean
        get() = collision.bounce != null
    
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

    protected open fun playSfxLand(engine: Engine) {
        engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_land"), SoundInterface.SFXType.NORMAL)
    }

    protected open fun playSfxSideCollision(engine: Engine) {
        engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_side_collision"), SoundInterface.SFXType.NORMAL)
    }

    protected open fun playSfxExplosion(engine: Engine) {
        engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_explosion"), SoundInterface.SFXType.NORMAL)
    }
}

