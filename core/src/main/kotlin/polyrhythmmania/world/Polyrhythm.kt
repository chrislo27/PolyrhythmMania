package polyrhythmmania.world


import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.registry.AssetRegistry
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.SoundInterface
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.engine.input.InputResult
import polyrhythmmania.engine.input.InputScore
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.world.entity.EntityExplosion
import polyrhythmmania.world.entity.EntityInputIndicator
import polyrhythmmania.world.entity.EntityPiston
import polyrhythmmania.world.entity.EntityRod
import polyrhythmmania.world.entity.EntityRod.Companion.MIN_COLLISION_UPDATE_RATE
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.render.WorldRenderer
import kotlin.math.floor


class EntityRowBlock(world: World, val baseY: Float, val row: Row, val rowIndex: Int)
    : EntityPiston(world) {

    enum class SpawningState {
        NEUTRAL,
        SPAWNING,
        DESPAWNING
    }

    var spawningState: SpawningState = SpawningState.NEUTRAL
        private set
    private var spawnPercentage: Float = 0f

    init {
        this.position.y = baseY - 1f // Start in the ground
    }
    
    fun fullyExtendVanity(engine: Engine, beat: Float) {
        super.fullyExtend(engine, beat)
        row.updateInputIndicators()
    }

    override fun fullyExtend(engine: Engine, beat: Float) {
        super.fullyExtend(engine, beat)
        row.updateInputIndicators()

        when (type) {
            Type.PLATFORM -> {
            }
            Type.PISTON_A -> engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_input_a"), SoundInterface.SFXType.PLAYER_INPUT)
            Type.PISTON_DPAD -> engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_input_d"), SoundInterface.SFXType.PLAYER_INPUT)
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
    }

    override fun retract(): Boolean {
        val result = super.retract()
        row.updateInputIndicators()
        return result
    }

    fun spawn(percentage: Float) {
        val clamped = percentage.coerceIn(0f, 1f)
        if (spawnPercentage > clamped) return
        active = clamped > 0f
        position.y = Interpolation.linear.apply(baseY - 1, baseY, clamped)
        row.updateInputIndicators()
        spawningState = if (clamped <= 0f || clamped >= 1f) SpawningState.NEUTRAL else SpawningState.SPAWNING
        spawnPercentage = clamped
    }

    /**
     * Returns true if some property was affected.
     */
    fun despawn(percentage: Float): Boolean {
        val clamped = percentage.coerceIn(0f, 1f)
        if (spawnPercentage < (1f - clamped)) return false
        if (active) {
            active = clamped < 1f
            position.y = Interpolation.linear.apply(baseY, baseY - 1, clamped)
            row.updateInputIndicators()
            spawningState = if (clamped <= 0f || clamped >= 1f) SpawningState.NEUTRAL else SpawningState.DESPAWNING
            spawnPercentage = 1f - clamped
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

class EntityRodPR(world: World, deployBeat: Float, val row: Row,
                  val lifeLost: BooleanVar? = null)
    : EntityRod(world, deployBeat) {

    data class InputTracker(
            val totalResultCount: Int,
            val expected: MutableList<ExpectedInput> = MutableList(totalResultCount) { ExpectedInput.Unknown },
            val results: MutableList<InputResult> = mutableListOf(),
    )
    
    sealed class ExpectedInput {
        object Unknown : ExpectedInput()
        object Skipped : ExpectedInput()
        object InAir : ExpectedInput()
        class Expected(val thisIndex: Int, val nextJumpIndex: Int) : ExpectedInput() {
            override fun toString(): String {
                return "Expected(thisIndex=$thisIndex, nextJumpIndex=$nextJumpIndex)"
            }
        }
    }

    private val killAfterBeats: Float = 4f + row.length / xUnitsPerBeat + 1 // 4 prior to first index 0 + rowLength/xUnitsPerBeat + 1 buffer

    private var explodeAtSec: Float = Float.MAX_VALUE
    var exploded: Boolean = false
        private set

    private val visualPosition: Vector3 = Vector3().set(this.position)
    private var lastCurrentIndex: Float = -10000f
    var registeredMiss: Boolean = false
        private set

    val inputTracker: InputTracker = InputTracker(row.length)
    val acceptingInputs: Boolean
        get() = !collision.collidedWithWall && !exploded
    
    init {
        this.position.x = getPosXFromBeat(0f)
        this.position.z = row.startZ.toFloat()
        this.position.y = row.startY.toFloat() + 1f
        
        this.visualPosition.set(this.position)
    }

    override fun getRenderVec(): Vector3 {
        return visualPosition
    }

    fun getCurrentIndex(posX: Float = this.position.x): Float = posX - row.startX
    fun getCurrentIndexFloor(posX: Float = this.position.x): Int = floor(getCurrentIndex(posX)).toInt()

    fun getPosXFromBeat(beatsFromDeploy: Float): Float {
        return (row.startX + 0.5f - 4 * xUnitsPerBeat) + (beatsFromDeploy) * xUnitsPerBeat - (6 / 32f)
    }

    fun explode(engine: Engine) {
        if (isKilled || exploded) return
        exploded = true
        world.addEntity(EntityExplosion(world, engine.seconds, this.renderWidth).also {
            it.position.set(this.position)
        })
        playSfxExplosion(engine)
        registerMiss(engine.inputter)
        engine.inputter.onRodPRExploded()
        if (world.worldMode.showEndlessScore) {
            val lifeLostVar = this.lifeLost
            if (lifeLostVar != null) {
                if (!lifeLostVar.get()) {
                    lifeLostVar.set(true)
                    engine.inputter.triggerLifeLost()
                }
            }
        }
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
            bounce(startIndex, getLookaheadIndex(startIndex))
        }
    }

    /**
     * Gets the index where the rod would bounce to in the CURRENT world state. The returned index
     * may be out of bounds.
     */
    fun getLookaheadIndex(startIndex: Int): Int {
        return if (startIndex >= row.length - 1) {
            startIndex + 1
        } else {
            var nextNonNull = startIndex + 1
            for (i in startIndex + 1 until row.length) {
                nextNonNull = i
                val rowBlock = row.rowBlocks[i]
                if (rowBlock.active) {
                    break
                }
            }
            nextNonNull
        }
    }

    /**
     * Updates the internal [InputTracker]. This continues even if the rod has exploded.
     */
    fun updateInputTracking(beat: Float) {
        val currentIndexFloor: Int = floor(getCurrentIndex(getPosXFromBeat(beat - deployBeat))).toInt()
        if (currentIndexFloor < 0 || currentIndexFloor >= inputTracker.totalResultCount)
            return
        
        val currentExpected: ExpectedInput = inputTracker.expected[currentIndexFloor]
        if (currentExpected != ExpectedInput.Unknown)
            return

        val rowBlock = row.rowBlocks[currentIndexFloor]
        val type = if (!rowBlock.active) null else rowBlock.type
        if (type == null || type == EntityPiston.Type.PLATFORM) {
            inputTracker.expected[currentIndexFloor] = ExpectedInput.Skipped
        } else {
            // Lookahead to where it should land.
            val lookahead = getLookaheadIndex(currentIndexFloor)
            inputTracker.expected[currentIndexFloor] = ExpectedInput.Expected(currentIndexFloor, lookahead)
            for (i in currentIndexFloor + 1 until lookahead.coerceAtMost(inputTracker.totalResultCount - 1)) {
                if (inputTracker.expected[i] == ExpectedInput.Unknown)
                    inputTracker.expected[i] = ExpectedInput.InAir
            }
        }
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)

        this.visualPosition.z = this.position.z
        
        if (seconds >= explodeAtSec && !exploded) {
            explode(engine)
        } else if ((beat - deployBeat) >= killAfterBeats && !isKilled) {
            kill()
        }
        
        updateInputTracking(beat)
        
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
    override fun collisionCheck(engine: Engine, beat: Float, seconds: Float, deltaSec: Float) {
        if (exploded) return
        
        val prevPosX = this.position.x
        val prevPosY = this.position.y
        
        val unswungBeat = engine.tempos.secondsToBeats(engine.tempos.beatsToSeconds(beat, disregardSwing = false), disregardSwing = true)

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
        if (currentIndexFloat >= -0.7f || (floor(lastCurrentIndex).toInt() != floor(currentIndexFloat).toInt() && lastCurrentIndex >= 0)) {
            updateInputTracking(beat)
        }

        // Check for wall stop
        if (!collision.collidedWithWall && (currentIndexFloat - currentIndex) >= 0.80f && currentIndex >= -1 &&
                collision.bounce == null /* non-null Bounce indicates that the EngineInputter accepted it */) {
            val nextIndex = currentIndex + 1
            if (nextIndex in 0 until row.length) {
                val next = row.rowBlocks[nextIndex]
                val heightOfNext = next.collisionHeight
                var posYCheck = prevPosY
                if (currentIndex in 0 until row.length) {
                    val blockBelow: EntityRowBlock = row.rowBlocks[currentIndex]
                    val floorBelow: Float = if (!blockBelow.active) row.startY.toFloat() else {
                        blockBelow.position.y + blockBelow.collisionHeight
                    }
                    if (blockBelow.spawningState == EntityRowBlock.SpawningState.DESPAWNING && floorBelow + (2 / 32f) >= this.position.y) {
                        posYCheck = floorBelow
                    } else if (floorBelow >= posYCheck) {
                        posYCheck = floorBelow
                    } 
                }
                
                if (next.active && posYCheck in next.position.y..(next.position.y + heightOfNext - (1f / 32f))) {
//                    println("Collision: my Y ${prevPosY} ${posYCheck}   ${next.position.y}  ${next.position.y + heightOfNext}")
                    
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
            this.visualPosition.x = getPosXFromBeat(unswungBeat - deployBeat)
        } else {
            this.visualPosition.x = this.position.x
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
                this.visualPosition.y = currentBounce.getYFromX(this.position.x)
                collision.velocityY = (this.position.y - prevPosY) / deltaSec
            } else {
                val floorBelow: Float = if (!blockBelow.active) row.startY.toFloat() else {
                    blockBelow.position.y + blockBelow.collisionHeight
                }
                if (floorBelow >= this.position.y) { // Push the rod up to the floor height and kill velocityY
                    collision.velocityY = 0f
                    this.position.y = floorBelow
                } else if (blockBelow.spawningState == EntityRowBlock.SpawningState.DESPAWNING && floorBelow + (4 / 32f) >= this.position.y) {
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
                            if (blockBelow.spawningState != EntityRowBlock.SpawningState.DESPAWNING && currentIndex <= 12) {
                                playSfxLand(engine)
                            }
                            collision.velocityY = 0f
                        } else {
                            this.position.y = futureY
                        }
                    }
                }
                
                this.visualPosition.y = this.position.y

                // Auto-inputs
                val inputter = engine.inputter
                val currentExpectedInput: ExpectedInput? = inputTracker.expected.getOrNull(currentIndex)
                if (engine.autoInputs) {
                    if (collision.velocityY == 0f && blockBelow.active && blockBelow.type != EntityPiston.Type.PLATFORM
                            && blockBelow.pistonState == EntityPiston.PistonState.RETRACTED
                            && (currentExpectedInput is ExpectedInput.Expected && currentExpectedInput.thisIndex == currentIndex)
                            && currentIndexFloat - currentIndex in 0.25f..0.65f) {
                        blockBelow.fullyExtend(engine, beat)
                        inputter.attemptSkillStar(currentIndex / this.xUnitsPerBeat + this.deployBeat + 4f)
                        inputter.inputFeedbackFlashes[inputter.getInputFeedbackIndex(InputScore.ACE, false)] = seconds
                    }
                } else {
                    // Check if NOT in air but current expected input is that it should be in the air
                    if (currentExpectedInput == ExpectedInput.InAir) {
                        registerMiss(inputter)
                    } else {
                        // Edge case: only have to bounce one unit (check two units behind to give time for lates)
                        val lastIndex = currentIndex - 1
                        val last2ExpectedInput: ExpectedInput? = inputTracker.expected.getOrNull(lastIndex - 1)
                        if (last2ExpectedInput is ExpectedInput.Expected && last2ExpectedInput.nextJumpIndex == lastIndex) {
                            if (inputTracker.results.none { it.expectedIndex == last2ExpectedInput.thisIndex }) {
                                registerMiss(inputter)
                            }
                        }
                    }
                }
            }
        } else {
            // Set to row height when not in the block area
            this.position.y = row.startY.toFloat() + 1
            this.visualPosition.y = this.position.y
            this.collision.velocityY = 0f
        }
    }

    override fun onRemovedFromWorld(engine: Engine) {
        super.onRemovedFromWorld(engine)
        engine.inputter.submitInputsFromRod(this)
    }

    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine) {
        if (!exploded) super.render(renderer, batch, tileset, engine)
    }

    private fun registerMiss(inputter: EngineInputter) {
        if (!registeredMiss) {
            registeredMiss = true
            inputter.missed()
        }
    }
}
