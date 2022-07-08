package polyrhythmmania.engine.input

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.math.MathUtils
import paintbox.Paintbox
import paintbox.binding.*
import paintbox.registry.AssetRegistry
import polyrhythmmania.engine.*
import polyrhythmmania.engine.input.practice.PracticeData
import polyrhythmmania.engine.modifiers.EngineModifiers
import polyrhythmmania.gamemodes.SidemodeAssets
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.world.*
import polyrhythmmania.world.entity.EntityPiston
import kotlin.math.max


/**
 * Receives the player inputs.
 *
 * Input results are stored PER [EntityRodPR] as the input pattern is determined at runtime. See [EntityRodPR.InputTracker].
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

    companion object {
        const val DEBUG_LOG_INPUTS: Boolean = false
        const val BEAT_EPSILON: Float = 0.01f
    }

    private val world: World = engine.world
    private val modifiers: EngineModifiers get() = engine.modifiers // Must be get() because modifiers depends on inputter first

    val inputCountStats: InputCountStats = InputCountStats()
    var areInputsLocked: Boolean = true
    var skillStarBeat: Float = Float.POSITIVE_INFINITY
    val inputChallenge: InputChallengeData = InputChallengeData()
    val inputterListeners: MutableList<InputterListener> = mutableListOf()

    val practice: PracticeData = PracticeData()

    val inputFeedbackFlashes: FloatArray = FloatArray(5) { -10000f }
    var totalExpectedInputs: Int = 0
        private set
    var noMiss: Boolean = true
        private set
    var minimumInputCount: Int = 0

    val skillStarGotten: BooleanVar = BooleanVar(false)
    val inputResults: List<InputResult> = mutableListOf()
    val expectedInputsPr: List<EntityRodPR.ExpectedInput.Expected> = mutableListOf()

    init {
        resetState()
    }

    fun clearInputs(beforeBeat: Float = Float.POSITIVE_INFINITY) {
        totalExpectedInputs = 0
        (inputResults as MutableList).removeIf { it.perfectBeat < beforeBeat }
        (expectedInputsPr as MutableList).removeIf { it.perfectBeat < beforeBeat }
        practice.requiredInputs = emptyList()
    }

    fun resetState() {
        clearInputs()
        inputFeedbackFlashes.fill(-10000f)
        noMiss = true
        skillStarGotten.set(false)
        skillStarBeat = Float.POSITIVE_INFINITY
        practice.reset()
        inputCountStats.reset()
    }

    fun onButtonPressed(release: Boolean, type: InputType) {
        if (type == InputType.A) {
            val activeTextBox = engine.activeTextBox
            if (activeTextBox != null && activeTextBox.textBox.requiresInput && activeTextBox.secondsTimer <= 0f) {
                if (!activeTextBox.isADown) {
                    if (!release) {
                        engine.soundInterface.playMenuSfx(AssetRegistry.get<Sound>("sfx_text_advance_1"))
                        activeTextBox.isADown = true
                    }
                } else {
                    if (release) {
                        engine.soundInterface.playMenuSfx(AssetRegistry.get<Sound>("sfx_text_advance_2"))
                        engine.removeActiveTextbox(unpauseSoundInterface = true, runTextboxOnComplete = true)
                    }
                }
            } else {
                if (!release) {
                    onInput(InputType.A)
                }
            }
        } else {
            if (!release) {
                onInput(InputType.DPAD_ANY)
            }
        }
    }

    private fun onInput(type: InputType, atSeconds: Float = engine.seconds) {
        if (areInputsLocked || engine.activeTextBox?.textBox?.requiresInput == true) return

        val atBeat = engine.tempos.secondsToBeats(atSeconds)

        val worldMode = world.worldMode
        if (worldMode.worldType is WorldType.Polyrhythm) {
            val rowBlockType: EntityPiston.Type = if (type.isDpad) EntityPiston.Type.PISTON_DPAD else EntityPiston.Type.PISTON_A

            for (row in world.rows) {
                row.updateInputIndicators()
                val activeIndex = row.nextActiveIndex
                if (activeIndex !in 0 until row.length) continue
                val rowBlock = row.rowBlocks[activeIndex]
                if (rowBlock.type != rowBlockType) continue

                for (entity in engine.world.entities) {
                    if (entity !is EntityRodPR || entity.row !== row || !entity.acceptingInputs) continue
                    val rod: EntityRodPR = entity
                    rod.updateInputTracking(atBeat)
                    val inputTracker = rod.inputTracker
                    val nextBlockIndex = entity.getCurrentIndexFloor(entity.getPosXFromBeat(atBeat - entity.deployBeat))
                    if (nextBlockIndex !in 0 until entity.row.length) continue // Not in range
                    if (inputTracker.expected[nextBlockIndex] !is EntityRodPR.ExpectedInput.Expected) continue // Not an expected input
                    if (nextBlockIndex != activeIndex) { // Not the current active index
//                    Paintbox.LOGGER.debug("$rod: Skipping input because nextBlockIndex != activeIndex (${nextBlockIndex} >= ${activeIndex})")
                        if (activeIndex < nextBlockIndex) {
                            missed()
                        }
                        continue
                    }
                    if (inputTracker.results.any { it.expectedIndex == nextBlockIndex }) { // Prevent potential duplicates
                        // Technically shouldn't happen since the active index changes when updateInputIndicators is called
                        Paintbox.LOGGER.debug("$rod: duplicate input for index $nextBlockIndex caught and ignored!")
                        continue
                    }

                    // Compare timing
                    val perfectBeats = rod.getBeatForIndex(nextBlockIndex)
                    val perfectSeconds = engine.tempos.beatsToSeconds(perfectBeats)

                    val differenceSec = atSeconds - perfectSeconds
                    val minSec = perfectSeconds - InputThresholds.MAX_OFFSET_SEC
                    val maxSec = perfectSeconds + InputThresholds.MAX_OFFSET_SEC

                    val accuracyPercent = (differenceSec / InputThresholds.MAX_OFFSET_SEC).coerceIn(-1f, 1f)
                    val inputResult = InputResult(perfectBeats, type, accuracyPercent, differenceSec, activeIndex)
                    if (DEBUG_LOG_INPUTS) {
                        Paintbox.LOGGER.debug("${rod.toString().substringAfter("polyrhythmmania.world.Entity")}: Input ${type}: ${if (differenceSec < 0) "EARLY" else if (differenceSec > 0) "LATE" else "PERFECT"} ${inputResult.inputScore} \t | perfectBeat=$perfectBeats, perfectSec=$perfectSeconds, diffSec=$differenceSec, minmaxSec=[$minSec, $maxSec], actualSec=$atSeconds")
                    }
                    inputTracker.results += inputResult

                    val countsAsMiss = inputChallenge.isInputScoreMiss(inputResult.inputScore)
                    if (practice.practiceModeEnabled && !countsAsMiss) {
                        val allWereHit = practice.requiredInputs.all { it.wasHit }
                        if (!allWereHit) {
                            practice.requiredInputs.forEach { ri ->
                                if (!ri.wasHit && type.isInputEquivalent(ri.inputType) && MathUtils.isEqual(perfectBeats, ri.beat, BEAT_EPSILON)) {
                                    ri.wasHit = true
                                    ri.hitScore = inputResult.inputScore
                                }
                            }
                            if (practice.requiredInputs.all { it.wasHit }) {
                                val newValue = (practice.moreTimes.get() - 1).coerceAtLeast(0)
                                practice.moreTimes.set(newValue)
                                if (newValue == 0) {
                                    engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_practice_moretimes_2"), SoundInterface.SFXType.NORMAL)
                                    practice.clearText = 1f
                                } else {
                                    engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_practice_moretimes_1"), SoundInterface.SFXType.NORMAL)
                                }
                            }
                        }
                    }

                    val inputFeedbackIndex: Int = getInputFeedbackIndex(inputResult.inputScore, inputResult.accuracySec < 0f)
                    if (inputFeedbackIndex in inputFeedbackFlashes.indices) {
                        inputFeedbackFlashes[inputFeedbackIndex] = atSeconds
                    }

                    inputterListeners.forEach { it.onInputResultHit(this, inputResult, countsAsMiss) }

                    // Bounce the rod
                    if (!countsAsMiss) {
                        rod.bounce(nextBlockIndex)
                        if (inputResult.inputScore == InputScore.ACE) {
                            attemptSkillStar(perfectBeats)
                        }
                    } else {
                        missed()
                    }
                }

                // Trigger this piston (will also call updateInputIndicators)
                rowBlock.fullyExtend(engine, atBeat)
            }
        } else if (worldMode.worldType == WorldType.Dunk && type == InputType.A) {
            for (entity in engine.world.entities) {
                if (entity !is EntityRodDunk || !entity.acceptingInputs) continue
                val rod: EntityRodDunk = entity
                val nextBlockIndex = entity.getCurrentIndexFloor(entity.getPosXFromBeat(atBeat - entity.deployBeat))
                val activeIndex = 0
                if (nextBlockIndex != activeIndex) continue // Not in range

                // Compare timing
                val perfectBeats = rod.deployBeat + 3f + nextBlockIndex / rod.xUnitsPerBeat
                val perfectSeconds = engine.tempos.beatsToSeconds(perfectBeats)

                val differenceSec = atSeconds - perfectSeconds
                val minSec = perfectSeconds - InputThresholds.MAX_OFFSET_SEC
                val maxSec = perfectSeconds + InputThresholds.MAX_OFFSET_SEC

                val accuracyPercent = (differenceSec / InputThresholds.MAX_OFFSET_SEC).coerceIn(-1f, 1f)
                val inputResult = InputResult(perfectBeats, type, accuracyPercent, differenceSec, activeIndex)
                if (DEBUG_LOG_INPUTS) {
                    Paintbox.LOGGER.debug("${rod.toString().substringAfter("polyrhythmmania.world.Entity")}: Input ${type}: ${if (differenceSec < 0) "EARLY" else if (differenceSec > 0) "LATE" else "PERFECT"} ${inputResult.inputScore} \t | perfectBeat=$perfectBeats, perfectSec=$perfectSeconds, diffSec=$differenceSec, minmaxSec=[$minSec, $maxSec], actualSec=$atSeconds")
                }

                if (!modifiers.endlessScore.enabled.get()) {
                    (inputResults as MutableList).add(inputResult)
                }

                val inputFeedbackIndex: Int = getInputFeedbackIndex(inputResult.inputScore, inputResult.accuracySec < 0f)
                if (inputFeedbackIndex in inputFeedbackFlashes.indices) {
                    inputFeedbackFlashes[inputFeedbackIndex] = atSeconds
                }

                val countsAsMiss = inputChallenge.isInputScoreMiss(inputResult.inputScore)
                inputterListeners.forEach { it.onInputResultHit(this, inputResult, countsAsMiss) }

                // Bounce the rod regardless of miss; if aces only, bounce still has to apply for early/late
                rod.bounce(engine, inputResult)
                if (countsAsMiss) { // Explicitly checking for MISS, since rod.bounce returns immediately if it is MISS
                    missed()
                }
            }
            world.dunkPiston.fullyExtend(engine, atBeat)
        } else if (worldMode.worldType == WorldType.Assemble && type == InputType.A) {
            val asmPlayerPiston = world.asmPlayerPiston
            var hit = false
            var hitDuration = 1.5f
            if (asmPlayerPiston.pistonState == EntityPiston.PistonState.RETRACTED) {
                for (entity in engine.world.entities) {
                    if (entity !is EntityRodAsm || !entity.acceptingInputs) continue
                    val rod: EntityRodAsm = entity

                    // Compare timing
                    val nextExpected = rod.expectedInputs.lastOrNull() ?: continue
                    val perfectBeats = nextExpected.inputBeat
                    val perfectSeconds = engine.tempos.beatsToSeconds(perfectBeats)

                    val differenceSec = atSeconds - perfectSeconds
                    val minSec = perfectSeconds - InputThresholds.MAX_OFFSET_SEC
                    val maxSec = perfectSeconds + InputThresholds.MAX_OFFSET_SEC

                    if (atSeconds !in minSec..maxSec) {
                        continue
                    }

                    val accuracyPercent = (differenceSec / InputThresholds.MAX_OFFSET_SEC).coerceIn(-1f, 1f)
                    val inputResult = InputResult(perfectBeats, type, accuracyPercent, differenceSec, 0)
                    if (DEBUG_LOG_INPUTS) {
                        Paintbox.LOGGER.debug("${rod.toString().substringAfter("polyrhythmmania.world.Entity")}: Input ${type}: ${if (differenceSec < 0) "EARLY" else if (differenceSec > 0) "LATE" else "PERFECT"} ${inputResult.inputScore} \t | perfectBeat=$perfectBeats, perfectSec=$perfectSeconds, diffSec=$differenceSec, minmaxSec=[$minSec, $maxSec], actualSec=$atSeconds")
                    }

                    if (!modifiers.endlessScore.enabled.get()) {
                        (inputResults as MutableList).add(inputResult)
                    }

                    val inputFeedbackIndex: Int = getInputFeedbackIndex(inputResult.inputScore, inputResult.accuracySec < 0f)
                    if (inputFeedbackIndex in inputFeedbackFlashes.indices) {
                        inputFeedbackFlashes[inputFeedbackIndex] = atSeconds
                    }

                    val countsAsMiss = inputChallenge.isInputScoreMiss(inputResult.inputScore)
                    inputterListeners.forEach { it.onInputResultHit(this, inputResult, countsAsMiss) }

                    // Bounce the rod
                    if (!countsAsMiss) {
                        rod.addInputResult(engine, inputResult)
                        hit = true
                        hitDuration = 1f
                    }
                }

                val animation = asmPlayerPiston.animation
                if (animation is EntityPistonAsm.Animation.Neutral) {
                    asmPlayerPiston.fullyExtend(engine, atBeat, hitDuration, doWiggle = hit)
                    if (!hit) {
                        engine.soundInterface.playAudioNoOverlap(SidemodeAssets.assembleSfx.getValue("sfx_asm_middle_right"), SoundInterface.SFXType.PLAYER_INPUT) {
                            it.pitch = 1.5f
                        }
                    }
                } else if (animation is EntityPistonAsm.Animation.Charged && !hit) {
                    // Release charge
                    val piston = world.asmPlayerPiston
                    piston.animation = EntityPistonAsm.Animation.Fire(piston, atBeat)
                    piston.retract()
                    engine.soundInterface.playAudioNoOverlap(SidemodeAssets.assembleSfx.getValue("sfx_asm_shoot"), SoundInterface.SFXType.PLAYER_INPUT)

                    for (entity in engine.world.entities) {
                        if (entity !is EntityRodAsm || !entity.acceptingInputs) continue
                        val nextExpected = entity.expectedInputs.lastOrNull() ?: continue
                        if (nextExpected.isFire) {
                            entity.disableInputs = true
                        }
                    }
                }
            }
        }
    }

    fun getInputFeedbackIndex(score: InputScore, early: Boolean): Int {
        return when (score) {
            InputScore.ACE -> 2
            InputScore.GOOD -> if (early) 1 else 3
            InputScore.BARELY -> if (early) 0 else 4
            InputScore.MISS -> -1
        }
    }

    fun submitInputsFromRod(rod: EntityRodPR) {
        val inputTracker = rod.inputTracker

        val numExpected = inputTracker.expected.count { it is EntityRodPR.ExpectedInput.Expected }
        val validResults = inputTracker.results.filter { !inputChallenge.isInputScoreMiss(it.inputScore) }

        totalExpectedInputs += numExpected
        val endlessScore = modifiers.endlessScore
        if (endlessScore.enabled.get()) {
            // Special case when in endless mode
            // Inputs don't count when lives = 0
            if (engine.areStatisticsEnabled && endlessScore.lives.get() > 0) {
                inputCountStats.total += numExpected
                inputCountStats.missed += (numExpected - validResults.size).coerceAtLeast(0)
                inputCountStats.aces += validResults.count { it.inputScore == InputScore.ACE }
                inputCountStats.goods += validResults.count { it.inputScore == InputScore.GOOD }
                inputCountStats.barelies += validResults.count { it.inputScore == InputScore.BARELY }
                inputCountStats.early += validResults.count { it.inputScore != InputScore.ACE && it.accuracyPercent < 0f }
                inputCountStats.late += validResults.count { it.inputScore != InputScore.ACE && it.accuracyPercent > 0f }
            }
        } else {
            (inputResults as MutableList).addAll(validResults)
            (expectedInputsPr as MutableList).addAll(inputTracker.expected.filterIsInstance<EntityRodPR.ExpectedInput.Expected>())
            if (engine.areStatisticsEnabled && !rod.exploded && numExpected > 0 && validResults.size == numExpected) {
                GlobalStats.rodsFerriedPolyrhythm.increment()
            }
        }

        if (!engine.autoInputs && noMiss && !rod.registeredMiss) {
            if ((rod.exploded && numExpected > 0) || (numExpected > validResults.size) || inputTracker.results.any { inputChallenge.isInputScoreMiss(it.inputScore) }) {
                missed()
            }
        }
    }

    /**
     * Called when a miss is flagged. This could be called on an input or when a rod explodes, etc, so it shouldn't be
     * used as an indicator for endless lives being lost.
     */
    fun missed() {
        val wasNoMiss = this.noMiss
        this.noMiss = false

        inputterListeners.forEach { it.onMissed(this, wasNoMiss) }
    }

    fun attemptSkillStar(beat: Float): Boolean {
        if (!skillStarGotten.get() && MathUtils.isEqual(skillStarBeat, beat, BEAT_EPSILON)) {
            skillStarGotten.set(true)
            onSkillStarHit()
            return true
        }
        return false
    }

    fun onSkillStarHit() {
        engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_skill_star"), SoundInterface.SFXType.PLAYER_INPUT) { player ->
            player.gain = 0.6f
        }
        if (engine.areStatisticsEnabled) {
            GlobalStats.skillStarsEarned.increment()
        }
    }

    fun addNonEndlessInputStats() {
        if (!engine.areStatisticsEnabled) return
        if (modifiers.endlessScore.enabled.get()) return

        when (world.worldMode.worldType) {
            is WorldType.Polyrhythm, WorldType.Assemble, WorldType.Dunk -> {
                val results = this.inputResults
                val nInputs = max(results.size, max(totalExpectedInputs, minimumInputCount))
                val nonMissResults = results.filter { !inputChallenge.isInputScoreMiss(it.inputScore) }
                val missed = nInputs - nonMissResults.size
                val aces = results.count { it.inputScore == InputScore.ACE }
                val goods = results.count { it.inputScore == InputScore.GOOD }
                val barelies = results.count { it.inputScore == InputScore.BARELY }
                val earlies = nonMissResults.count { it.inputScore != InputScore.ACE && it.accuracyPercent < 0f }
                val lates = nonMissResults.count { it.inputScore != InputScore.ACE && it.accuracyPercent > 0f }

                GlobalStats.inputsGottenTotal.increment(nInputs)
                GlobalStats.inputsMissed.increment(missed)
                GlobalStats.inputsGottenAce.increment(aces)
                GlobalStats.inputsGottenGood.increment(goods)
                GlobalStats.inputsGottenBarely.increment(barelies)
                GlobalStats.inputsGottenEarly.increment(earlies)
                GlobalStats.inputsGottenLate.increment(lates)
            }
        }
    }

}
