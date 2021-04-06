package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Engine
import polyrhythmmania.util.WaveUtils
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.WorldRenderer
import java.sql.RowIdLifetime
import kotlin.math.floor

class EntityRod(world: World, val deployBeat: Float, val row: Row) : Entity(world) {

    companion object {
        private val tmpVec = Vector3()
    }

    var collidedWithWall: Boolean = false
        private set
    private var fallState: FallState = FallState.Grounded
    private var initializedActiveBlocks = false
    private val activeBlocks: Array<EntityRowBlock.Type?> = Array(row.length) { null }
    private val xUnitsPerBeat: Float = 2f
    private val killAfterBeats: Float = 4f + row.length / xUnitsPerBeat + 1 // 4 prior to first index 0 + rowLength/xUnitsPerBeat + 1 buffer
    private var explodeAtSec: Float = Float.MAX_VALUE
    private val isInAir: Boolean get() = fallState != FallState.Grounded

    init {
        this.position.z = row.startZ.toFloat()
        this.position.y = row.startY.toFloat() + 1f
    }

    override fun getRenderWidth(): Float = 0.75f
    override fun getRenderHeight(): Float = 0.5f

    fun getPosXFromBeat(beatsFromDeploy: Float): Float {
        return (row.startX + 0.5f - 4 * xUnitsPerBeat) + (beatsFromDeploy) * xUnitsPerBeat - (6 / 32f)
    }

    fun explode(engine: Engine) {
        if (isKilled) return
        kill()
        world.addEntity(EntityExplosion(world, engine.seconds, this.getRenderWidth()).also {
            it.position.set(this.position)
        })
    }
    
    fun bounce(startIndex: Int, endIndex: Int) {
        val difference = endIndex - startIndex
        if (difference <= 0) return
        fallState = FallState.Bouncing(row.startY + 1f + difference, this.position.x, this.position.y, endIndex.toFloat() + row.startX + 0.25f, row.startY + 1f)
    }
    
    fun bounce(startIndex: Int) {
        if (initializedActiveBlocks && startIndex in 0 until row.length) {
            if (startIndex >= row.length - 1) {
                bounce(startIndex, startIndex + 1)
            } else {
                initializeActiveBlocks()
                var nextNonNull = startIndex + 1
                for (i in startIndex + 1 until row.length) {
                    nextNonNull = i
                    if (activeBlocks[i] != null) {
                        break
                    }
                }
                bounce(startIndex, nextNonNull)
            }
        }
    }

    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine) {
        val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(this.position))

        val beatsFullAnimation = 60f / 128f
        val animationAlpha = (((this.position.x / xUnitsPerBeat) % beatsFullAnimation) / beatsFullAnimation).coerceIn(0f, 1f) //MathHelper.getSawtoothWave(System.currentTimeMillis(), 0.2f)
        val texReg: TextureRegion = if (!isInAir) {
            tileset.rodGroundAnimations[(animationAlpha * tileset.rodGroundFrames).toInt().coerceIn(0, tileset.rodGroundFrames - 1)]
        } else {
            tileset.rodAerialAnimations[(animationAlpha * tileset.rodAerialFrames).toInt().coerceIn(0, tileset.rodAerialFrames - 1)]
        }

        // Debug transparency for fall state
//        when (fallState) {
//            is FallState.Bouncing -> batch.setColor(1f, 1f, 1f, 0.25f)
//            is FallState.Falling -> batch.setColor(1f, 1f, 1f, 0.75f)
//        }
        batch.draw(texReg, convertedVec.x - (1 / 32f), convertedVec.y, getRenderWidth(), getRenderHeight())
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private var engineUpdateLastSec: Float = Float.MAX_VALUE

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)

        if (engineUpdateLastSec == Float.MAX_VALUE) {
            engineUpdateLastSec = seconds
        }

        val delta = seconds - engineUpdateLastSec

        collisionCheck(engine, beat, seconds, delta)

        if (seconds >= explodeAtSec) {
            explode(engine)
        } else if ((beat - deployBeat) >= killAfterBeats && !isKilled) {
            kill()
        }

        engineUpdateLastSec = seconds
    }
    
    private fun initializeActiveBlocks() {
        row.rowBlocks.forEachIndexed { index, entity ->
            activeBlocks[index] = if (!entity.active) null else entity.type
        }
        initializedActiveBlocks = true
    }

    private fun collisionCheck(engine: Engine, beat: Float, seconds: Float, deltaSec: Float) {
        val prevPosX = this.position.x
        val prevPosY = this.position.y
        val prevPosZ = this.position.z

        // Do collision check. Only works on the EntityRowBlocks for the given row.
        // 1. Stops instantly if it hits a prematurely deployed piston
        // 2. Stops if it hits a block when falling
        val beatsFromDeploy = beat - deployBeat
        val beatsFromFirst = beatsFromDeploy - 4
        val targetX = getPosXFromBeat(beatsFromDeploy)
        // The index that the rod is on
        val currentIndexFloat = targetX - row.startX
        val currentIndex = floor(currentIndexFloat).toInt()

        // Initialize active blocks if not already done
        if (!initializedActiveBlocks && currentIndexFloat >= -0.7f) {
            initializeActiveBlocks()
        }

        // Check for wall stop
        if (!collidedWithWall && (currentIndexFloat - currentIndex) >= 0.7f) {
            val nextIndex = currentIndex + 1
            if (nextIndex in 0 until row.length) {
                val next = row.rowBlocks[nextIndex]
                val heightOfNext = next.collisionHeight
                if (next.active && prevPosY in next.position.y..(next.position.y + heightOfNext - (1f / 40f))) {
                    collidedWithWall = true
                    this.position.x = currentIndex + 0.7f + row.startX
                    
                    val currentFallState = fallState
                    val fallVelo = if (currentFallState is FallState.Bouncing) {
                        (currentFallState.getYFromX(this.position.x) - currentFallState.getYFromX(prevPosX)) / deltaSec
                    } else 0f
                    fallState = FallState.Falling(fallVelo) // FIXME the velocity may not be 0 upon hit. Should compute if Bouncing
                }
            }
        }
        if (!collidedWithWall) {
            this.position.x = targetX
        }

        // Control Y position
        if (initializedActiveBlocks && currentIndex in 0 until row.length) {
            when (val currentFallState = fallState) {
                is FallState.Bouncing -> {
                    // When bouncing, the Y position follows an arc until the target X position is reached
                    this.position.y = currentFallState.getYFromX(this.position.x)
                    if (this.position.x >= currentFallState.endX) {
                        fallState = FallState.Grounded
                        // TODO play land sound
                    }
                }
                is FallState.Falling -> {
                    // Gravity acceleration
                    currentFallState.velocityY += (-50f) * deltaSec

                    // When falling, the Y position changes but collision detection is done to check for row start Y
                    val futureY = prevPosY + (currentFallState.velocityY * deltaSec)

                    // TODO make more robust. Only checks for row start Y
                    if (futureY <= row.startY) {
                        this.position.y = row.startY.toFloat()
                        if (currentFallState != FallState.Grounded) {
                            fallState = FallState.Grounded
                            // TODO play land sound
                        }
                    } else {
                        this.position.y = futureY
                    }
                }
                FallState.Grounded -> {
                    // When grounded, the y position does not change but has to be checked if we transition to Falling
                    // Transition to falling: entity block below is inactive (null)
                    if (this.position.y <= row.startY) {
                        // Don't go below floor level
                        this.position.y = row.startY.toFloat()
                    } else {
                        val blockBelow = activeBlocks[currentIndex]
                        if (blockBelow == null) {
                            fallState = FallState.Falling(0f)
                        } else {
                            val entity = row.rowBlocks[currentIndex]
                            val topPart = entity.position.y + entity.collisionHeight
                            if (this.position.y < topPart) {
                                this.position.y = topPart
                            }
                        }
                    }

                    if (explodeAtSec == Float.MAX_VALUE && collidedWithWall) {
                        explodeAtSec = seconds + (1 / 3f)
                    }
                }
            }
        } else {
            // Before first row block
            this.position.y = row.startY.toFloat() + 1
        }
    }
}

sealed class FallState {
    /**
     * The rod is not currently falling.
     */
    object Grounded : FallState()

    /**
     * The rod is currently falling BUT not from a bounce off a piston.
     */
    class Falling(var velocityY: Float) : FallState()

    /**
     * The rod is mid-bounce from a piston, and has a start/end position and given height.
     */
    class Bouncing(val peakHeight: Float, val startX: Float, val startY: Float, val endX: Float, val endY: Float)
        : FallState() {

        fun getYFromX(x: Float): Float {
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

        override fun toString(): String {
            return "Bouncing(peakHeight=$peakHeight, startX=$startX, startY=$startY, endX=$endX, endY=$endY)"
        }
        
    }
}
