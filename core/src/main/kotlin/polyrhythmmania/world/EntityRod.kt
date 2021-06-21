package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import paintbox.registry.AssetRegistry
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.InputResult
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.util.WaveUtils
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
        val offsetX = -(1 / 32f)
        val offsetY = 1f / 32f
        val regionBorder: TintedRegion = if (!isInAir) {
            tileset.rodGroundBorderAnimations[(animationAlpha * tileset.rodGroundFrameCount).toInt().coerceIn(0, tileset.rodGroundFrameCount - 1)]
        } else {
            tileset.rodAerialBorderAnimations[(animationAlpha * tileset.rodAerialFrameCount).toInt().coerceIn(0, tileset.rodAerialFrameCount - 1)]
        }
        drawTintedRegion(batch, vec, regionBorder, offsetX, offsetY, renderW, renderH)
        val regionFill: TintedRegion = if (!isInAir) {
            tileset.rodGroundFillAnimations[(animationAlpha * tileset.rodGroundFrameCount).toInt().coerceIn(0, tileset.rodGroundFrameCount - 1)]
        } else {
            tileset.rodAerialFillAnimations[(animationAlpha * tileset.rodAerialFrameCount).toInt().coerceIn(0, tileset.rodAerialFrameCount - 1)]
        }
        drawTintedRegion(batch, vec, regionFill, offsetX, offsetY, renderW, renderH)

        batch.setColor(1f, 1f, 1f, 1f)
    }
}

class EntityRod(world: World, val deployBeat: Float, val row: Row)
    : EntityRodDecor(world), TemporaryEntity {

    companion object {
        private const val EXPLODE_DELAY_SEC: Float = 1f / 3f
        private const val GRAVITY: Float = -52f
        private const val MIN_COLLISION_UPDATE_RATE: Int = 50
    }

    data class InputTracker(val expectedInputIndices: MutableList<Int> = mutableListOf(),
                            val results: MutableList<InputResult> = mutableListOf())

    private data class Collision(
            var collidedWithWall: Boolean = false,
            var velocityY: Float = 0f,
            var isInAir: Boolean = false,
            var bounce: Bounce? = null,
    )

    private data class Bounce(val rod: EntityRod, val peakHeight: Float,
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

    private val killAfterBeats: Float = 4f + row.length / xUnitsPerBeat + 1 // 4 prior to first index 0 + rowLength/xUnitsPerBeat + 1 buffer

    private var explodeAtSec: Float = Float.MAX_VALUE
    private val collision: Collision = Collision()
    override val isInAir: Boolean
        get() = collision.isInAir

    val inputTracker: InputTracker = InputTracker()
    val acceptingInputs: Boolean
        get() = !collision.collidedWithWall

    private var engineUpdateLastSec: Float = Float.MAX_VALUE
    private var collisionUpdateLastSec: Float = Float.MAX_VALUE
    private var lastCurrentIndex: Float = Float.MAX_VALUE

    init {
        this.position.x = getPosXFromBeat(0f)
        this.position.z = row.startZ.toFloat()
        this.position.y = row.startY.toFloat() + 1f
    }

    override fun getAnimationAlpha(): Float {
        val beatsFullAnimation = 60f / 128f
        val posX = this.position.x
        return ((((if (posX < 0f) (posX + floor(posX).absoluteValue) else posX) / xUnitsPerBeat) % beatsFullAnimation) / beatsFullAnimation)
    }

    fun getCurrentIndex(posX: Float = this.position.x): Float = posX - row.startX
    fun getCurrentIndexFloor(posX: Float = this.position.x): Int = floor(getCurrentIndex(posX)).toInt()
    
    fun getPosXFromBeat(beatsFromDeploy: Float): Float {
        return (row.startX + 0.5f - 4 * xUnitsPerBeat) + (beatsFromDeploy) * xUnitsPerBeat - (6 / 32f)
    }

    fun explode(engine: Engine) {
        if (isKilled) return
        kill()
        world.addEntity(EntityExplosion(world, engine.seconds, this.renderWidth).also {
            it.position.set(this.position)
        })
        playSfxExplosion(engine)
    }

    fun bounce(startIndex: Int, endIndex: Int) {
        val difference = endIndex - startIndex
        if (difference <= 0) return

        fun indexToX(index: Int): Float = index + row.startX + 0.25f

        val prevBounce = collision.bounce
        if (prevBounce != null) {
            collision.bounce = Bounce(this, row.startY + 1f + difference, indexToX(startIndex), row.startY + 1f, indexToX(endIndex), row.startY + 1f, prevBounce)
        } else {
            collision.bounce = Bounce(this, row.startY + 1f + difference, this.position.x, this.position.y, indexToX(endIndex), row.startY + 1f, null)
        }
    }

    fun bounce(startIndex: Int) {
        if (startIndex in 0 until row.length) {
            if (startIndex >= row.length - 1) {
                bounce(startIndex, startIndex + 1)
            } else {
                var nextNonNull = startIndex + 1
                for (i in startIndex + 1 until row.length) {
                    nextNonNull = i
                    val rowBlock = row.rowBlocks[i]
                    if (rowBlock.active) {
                        break
                    }
                }
                bounce(startIndex, nextNonNull)
            }
        }
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)

        if (engineUpdateLastSec == Float.MAX_VALUE) {
            engineUpdateLastSec = seconds
        }
        if (collisionUpdateLastSec == Float.MAX_VALUE) {
            val beatDeltaSec = engine.tempos.beatsToSeconds(beat) - engine.tempos.beatsToSeconds(deployBeat)
            collisionUpdateLastSec = if (beatDeltaSec > 0f) {
                engine.tempos.beatsToSeconds(deployBeat)
            } else {
                seconds
            }
        }

        val engineUpdateDelta = seconds - engineUpdateLastSec

        var collisionUpdateDelta = seconds - collisionUpdateLastSec
        val minCollisionUpdateInterval = 1f / MIN_COLLISION_UPDATE_RATE
        var count = 0
        while (collisionUpdateDelta > 0f) {
            collisionCheck(engine, beat, seconds, collisionUpdateDelta.coerceAtMost(minCollisionUpdateInterval))
            collisionUpdateDelta -= minCollisionUpdateInterval
            count++
        }

        if (seconds >= explodeAtSec) {
            explode(engine)
        } else if ((beat - deployBeat) >= killAfterBeats && !isKilled) {
            kill()
        }

        engineUpdateLastSec = seconds
        collisionUpdateLastSec = seconds
        lastCurrentIndex = getCurrentIndex(this.position.x)
    }

    /**
     * [inputTracker] will be updated with the correct number of inputs expected.
     */
    fun updateInputIndices(currentBeat: Float) {
        val lastExpectedSoFar = inputTracker.expectedInputIndices.lastOrNull() ?: -1
        val currentIndexFloor = getCurrentIndexFloor(this.position.x)
        row.rowBlocks.forEachIndexed { index, entity ->
            if (index > lastExpectedSoFar && index >= currentIndexFloor) {
                val type = if (!entity.active) null else entity.type
                if (type != null && entity.type != EntityRowBlock.Type.PLATFORM) {
                    inputTracker.expectedInputIndices.add(index)
                }
            }
        }
    }

    /**
     * Collision:
     * - Collision is checked per engine update with a minimum of [MIN_COLLISION_UPDATE_RATE] per engine second
     * - Rods move in a straight line on the X axis unless horizontally stopped
     * - Rods can be horizontally stopped if it collides with the side wall of a block (incl extended pistons)
     *   - Side collision is simply if (currentIndexFloat - currentIndex) >= 0.7f and the next block's Y > rod's Y pos
     * - When a bouncer bounces a rod, the bouncer determines where the landing point is at moment of bounce
     * - This puts the rod into a "bounce" state where it is not
     * - Rods can be moved up and down by movement of the platform/piston
     */
    private fun collisionCheck(engine: Engine, beat: Float, seconds: Float, deltaSec: Float) {
        val prevPosX = this.position.x
        val prevPosY = this.position.y

        // Do collision check. Only works on the EntityRowBlocks for the given row.
        // 1. Stops instantly if it hits a prematurely deployed piston
        // 2. Stops if it hits a block when falling
        val beatsFromDeploy = beat - deployBeat
        val targetX = getPosXFromBeat(beatsFromDeploy)
        // The index that the rod is on
        val currentIndexFloat = getCurrentIndex(prevPosX) // /*targetX*/ 
        val currentIndex = floor(currentIndexFloat).toInt()
        val collision = this.collision

        // Initialize active blocks
        if (currentIndexFloat >= -0.7f) {
            updateInputIndices(beat)
        } else if (floor(lastCurrentIndex).toInt() != floor(currentIndexFloat).toInt() && lastCurrentIndex >= 0) {
            updateInputIndices(beat)
        }

        // Check for wall stop
        if (!collision.collidedWithWall && (currentIndexFloat - currentIndex) >= 0.75f && currentIndex >= -1 && 
                collision.bounce == null /* non-null Bounce indicates that the EngineInputter accepted it */) {
            val nextIndex = currentIndex + 1
            if (nextIndex in 0 until row.length) {
                val next = row.rowBlocks[nextIndex]
                val heightOfNext = next.collisionHeight
                if (next.active && prevPosY in next.position.y..(next.position.y + heightOfNext - (1f / 32f))) {
                    collision.collidedWithWall = true
//                    println("$seconds Collided with wall: currentIndex = ${currentIndexFloat}  x = ${this.position.x}")
                    this.position.x = currentIndex + 0.7f + row.startX
//                    println("$seconds After setting X: currentIndex would be ${this.position.x - row.startX}   x = ${this.position.x}")

                    playSfxSideCollision(engine)

                    // Standard collision detection will not take effect before index = -1
                    if (explodeAtSec == Float.MAX_VALUE) {
                        explodeAtSec = seconds + EXPLODE_DELAY_SEC
                    }
                }
            }
        }

        // If not already collided with a wall, move X
        if (!collision.collidedWithWall) {
            this.position.x = targetX
        }

        // Control the Y position
        if (currentIndex in 0 until row.length) {
            if ((collision.bounce?.endX ?: Float.MAX_VALUE) < this.position.x) {
                collision.bounce = null
            }
            val currentBounce: Bounce? = collision.bounce
            val blockBelow: EntityRowBlock = row.rowBlocks[currentIndex]

            if (currentBounce != null) {
                val posX = this.position.x
                this.position.y = currentBounce.getYFromX(posX)
                collision.velocityY = (this.position.y - prevPosY) / deltaSec
            } else {
                val floorBelow: Float = if (!blockBelow.active) row.startY.toFloat() else {
                    blockBelow.position.y + blockBelow.collisionHeight
                }
                if (floorBelow >= this.position.y) { // Push the rod up to the floor height and kill velocityY
                    collision.velocityY = 0f
                    this.position.y = floorBelow
                } else if (blockBelow.retractionState == EntityRowBlock.RetractionState.RETRACTING && floorBelow + (2 / 32f) >= this.position.y) {
                    // Magnetize the rod to the retracting block to prevent landing SFX from playing
                    collision.velocityY = 0f
                    this.position.y = floorBelow
                } else {
                    collision.velocityY += GRAVITY * deltaSec

                    val veloY = collision.velocityY
                    if (veloY != 0f) {
                        val futureY = this.position.y + veloY * deltaSec
                        if (futureY < floorBelow) {
                            this.position.y = floorBelow
                            playSfxLand(engine)
                            collision.velocityY = 0f
                        } else {
                            this.position.y = futureY
                        }
                    }
                }
                
                if (engine.autoInputs) {
                    if (collision.velocityY == 0f && blockBelow.active && blockBelow.type != EntityRowBlock.Type.PLATFORM 
                            && blockBelow.pistonState == EntityRowBlock.PistonState.RETRACTED
                            && currentIndexFloat - currentIndex in 0.25f..0.65f) {
                        blockBelow.fullyExtend(engine, beat)
                    }
                }
            }
        } else {
            // Set to row height when not in the block area
            this.position.y = row.startY.toFloat() + 1
            this.collision.velocityY = 0f
        }
    }

    override fun onRemovedFromWorld(engine: Engine) {
        super.onRemovedFromWorld(engine)
        engine.inputter.submitInputsFromRod(this)
    }

    private fun playSfxLand(engine: Engine) {
        engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_land"))
    }

    private fun playSfxSideCollision(engine: Engine) {
        engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_side_collision"))
    }

    private fun playSfxExplosion(engine: Engine) {
        engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_explosion"))
    }
}

