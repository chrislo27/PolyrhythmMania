package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import io.github.chrislo27.paintbox.Paintbox
import io.github.chrislo27.paintbox.registry.AssetRegistry
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.InputResult
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.util.WaveUtils
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.WorldRenderer
import kotlin.math.absoluteValue
import kotlin.math.floor

class EntityRod(world: World, val deployBeat: Float, val row: Row) : Entity(world) {

    companion object {
        private val tmpVec = Vector3()
        private val EXPLODE_DELAY_SEC: Float = 1f / 3f
    }

    data class InputTracker(var expectedInputIndices: MutableList<Int> = mutableListOf(),
                            val results: MutableList<InputResult> = mutableListOf())

    var collidedWithWall: Boolean = false
        private set
    private var fallState: FallState = FallState.Grounded
    private var initializedActiveBlocks = false
    private val activeBlocks: Array<EntityRowBlock.Type?> = Array(row.length) { null }
    val xUnitsPerBeat: Float = 2f
    private val killAfterBeats: Float = 4f + row.length / xUnitsPerBeat + 1 // 4 prior to first index 0 + rowLength/xUnitsPerBeat + 1 buffer
    private var explodeAtSec: Float = Float.MAX_VALUE
    private val isInAir: Boolean get() = fallState != FallState.Grounded
    val inputTracker: InputTracker = InputTracker()

    init {
        this.position.z = row.startZ.toFloat()
        this.position.y = row.startY.toFloat() + 1f
    }

    override fun getRenderWidth(): Float = 0.75f
    override fun getRenderHeight(): Float = 0.5f

    fun getCurrentIndex(posX: Float = this.position.x): Float = posX - row.startX
    fun getCurrentIndexFloor(posX: Float = this.position.x): Int = floor(getCurrentIndex(posX)).toInt()
    

    fun getPosXFromBeat(beatsFromDeploy: Float): Float {
        return (row.startX + 0.5f - 4 * xUnitsPerBeat) + (beatsFromDeploy) * xUnitsPerBeat - (6 / 32f)
    }

    fun explode(engine: Engine) {
        if (isKilled) return
        kill()
        world.addEntity(EntityExplosion(world, engine.seconds, this.getRenderWidth()).also {
            it.position.set(this.position)
        })
        engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_explosion"))
    }

    fun bounce(startIndex: Int, endIndex: Int) {
        val difference = endIndex - startIndex
        if (difference <= 0) return
        
        fun indexToX(index: Int): Float = index + row.startX + 0.25f
        
        val prevFallState = this.fallState
        if (prevFallState is FallState.Bouncing) {
            this.fallState = FallState.Bouncing(row.startY + 1f + difference,
                    indexToX(startIndex), row.startY + 1f, indexToX(endIndex), row.startY + 1f,
                    prevFallState)
        } else {
            this.fallState = FallState.Bouncing(row.startY + 1f + difference,
                    this.position.x, this.position.y, indexToX(endIndex), row.startY + 1f,
                    null)
        }
    }

    fun bounce(startIndex: Int) {
        if (initializedActiveBlocks && startIndex in 0 until row.length) {
            if (startIndex >= row.length - 1) {
                bounce(startIndex, startIndex + 1)
            } else {
                updateActiveBlocks()
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
        val posX = this.position.x
        val animationAlpha = ((((if (posX < 0f) (posX + floor(posX).absoluteValue) else posX) / xUnitsPerBeat) % beatsFullAnimation) / beatsFullAnimation).coerceIn(0f, 1f)
        val texReg: TextureRegion = if (!isInAir) {
            tileset.rodGroundAnimations[(animationAlpha * tileset.rodGroundFrames).toInt().coerceIn(0, tileset.rodGroundFrames - 1)]
        } else {
            tileset.rodAerialAnimations[(animationAlpha * tileset.rodAerialFrames).toInt().coerceIn(0, tileset.rodAerialFrames - 1)]
        }

        // Debug transparency for fall state
        if (Paintbox.debugMode) {
            when (fallState) {
                is FallState.Bouncing -> batch.setColor(1f, 1f, 1f, 0.25f)
                is FallState.Falling -> batch.setColor(1f, 1f, 1f, 0.75f)
            }
        }
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

    /**
     * Recomputes what blocks are active. Blocks that were already marked active do not get unmarked.
     * [inputTracker] will also be updated with the correct number of inputs expected.
     */
    fun updateActiveBlocks() {
        row.rowBlocks.forEachIndexed { index, entity ->
            if (activeBlocks[index] == null) {
                val type = if (!entity.active) null else entity.type
                activeBlocks[index] = type
                if (type != null && entity.type != EntityRowBlock.Type.PLATFORM) {
                    inputTracker.expectedInputIndices.add(index)
                }
            }
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
        val targetX = getPosXFromBeat(beatsFromDeploy)
        // The index that the rod is on
        val currentIndexFloat = getCurrentIndex(prevPosX) // /*targetX*/ 
        val currentIndex = floor(currentIndexFloat).toInt()

        // Initialize active blocks if not already done
        if (!initializedActiveBlocks && currentIndexFloat >= -0.7f) {
            updateActiveBlocks()
        }

        // Check for wall stop
        if (!collidedWithWall && (currentIndexFloat - currentIndex) >= 0.7f) {
            val nextIndex = currentIndex + 1
            if (nextIndex in 0 until row.length) {
                val next = row.rowBlocks[nextIndex]
                val heightOfNext = next.collisionHeight
                if (next.active && prevPosY in next.position.y..(next.position.y + heightOfNext - (1f / 32f))) {
                    collidedWithWall = true
//                    println("$seconds Collided with wall: currentIndex = ${currentIndexFloat}  x = ${this.position.x}")
                    this.position.x = currentIndex + 0.7f + row.startX
//                    println("$seconds After setting X: currentIndex would be ${this.position.x - row.startX}   x = ${this.position.x}")

                    val currentFallState = fallState
                    val fallVelo = if (currentFallState is FallState.Bouncing) {
                        (currentFallState.getYFromX(this.position.x) - currentFallState.getYFromX(prevPosX)) / deltaSec
                    } else 0f
                    if (currentFallState is FallState.Bouncing) {
                        fallState = FallState.Falling(fallVelo)
                    }
                    engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_side_collision"))

                    if (currentIndexFloat < 0f) {
                        // Standard collision detection will not take affect before index = 0
                        if (explodeAtSec == Float.MAX_VALUE) {
                            explodeAtSec = seconds + EXPLODE_DELAY_SEC
                        }
                    }
                }
            }
        }
        if (!collidedWithWall) {
            this.position.x = targetX
//            println("$seconds Set position X to target: ${targetX}")
        }

        // Control Y position
        if (initializedActiveBlocks && currentIndex in 0 until row.length) {
            when (val currentFallState = fallState) {
                is FallState.Bouncing -> {
                    // When bouncing, the Y position follows an arc until the target X position is reached
                    this.position.y = currentFallState.getYFromX(this.position.x)

                    val blockBelow = activeBlocks[currentIndex]
                    if (blockBelow != null) {
                        val entity = row.rowBlocks[currentIndex]
                        val topPart = entity.position.y + entity.collisionHeight
                        if (this.position.y < topPart) {
                            this.position.y = topPart
                        }
                    }

                    if (this.position.x >= currentFallState.endX) {
                        fallState = FallState.Grounded
                        engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_land"))
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
                            engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_land"))
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
//                                println("$seconds Set pos y to ${topPart}, currentIndex = $currentIndexFloat  x = ${this.position.x}")
                            }
                        }
                    }

                    if (explodeAtSec == Float.MAX_VALUE && collidedWithWall) {
                        explodeAtSec = seconds + EXPLODE_DELAY_SEC
                    }
                }
            }
        } else {
            // Before first row block
            this.position.y = row.startY.toFloat() + 1
        }
    }

    override fun onRemovedFromWorld(engine: Engine) {
        super.onRemovedFromWorld(engine)
        engine.inputter.submitInputsFromRod(this)
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
    class Bouncing(val peakHeight: Float, val startX: Float, val startY: Float, val endX: Float, val endY: Float,
                   val previousBounce: Bouncing?)
        : FallState() {

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

        override fun toString(): String {
            return "Bouncing(peakHeight=$peakHeight, startX=$startX, startY=$startY, endX=$endX, endY=$endY)"
        }

    }
}

