package polyrhythmmania.storymode.gamemode.boss.scripting

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import paintbox.Paintbox
import polyrhythmmania.editor.block.BlockSpotlightSwitch
import polyrhythmmania.editor.block.SpotlightTimingMode
import polyrhythmmania.editor.block.data.SwitchedLightColor
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.EventPlayMenuSFX
import polyrhythmmania.engine.EventPlaySFX
import polyrhythmmania.engine.TextBox
import polyrhythmmania.engine.input.EventClearInputs
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.storymode.gamemode.boss.*
import polyrhythmmania.storymode.gamemode.boss.pattern.BossPatternPools
import polyrhythmmania.storymode.gamemode.boss.pattern.Pattern
import polyrhythmmania.storymode.music.StemID
import polyrhythmmania.world.EventRowBlockDespawn
import polyrhythmmania.world.EventRowBlockRetract
import polyrhythmmania.world.EventTextbox
import polyrhythmmania.world.World
import polyrhythmmania.world.entity.EntityRodDecor
import polyrhythmmania.world.spotlights.EventSpotlightTransition
import polyrhythmmania.world.spotlights.Spotlights
import polyrhythmmania.world.tileset.PaletteTransition
import polyrhythmmania.world.tileset.TransitionCurve
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
        
        
        private fun parseLightIndexSelection(selection: String): Set<Int> {
            return selection.mapNotNull { c ->
                if (c in '0'..'9') (c - '0') else null
            }.toSet()
        }
    }

    protected val world: World get() = engine.world
    protected val modifierModule: BossModifierModule = gamemode.modifierModule
    protected val random: Random get() = gamemode.random
    protected val patternPools: BossPatternPools get() = gamemode.patternPools
    
    private var currentPattern: Pattern? = null
    protected val lightSelections: MutableMap<Boolean, Set<Int>> =
        mutableMapOf(SIDE_DOWNSIDE to emptySet(), SIDE_UPSIDE to emptySet())

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

    protected fun MutableList<Event>.retractPistonsDownside(): MutableList<Event> {
        val retractStartBeat = 0f

        this.add(EventRowBlockRetract(engine, world.rowA, 0, retractStartBeat, affectThisIndexAndForward = true))

        return this
    }

    protected fun MutableList<Event>.retractPistonsUpside(): MutableList<Event> {
        val retractStartBeat = 0f

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

    protected fun MutableList<Event>.despawnPatternDownside(): MutableList<Event> {
        val despawnStartBeat = 0f

        this.add(EventRowBlockDespawn(engine, world.rowA, 0, despawnStartBeat, affectThisIndexAndForward = true))

        return this
    }

    protected fun MutableList<Event>.despawnPatternUpside(): MutableList<Event> {
        val despawnStartBeat = 0f

        this.add(EventRowBlockDespawn(engine, world.rowDpad, 0, despawnStartBeat, affectThisIndexAndForward = true))

        return this
    }

    protected fun MutableList<Event>.textBox(textbox: TextBox, duration: Float): MutableList<Event> {
        this.add(EventTextbox(engine, 0f, duration, textbox))

        return this
    }

    protected fun MutableList<Event>.targetLights(side: Boolean, selection: String): MutableList<Event> {
        val indices = parseLightIndexSelection(selection)

        lightSelections[side] = indices

        return this
    }

    protected fun MutableList<Event>.targetLights(side: Boolean, indices: Set<Int>): MutableList<Event> {
        lightSelections[side] = indices

        return this
    }

    protected fun MutableList<Event>.changeLightStrength(
        lightStrength: LightStrength,
        duration: Float,
        transitionCurve: TransitionCurve = TransitionCurve.SLOW_FAST
    ): MutableList<Event> {
        val startBeat = 0f
        val spotlights = engine.world.spotlights

        val timingMode = SpotlightTimingMode.INSTANT
        val paletteTransition = PaletteTransition(duration, transitionCurve, pulseMode = false, reverse = false)

        val ambientLight = SwitchedLightColor(
            lightStrength.ambientLightColorOverride ?: Spotlights.AMBIENT_LIGHT_RESET_COLOR,
            lightStrength.ambient
        )
        this += EventSpotlightTransition(
            engine,
            startBeat,
            paletteTransition,
            spotlights.ambientLight,
            ambientLight.color,
            ambientLight.strength
        )

        fun getSwitchedLightColorPairs(side: Boolean): List<Pair<Color?, Float?>> {
            val onIndices = lightSelections.getValue(side)
            return (0 until spotlights.numPerRow).map { i ->
                Pair(Spotlights.SPOTLIGHT_RESET_COLOR, if (i in onIndices) lightStrength.selected else 0f)
            }
        }

        this.addAll(
            BlockSpotlightSwitch.createSpotlightEvents(
                engine, spotlights,
                getSwitchedLightColorPairs(SIDE_DOWNSIDE),
                getSwitchedLightColorPairs(SIDE_UPSIDE),
                startBeat, timingMode, paletteTransition
            )
        )

        return this
    }

    protected fun MutableList<Event>.playSfx(sound: BeadsSound): MutableList<Event> {
        this += EventPlaySFX(engine, 0f, { sound })

        return this
    }

    protected fun MutableList<Event>.playMenuSfx(sound: Sound): MutableList<Event> {
        this += EventPlayMenuSFX(engine, 0f) { sound }

        return this
    }

}
