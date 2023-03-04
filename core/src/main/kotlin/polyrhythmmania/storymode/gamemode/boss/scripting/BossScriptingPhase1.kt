package polyrhythmmania.storymode.gamemode.boss.scripting

import com.badlogic.gdx.graphics.Color
import polyrhythmmania.editor.block.BlockSpotlightSwitch
import polyrhythmmania.editor.block.SpotlightTimingMode
import polyrhythmmania.editor.block.data.SwitchedLightColor
import polyrhythmmania.engine.Event
import polyrhythmmania.storymode.gamemode.boss.StoryBossGameMode
import polyrhythmmania.storymode.music.StemID
import polyrhythmmania.world.spotlights.EventSpotlightTransition
import polyrhythmmania.world.spotlights.Spotlights
import polyrhythmmania.world.tileset.PaletteTransition
import polyrhythmmania.world.tileset.TransitionCurve


open class BossScriptPhase1(gamemode: StoryBossGameMode, script: Script) : BossScriptFunction(gamemode, script) {

    override fun getEvents(): List<Event> {
        /*
    run boss1_a1;

    while (true) {
        run boss1_b2;
        run boss1_c;
        run boss1_b1;
        run boss1_b2;
        run boss1_d;
        run boss1_e1;
        run boss1_e2;
        run boss1_f;
        run boss1_a2;
    }

    interrupt_boss1_defeated:
        boss1_music_stop();
        boss1_defeat();
        rest 2.0;
        despawn_pattern();
        rest 6.0;

    return;
         */

        return mutableListOf<Event>()
            .addFunctionAsEvent(BossScriptPhase1A1(this))
            .addFunctionAsEvent(LoopingSegment())
    }

    private inner class LoopingSegment : BossScriptFunction(gamemode, script) {

        override fun getEvents(): List<Event> {
            
            class PlaceholderMusicStem(val stemID: StemID) : AbstractBossScriptPhase1Part(this@BossScriptPhase1) {

                override fun getEvents(): List<Event> {
                    return mutableListOf<Event>()
                        .music(stemID, 8, extraBeatDuration = EXTRA_BEATS_SPACING)
                        .todo("StemID: ${stemID.baseID}")
                        .rest(32.0f)
                }
            }

            /*

    while (true) {
        run boss1_b2;
        run boss1_c;
        run boss1_b1;
        run boss1_b2;
        run boss1_d;
        run boss1_e1;
        run boss1_e2;
        run boss1_f;
        run boss1_a2;
    }

             */
            
            val stemList = listOf(
                BossScriptPhase1B2(this@BossScriptPhase1),
                BossScriptPhase1C(this@BossScriptPhase1),
                BossScriptPhase1B1(this@BossScriptPhase1),
                BossScriptPhase1B2(this@BossScriptPhase1),
                BossScriptPhase1D(this@BossScriptPhase1),
                BossScriptPhase1E1(this@BossScriptPhase1),
                BossScriptPhase1E2(this@BossScriptPhase1),
                BossScriptPhase1F(this@BossScriptPhase1),
                BossScriptPhase1A2(this@BossScriptPhase1)
            )

            val list = mutableListOf<Event>()

            stemList.forEach { stem ->
                list.addFunctionAsEvent(stem)
            }

            list.addFunctionAsEvent(this)

            return list
        }
    }
}

data class LightStrength(val ambient: Float, val selected: Float) {
    companion object {
        val NORMAL: LightStrength = LightStrength(1.0f, 0.0f)
        val DARK1: LightStrength = LightStrength(0.15f, 1.0f)
        val DARK2: LightStrength = LightStrength(0.05f, 0.5f)
        val DARK3: LightStrength = LightStrength(0.0f, 0.25f)
    }
}

abstract class AbstractBossScriptPhase1Part(val phase1: BossScriptPhase1) : BossScriptFunction(
    phase1.gamemode,
    phase1.script
) {
    
    companion object {
        private fun parseLightIndexSelection(selection: String): Set<Int> {
            return selection.mapNotNull { c ->
                if (c in '0'..'9') (c - '0') else null
            }.toSet()
        }
    }

    protected val lightSelections: MutableMap<Boolean, Set<Int>> =
        mutableMapOf(SIDE_DOWNSIDE to emptySet(), SIDE_UPSIDE to emptySet())

    protected fun MutableList<Event>.targetLights(side: Boolean, selection: String): MutableList<Event> {
        val indices = parseLightIndexSelection(selection)

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

        val ambientLight = SwitchedLightColor(Spotlights.AMBIENT_LIGHT_RESET_COLOR, lightStrength.ambient)
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
            return (0 until 10).map { i ->
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
}


class BossScriptPhase1DebugLoop(
    gamemode: StoryBossGameMode,
    script: Script,
    val scriptFunctionFactory: (BossScriptPhase1DebugLoop) -> List<ScriptFunction>
) : BossScriptPhase1(gamemode, script) {

    override fun getEvents(): List<Event> {
        return mutableListOf<Event>()
            .addFunctionAsEvent(LoopingSegment())
    }

    private inner class LoopingSegment : BossScriptFunction(gamemode, script) {

        override fun getEvents(): List<Event> {
            val list = mutableListOf<Event>()
            
            val scriptFunctions = scriptFunctionFactory(this@BossScriptPhase1DebugLoop)
            if (scriptFunctions.isEmpty()) error("scriptFunctionFactory cannot return an empty list")
            
            scriptFunctions.forEach { scriptFunction ->
                list.addFunctionAsEvent(object : ScriptFunction(script) {
                    override fun getEvents(): List<Event> {
                        val ls = mutableListOf<Event>()
                        
                        ls.despawnPattern()
                        ls.addAll(scriptFunction.getEvents())
                        
                        return ls
                    }
                }) 
            }

            list.addFunctionAsEvent(this)

            return list
        }
    }
}
