package polyrhythmmania.storymode.gamemode.boss.scripting

import polyrhythmmania.engine.Event
import polyrhythmmania.engine.input.EventClearInputs
import polyrhythmmania.storymode.gamemode.boss.*
import polyrhythmmania.storymode.gamemode.boss.pattern.BossPatternPools
import polyrhythmmania.storymode.gamemode.boss.pattern.Pattern
import polyrhythmmania.storymode.music.StemID
import polyrhythmmania.world.EventRowBlockDespawn
import polyrhythmmania.world.EventRowBlockRetract
import polyrhythmmania.world.World


abstract class BossScriptFunction(val gamemode: StoryBossGameMode, script: Script) : ScriptFunction(script) {
    
    companion object {
        @JvmStatic
        protected val EXTRA_MEASURES_SPACING: Int = 1
    }

    protected val world: World get() = engine.world
    protected val modifierModule: BossModifierModule = gamemode.modifierModule
    protected val patternPools: BossPatternPools get() = gamemode.patternPools

    protected fun MutableList<Event>.music(stemID: String, measures: Int): MutableList<Event> =
        this.addEvent(BossMusicEvent(engine, gamemode.stems, stemID, 0f, (measures * 4).toFloat()))

    protected fun MutableList<Event>.music(
        stemID: StemID,
        measures: Int,
        specificVariant: Int? = null,
    ): MutableList<Event> =
        this.addEvent(
            BossMusicEvent(
                engine,
                gamemode.stems,
                if (specificVariant != null) stemID.getID(specificVariant) else stemID.getRandomID(gamemode.random),
                0f,
                (measures * 4).toFloat()
            )
        )


    protected fun MutableList<Event>.spawnPattern(pattern: Pattern): MutableList<Event> {
        val patternStart = 0f // Will be offset by Script, so should be zero

        this.addAll(pattern.toEvents(engine, patternStart))

        return this
    }

    protected fun MutableList<Event>.spawnRods(pattern: Pattern): MutableList<Event> {
        val patternStart = -4f

        val anyA = pattern.anyA
        val anyDpad = pattern.anyDpad
        val bossDamageMultiplier = if (anyA && anyDpad) 1 else 2
        val damageTakenVar = EntityRodPRStoryBoss.PlayerDamageTaken()

        if (anyA) {
            this.add(
                EventDeployRodBoss(
                    engine, world.rowA, patternStart + pattern.delayDownside,
                    Pattern.DEFAULT_X_UNITS_PER_BEAT * pattern.rodDownside,
                    pattern.getLastPistonIndexDownside(),
                    damageTakenVar, bossDamageMultiplier
                )
            )
        }
        if (anyDpad) {
            this.add(
                EventDeployRodBoss(
                    engine, world.rowDpad, patternStart + pattern.delayUpside,
                    Pattern.DEFAULT_X_UNITS_PER_BEAT * pattern.rodUpside,
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
