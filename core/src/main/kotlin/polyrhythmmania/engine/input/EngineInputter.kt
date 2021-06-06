package polyrhythmmania.engine.input

import paintbox.Paintbox
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EntityRod
import polyrhythmmania.world.EntityRowBlock
import polyrhythmmania.world.World
import java.util.concurrent.CopyOnWriteArrayList


/**
 * Receives the player inputs.
 *
 * Input results are stored PER [EntityRod] as the input pattern is determined at runtime. See [EntityRod.InputTracker].
 *
 * **Flow:**
 *   - Pre-req: Inputs have to not be locked ([areInputsLocked] = `false`).
 *   - When a A or D-pad input is received, the appropriate pistons are triggered, assuming that inputs are not locked.
 *   - For each triggered piston:
 *     - For each [EntityRod] on the appropriate row for the piston:
 *       - Update its internal active blocks
 *       - Verify that the piston is the NEXT input for the rod
 *       - Check the timing from [InputThresholds]. If valid, then the rod can bounce and continue
 *
 *
 * **When a rod is killed after expiry:**
 *   - It sends its input data to this [EngineInputter]
 */
class EngineInputter(val engine: Engine) {

    private val world: World = engine.world
    var areInputsLocked: Boolean = true
    
    var totalExpectedInputs: Int = 0
        private set
    val inputResults: List<InputResult> = mutableListOf()
    
    fun clearInputs() {
        totalExpectedInputs = 0
        (inputResults as MutableList).clear()
    }

    fun onInput(type: InputType, atSeconds: Float) {
        if (areInputsLocked) return
        val atBeat = engine.tempos.secondsToBeats(atSeconds)
        val rowBlockType: EntityRowBlock.Type = when (type) {
            InputType.A -> EntityRowBlock.Type.PISTON_A
            InputType.DPAD -> EntityRowBlock.Type.PISTON_DPAD
        }

        for (row in world.rows) {
            row.updateInputIndicators()
            val activeIndex = row.nextActiveIndex
            if (activeIndex !in 0 until row.length) continue
            val rowBlock = row.rowBlocks[activeIndex]
            if (rowBlock.type != rowBlockType) continue

            for (entity in engine.world.entities) {
                if (entity !is EntityRod || entity.row !== row || !entity.acceptingInputs) continue
                val rod: EntityRod = entity
                rod.updateInputIndices()
                val inputTracker = rod.inputTracker
                if (inputTracker.results.size >= inputTracker.expectedInputIndices.size) {
//                    Paintbox.LOGGER.debug("$rod: Skipping input because results size >= expected inputs size (${inputTracker.results.size} >= ${inputTracker.expectedInputIndices.size})")
                    continue
                }
                val nextIndexIndex = inputTracker.results.size
                val nextBlockIndex = inputTracker.expectedInputIndices[nextIndexIndex]
                if (nextBlockIndex != activeIndex) {
//                    Paintbox.LOGGER.debug("$rod: Skipping input because nextBlockIndex != activeIndex (${nextBlockIndex} >= ${activeIndex})")
                    continue
                }
                
                // Compare timing
                val perfectBeats = rod.deployBeat + 4f + nextBlockIndex / rod.xUnitsPerBeat
                val perfectSeconds = engine.tempos.beatsToSeconds(perfectBeats)
                
                // TODO flexible timing solution, not using hardcoded constants in InputThresholds
                val differenceSec = atSeconds - perfectSeconds
                val minSec = perfectSeconds - InputThresholds.MAX_OFFSET_SEC
                val maxSec = perfectSeconds + InputThresholds.MAX_OFFSET_SEC

                Paintbox.LOGGER.debug("$rod: Input ${type}: perfectB=$perfectBeats, perfectS=$perfectSeconds, diff=$differenceSec, minmax=[$minSec, $maxSec], actual=$atSeconds")

                if (atSeconds !in minSec..maxSec) {
//                    Paintbox.LOGGER.debug("$rod: Skipping input because difference is not in bounds: perfect=$perfectSeconds, diff=$differenceSec, minmax=[$minSec, $maxSec], actual=$atSeconds")
                    continue
                }
                
                val accuracyPercent = (differenceSec / InputThresholds.MAX_OFFSET_SEC).coerceIn(-1f, 1f)
                inputTracker.results += InputResult(type, accuracyPercent, differenceSec)
                // Bounce the rod
                rod.bounce(nextBlockIndex)
                
            }
            
            // Trigger this piston
            rowBlock.fullyExtend(engine, atBeat)
        }
    }
    
    fun submitInputsFromRod(rod: EntityRod) {
        val inputTracker = rod.inputTracker
//        println("Submission from rod: ${inputTracker.expectedInputIndices}")
        totalExpectedInputs += inputTracker.expectedInputIndices.size
        (inputResults as MutableList).addAll(inputTracker.results)
    }
    
}

class EventLockInputs(engine: Engine, val lockInputs: Boolean)
    : Event(engine) {
    
    constructor(engine: Engine, lockInputs: Boolean, beat: Float) : this(engine, lockInputs) {
        this.beat = beat
    }

    override fun onStart(currentBeat: Float) {
        engine.inputter.areInputsLocked = lockInputs
    }
}