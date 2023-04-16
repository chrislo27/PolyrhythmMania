package polyrhythmmania.engine.modifiers

import com.badlogic.gdx.math.Interpolation
import paintbox.binding.BooleanVar
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyBooleanVar
import paintbox.binding.ReadOnlyFloatVar
import paintbox.registry.AssetRegistry
import polyrhythmmania.container.Container
import polyrhythmmania.editor.block.storymode.BlockMonsterGoalPoints
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.ResultFlag
import polyrhythmmania.engine.SoundInterface
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.engine.input.InputResult
import polyrhythmmania.engine.input.InputScore
import polyrhythmmania.soundsystem.BeadsSound
import kotlin.math.pow

/**
 * Monster goal. Get aces to keep the monster at bay.
 * 
 * Requires that the engine has a [Container].
 */
class MonsterGoalData(parent: EngineModifiers) : ModifierModule(parent) {
    
    companion object {
        /**
         * Computes the [MonsterGoalData.passiveCountdownRate] based on the goal.
         * @param difficulty A value from 0-100, with higher values indicating higher difficulty. Values less than 50 may not be failable.
         * @param duration The number of SECONDS from monster start and monster end
         */
        fun computeMonsterRate(difficulty: Float, duration: Float): Float {
            if (duration <= 0f) return 0f
            return (10f.pow(difficulty.coerceAtLeast(0f) / 100f)) / (3 * duration)
        }
        
        fun computeCameraZoom(countdown: Float): Float {
            return Interpolation.sineIn.apply(1f, 4f, (1f - countdown).coerceIn(0f, 1f))
        }
    }
    
    // Settings
    /**
     * The difficulty from 0-100. Used in computing [passiveCountdownRate].
     * RHRE presets from easiest to hardest were: 70, 80, 85, 90.
     */
    val difficulty: FloatVar = FloatVar(MonsterPresets.MEDIUM.difficulty)
    /**
     * The recovery penalty value. Used in computing [passiveCountdownRate]. Higher means worse replenishment.
     * Ideally should be the total number of inputs in the level for balance. 
     */
    val recoveryPenalty: FloatVar = FloatVar(50f)
    
    
    // Data
    /**
     * Percentage of time (1.0 down to 0.0) until game over.
     */
    val untilGameOver: FloatVar = FloatVar(1f)

    /**
     * Rate per second of decrementing [untilGameOver]. Computed by [computeMonsterRate].
     */
    private val passiveCountdownRate: FloatVar = FloatVar(0f)

    /**
     * Amount [untilGameOver] is incremented by on an Ace input.
     */
    private val countdownReplenishOnAce: ReadOnlyFloatVar = FloatVar {
        passiveCountdownRate.use() * (activeDuration.use() / recoveryPenalty.use().coerceAtLeast(1f)) * 1.5f
    } // passiveCountdownRate * (countedDurationSec / numResultsExpected) * 1.5f
    
    /**
     * The duration in *SECONDS* between the start and end points. -1 on reset, computed on first engine update.
     */
    val activeDuration: ReadOnlyFloatVar = FloatVar(-1f)
    
    /**
     * Set by events to mark the start and end range.
     */
    val canStartCountingDown: BooleanVar = BooleanVar(false)

    /**
     * True if monster goal is active, i.e. the monster is actively trying to eat the player.
     */
    private val isMonsterGoalActive: ReadOnlyBooleanVar = BooleanVar { 
        this@MonsterGoalData.enabled.use() && passiveCountdownRate.use() > 0f && canStartCountingDown.use() && activeDuration.use() > 0f && untilGameOver.use() > 0f
    }
    /**
     * Beat when the last monster ace SFX was played.
     */
    private val lastMonsterAceSfx: FloatVar = FloatVar(-1f)
    
    
    override fun resetState() {
        untilGameOver.set(1f)
        (activeDuration as FloatVar).set(-1f)
        passiveCountdownRate.set(0f)
        lastMonsterAceSfx.set(-1f)
        canStartCountingDown.set(false)
    }

    override fun engineUpdate(beat: Float, seconds: Float, deltaSec: Float) {
        if (this.enabled.get()) {
            if (activeDuration.get() < 0f && engine.container != null) {
                val blocks = engine.container.blocks
                val startBlock = blocks.find { it is BlockMonsterGoalPoints && it.start }
                val endBlock = blocks.find { it is BlockMonsterGoalPoints && !it.start }

                activeDuration as FloatVar
                if (startBlock != null && endBlock != null) {
                    val tempos = engine.tempos
                    activeDuration.set((tempos.beatsToSeconds(endBlock.beat) - tempos.beatsToSeconds(startBlock.beat)).coerceAtLeast(0f))
                    
                    passiveCountdownRate.set(computeMonsterRate(difficulty.get(), activeDuration.get()))
                } else {
                    activeDuration.set(0f)
                }
            }
            
            if (isMonsterGoalActive.get()) {
                untilGameOver.set((untilGameOver.get() - passiveCountdownRate.get() * deltaSec).coerceAtLeast(0f))
                if (untilGameOver.get() <= 0f) {
                    onGameOver(beat)
                }
            }
        }
    }

    fun onGameOver(beat: Float) {
        engine.resultFlag.set(ResultFlag.Fail.MonsterGoal)
    }
    
    fun onAceHit(silent: Boolean = false) {
        if (isMonsterGoalActive.get()) {
            untilGameOver.set((untilGameOver.get() + countdownReplenishOnAce.get()).coerceIn(0f, 1f))
            
            if (!silent) {
                val engineBeat = engine.beat
                val beatsSinceLastAce = (engineBeat - lastMonsterAceSfx.get()).coerceIn(0f, 4f)
                val volume: Float = when {
                    beatsSinceLastAce < 0.75f -> 1f
                    beatsSinceLastAce >= 0.75f && beatsSinceLastAce < 2f -> 0.35f
                    beatsSinceLastAce >= 2f && beatsSinceLastAce < 3f -> 0.62f
                    else -> 1f
                }
                engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_monster_goal_ace"), SoundInterface.SFXType.NORMAL) { player ->
                    player.gain = volume
                }
                lastMonsterAceSfx.set(engineBeat)
            }
        }
    }

    override fun onSkillStarHit(beat: Float) {
        super.onSkillStarHit(beat)
        
        // Skill Star gives 3x the recovery
        onAceHit(silent = true)
        onAceHit(silent = true)
    }

    override fun onInputResultHit(inputter: EngineInputter, result: InputResult, countsAsMiss: Boolean) {
        if (isMonsterGoalActive.get() && !countsAsMiss && result.inputScore == InputScore.ACE) {
            onAceHit()
        }
    }

    override fun onMissed(inputter: EngineInputter, firstMiss: Boolean) {
        // NO-OP
    }
}

class EventMonsterGoalPoint(engine: Engine, startBeat: Float, val canStart: Boolean)
    : Event(engine) {

    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        engine.modifiers.monsterGoal.canStartCountingDown.set(canStart)
    }
}

enum class MonsterPresets(val difficulty: Float) {
    EASY(70f),
    MEDIUM(80f),
    HARD(85f),
    SUPER_HARD(90f),
    ;
    
    companion object {
        val VALUES: List<MonsterPresets> = values().toList()
    }
}
