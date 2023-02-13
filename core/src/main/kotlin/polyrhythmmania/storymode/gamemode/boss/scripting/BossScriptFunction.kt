package polyrhythmmania.storymode.gamemode.boss.scripting

import paintbox.Paintbox
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.input.EventClearInputs
import polyrhythmmania.storymode.gamemode.boss.*
import polyrhythmmania.storymode.gamemode.boss.pattern.BossPatternPools
import polyrhythmmania.storymode.gamemode.boss.pattern.Pattern
import polyrhythmmania.storymode.music.StemID
import polyrhythmmania.world.EventRowBlockDespawn
import polyrhythmmania.world.EventRowBlockRetract
import polyrhythmmania.world.World
import polyrhythmmania.world.entity.EntityRodDecor
import java.util.*


abstract class BossScriptFunction(val gamemode: StoryBossGameMode, script: Script) : ScriptFunction(script) {
    
    companion object {

        private const val LOGGER_TAG = "BossScriptFunction"

        @JvmStatic
        protected val EXTRA_BEATS_SPACING: Int = 4

        @JvmStatic
        protected val SIDE_UPSIDE: Boolean = true

        @JvmStatic
        protected val SIDE_DOWNSIDE: Boolean = false

        @JvmStatic
        protected val DEFAULT_FLIP_CHANCE: Float = 1f / 6

        @JvmStatic
        protected val NO_FLIP_CHANCE: Float = 0f

        @JvmStatic
        protected val ALWAYS_FLIP_CHANCE: Float = 1f
    }

    protected val world: World get() = engine.world
    protected val modifierModule: BossModifierModule = gamemode.modifierModule
    protected val random: Random get() = gamemode.random
    protected val patternPools: BossPatternPools get() = gamemode.patternPools
    
    private var currentPattern: Pattern? = null

    /**
     * Gets the [currentPattern], or a blank pattern with a warning logged.
     */
    protected fun fetchCurrentPattern(): Pattern {
        return currentPattern ?: run {
            val logMsg = "MISSING currentPattern in BossScriptFunction"
            Paintbox.LOGGER.debug(logMsg, LOGGER_TAG)
            IllegalStateException(logMsg).printStackTrace()
            Pattern("--------", "--------")
        }
    }

    protected fun MutableList<Event>.music(stemID: String, measures: Int): MutableList<Event> {
        this.todo("Starting music, stemID ${stemID} for ${measures} measures")
        this.addEvent(BossMusicEvent(engine, gamemode.stems, stemID, 0f, (measures * 4).toFloat()))
        return this
    }

    protected fun MutableList<Event>.music(
        stemID: StemID,
        measures: Int,
        extraBeatDuration: Int = 0,
        specificVariant: Int? = null,
    ): MutableList<Event> {
        this.todo("Starting music, stemID ${stemID} (variant $specificVariant) for ${measures} measures")
        this.addEvent(
            BossMusicEvent(
                engine,
                gamemode.stems,
                if (specificVariant != null) stemID.getID(specificVariant) else stemID.getRandomID(random),
                0f,
                ((measures * 4) + extraBeatDuration).toFloat()
            )
        )
        return this
    }


    protected fun MutableList<Event>.spawnPattern(pattern: Pattern, flipChance: Float = DEFAULT_FLIP_CHANCE): MutableList<Event> {
        var patternToUse = pattern
        if (random.nextFloat() < flipChance) {
            patternToUse = pattern.flip()
        }
        
        val patternStart = 0f // Will be offset by Script, so should be zero

        currentPattern = patternToUse
        this.addAll(patternToUse.toEvents(engine, patternStart))

        return this
    }

    protected fun MutableList<Event>.spawnRods(): MutableList<Event> {
        val patternStart = -4f
        val pattern = fetchCurrentPattern()
        val anyA = pattern.anyA
        val anyDpad = pattern.anyDpad
        val bossDamageMultiplier = if (anyA && anyDpad) 1 else 2
        val damageTakenVar = EntityRodPRStoryBoss.PlayerDamageTaken()

        if (anyA) {
            this.add(
                EventDeployRodBoss(
                    engine, world.rowA, patternStart + pattern.delayDownside,
                    1f / pattern.rodDownside,
                    pattern.getLastPistonIndexDownside(),
                    damageTakenVar, bossDamageMultiplier
                )
            )
        }
        if (anyDpad) {
            this.add(
                EventDeployRodBoss(
                    engine, world.rowDpad, patternStart + pattern.delayUpside,
                    1f / pattern.rodUpside,
                    pattern.getLastPistonIndexUpside(),
                    damageTakenVar, bossDamageMultiplier
                )
            )
        }

        return this
    }

    protected fun MutableList<Event>.spawnOneRod(
        side: Boolean,
        damageTakenVar: EntityRodPRStoryBoss.PlayerDamageTaken,
        bossDamageMultiplier: Int,
        beatsPerBlock: Float = 1f / EntityRodDecor.DEFAULT_X_UNITS_PER_BEAT,
    ): MutableList<Event> {
        val patternStart = -4f
        val pattern = fetchCurrentPattern()

        if (side == SIDE_DOWNSIDE) {
            this.add(
                EventDeployRodBoss(
                    engine, world.rowA, patternStart,
                    1f / beatsPerBlock,
                    pattern.getLastPistonIndexDownside(),
                    damageTakenVar, bossDamageMultiplier
                )
            )
        } else {
            this.add(
                EventDeployRodBoss(
                    engine, world.rowDpad, patternStart,
                    1f / beatsPerBlock,
                    pattern.getLastPistonIndexUpside(),
                    damageTakenVar, bossDamageMultiplier
                )
            )
        }

        return this
    }

    protected fun MutableList<Event>.retractPistons(): MutableList<Event> {
        val retractStartBeat = 0f

        this.add(EventRowBlockRetract(engine, world.rowA, 0, retractStartBeat, affectThisIndexAndForward = true))
        this.add(EventRowBlockRetract(engine, world.rowDpad, 0, retractStartBeat, affectThisIndexAndForward = true))

        return this
    }

    protected fun MutableList<Event>.despawnPattern(): MutableList<Event> {
        val despawnStartBeat = 0f

        this.add(EventRowBlockDespawn(engine, world.rowA, 0, despawnStartBeat, affectThisIndexAndForward = true))
        this.add(EventRowBlockDespawn(engine, world.rowDpad, 0, despawnStartBeat, affectThisIndexAndForward = true))
        this.add(EventClearInputs(engine, despawnStartBeat))

        return this
    }

}
