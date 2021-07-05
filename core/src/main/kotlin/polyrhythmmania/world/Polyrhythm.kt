package polyrhythmmania.world


import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import paintbox.registry.AssetRegistry
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.InputResult
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.world.entity.EntityExplosion
import polyrhythmmania.world.entity.EntityInputIndicator
import polyrhythmmania.world.entity.EntityPiston
import polyrhythmmania.world.entity.EntityRod
import polyrhythmmania.world.entity.EntityRod.Companion.MIN_COLLISION_UPDATE_RATE
import kotlin.math.floor


class EntityRowBlock(world: World, val baseY: Float, val row: Row, val rowIndex: Int)
    : EntityPiston(world) {

    enum class RetractionState {
        NEUTRAL,
        EXTENDING,
        RETRACTING
    }

    var retractionState: RetractionState = RetractionState.NEUTRAL
        private set
    private var retractionPercentage: Float = 0f

    init {
        this.position.y = baseY - 1f // Start in the ground
    }

    override fun fullyExtend(engine: Engine, beat: Float) {
        super.fullyExtend(engine, beat)

        when (type) {
            Type.PLATFORM -> {
            }
            Type.PISTON_A -> engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_input_a"))
            Type.PISTON_DPAD -> engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_input_d"))
        }

        // For auto-inputs only. For regular inputs, see EngineInputter
        if (this.type != Type.PLATFORM && engine.autoInputs) {
            // Bounce any rods that are on this index
            world.entities.forEach { entity ->
                if (entity is EntityRodPR) {
                    // The index that the rod is on
                    val currentIndexFloat = entity.position.x - entity.row.startX
                    val currentIndex = floor(currentIndexFloat).toInt()
                    if (currentIndex == this.rowIndex && MathUtils.isEqual(entity.position.z, this.position.z)
                            && entity.position.y in (this.position.y + 1f - (1f / 32f))..(this.position.y + collisionHeight)) {
                        entity.bounce(currentIndex)
                    }
                }
            }
        }

        row.updateInputIndicators()
    }

    override fun retract(): Boolean {
        val result = super.retract()
        row.updateInputIndicators()
        return result
    }

    fun spawn(percentage: Float) {
        val clamped = percentage.coerceIn(0f, 1f)
        if (retractionPercentage > clamped) return
        active = clamped > 0f
        position.y = Interpolation.linear.apply(baseY - 1, baseY, clamped)
        row.updateInputIndicators()
        retractionState = if (clamped <= 0f) RetractionState.NEUTRAL else RetractionState.EXTENDING
        retractionPercentage = clamped
    }

    /**
     * Returns true if some property was affected.
     */
    fun despawn(percentage: Float): Boolean {
        val clamped = percentage.coerceIn(0f, 1f)
        if (retractionPercentage < (1f - clamped)) return false
        if (active) {
            active = clamped < 1f
            position.y = Interpolation.linear.apply(baseY, baseY - 1, clamped)
            row.updateInputIndicators()
            retractionState = if (clamped < 1f) RetractionState.NEUTRAL else RetractionState.RETRACTING
            retractionPercentage = 1f - clamped
            return true
        }
        return false
    }
}

class Row(val world: World, val length: Int, val startX: Int, val startY: Int, val startZ: Int, val isDpad: Boolean) {

    val rowBlocks: List<EntityRowBlock> = List(length) { index ->
        EntityRowBlock(world, startY.toFloat(), this, index).apply {
            this.type = EntityPiston.Type.PLATFORM
            this.active = false
            this.position.x = (startX + index).toFloat()
            this.position.z = startZ.toFloat()
        }
    }
    val inputIndicators: List<EntityInputIndicator> = List(length) { index ->
        EntityInputIndicator(world, isDpad).apply {
            this.visible = false
            this.position.x = (startX + index).toFloat() + 1f
            this.position.z = startZ.toFloat()
            this.position.y = startY + 1f + (2f / 32f)

            // Offset to -X +Z for render order
            this.position.z += 1
            this.position.x -= 1
            this.position.y += 1
        }
    }

    /**
     * The index of the next piston-type [EntityRowBlock] to be triggered.
     * Updated with a call to [updateInputIndicators]. -1 if there are none.
     */
    var nextActiveIndex: Int = -1
        private set

    fun initWithWorld() {
        rowBlocks.forEach {
            world.addEntity(it)
            it.retract()
            it.despawn(1f)
            it.type = EntityPiston.Type.PLATFORM
        }
        inputIndicators.forEach(world::addEntity)
        updateInputIndicators()
    }

    fun updateInputIndicators() {
        var foundActive = false
        for (i in 0 until length) {
            val rowBlock = rowBlocks[i]
            val inputInd = inputIndicators[i]

            if (foundActive) {
                inputInd.visible = false
            } else {
                if (rowBlock.active && rowBlock.type != EntityPiston.Type.PLATFORM && rowBlock.pistonState == EntityPiston.PistonState.RETRACTED) {
                    foundActive = true
                    inputInd.visible = true
                    inputInd.isDpad = rowBlock.type == EntityPiston.Type.PISTON_DPAD
                    nextActiveIndex = i
                } else {
                    inputInd.visible = false
                }
            }
        }
        if (!foundActive) {
            nextActiveIndex = -1
        }
    }

}

class EntityRodPR(world: World, val deployBeat: Float, val row: Row) : EntityRod(world) {
    
    data class InputTracker(val expectedInputIndices: MutableList<Int> = mutableListOf(),
                            val results: MutableList<InputResult> = mutableListOf())
    
    private val killAfterBeats: Float = 4f + row.length / xUnitsPerBeat + 1 // 4 prior to first index 0 + rowLength/xUnitsPerBeat + 1 buffer

    private var explodeAtSec: Float = Float.MAX_VALUE

    private var engineUpdateLastSec: Float = Float.MAX_VALUE
    private var collisionUpdateLastBeat: Float = Float.MAX_VALUE
    private var lastCurrentIndex: Float = -10000f

    val inputTracker: InputTracker = InputTracker()
    val acceptingInputs: Boolean
        get() = !collision.collidedWithWall
    
    init {
        this.position.x = getPosXFromBeat(0f)
        this.position.z = row.startZ.toFloat()
        this.position.y = row.startY.toFloat() + 1f
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

    /**
     * [inputTracker] will be updated with the correct number of inputs expected.
     */
    fun updateInputIndices(currentBeat: Float) {
        val lastExpectedSoFar = inputTracker.expectedInputIndices.lastOrNull() ?: -1
        val currentIndexFloor = getCurrentIndexFloor(this.position.x)
        if (currentIndexFloor < -1) return
        row.rowBlocks.forEachIndexed { index, entity ->
            if (index > lastExpectedSoFar && index >= currentIndexFloor) {
                val type = if (!entity.active) null else entity.type
                if (type != null && entity.type != EntityPiston.Type.PLATFORM) {
                    inputTracker.expectedInputIndices.add(index)
                }
            }
        }
//        println("[EntityRod] $this Input indices updated for current index $currentIndexFloor: ${inputTracker.expectedInputIndices}")
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

        if (seconds >= explodeAtSec) {
            explode(engine)
        } else if ((beat - deployBeat) >= killAfterBeats && !isKilled) {
            kill()
        }

        engineUpdateLastSec = seconds
        collisionUpdateLastBeat = beat
        lastCurrentIndex = getCurrentIndex(this.position.x)
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

                // Auto-inputs
                if (engine.autoInputs) {
                    if (collision.velocityY == 0f && blockBelow.active && blockBelow.type != EntityPiston.Type.PLATFORM
                            && blockBelow.pistonState == EntityPiston.PistonState.RETRACTED
                            && currentIndexFloat - currentIndex in 0.25f..0.65f) {
                        blockBelow.fullyExtend(engine, beat)
                        engine.inputter.attemptSkillStar(currentIndex / this.xUnitsPerBeat + this.deployBeat + 4f)
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
        engine.inputter.submitInputsFromRod(this, collision.collidedWithWall)
    }
}
