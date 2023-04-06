package polyrhythmmania.storymode.gamemode.boss.scripting

import polyrhythmmania.engine.Event
import polyrhythmmania.storymode.gamemode.boss.StoryBossGameMode
import polyrhythmmania.storymode.music.StemID


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

abstract class AbstractBossScriptPhase1Part(val phase1: BossScriptPhase1) : BossScriptFunction(
    phase1.gamemode,
    phase1.script
)


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
