package polyrhythmmania.storymode.gamemode.boss.scripting

import polyrhythmmania.engine.Event
import polyrhythmmania.storymode.gamemode.boss.StoryBossGameMode


class BossScriptPhase2(gamemode: StoryBossGameMode, script: Script) : BossScriptFunction(gamemode, script) {

    override fun getEvents(): List<Event> {
        /*
    run boss1_a1;       // (duration = 32.0)

    while (true) {
        run boss1_b2;   // (duration = 32.0)
        run boss1_c;    // (duration = 32.0)
        run boss1_b1;   // (duration = 32.0)
        run boss1_b2;   // (duration = 32.0)
        run boss1_d;    // (duration = 32.0)
        run boss1_e1;   // (duration = 32.0)
        run boss1_e2;   // (duration = 32.0)
        run boss1_f;    // (duration = 32.0)
        run boss1_a2;   // (duration = 32.0)
    }

    interrupt_boss1_defeated:
        boss_music1_stop();
        boss_defeat1();
        rest 2.0;
        despawn_pattern();
        rest 6.0;

    run boss_end;
         */

        // TODO
        return mutableListOf<Event>()
    }
}
