package polyrhythmmania.storymode.gamemode.boss.scripting

import polyrhythmmania.engine.Event
import polyrhythmmania.storymode.music.StoryMusicAssets


class BossScriptPhase1A1(phase1: BossScriptPhase1) : AbstractBossScriptPhase1Part(phase1) {

    override fun getEvents(): List<Event> {
        /*
    set_music(boss1_a1);

    retract_pistons();
    rest 1.0;
    despawn_pattern();
    rest 3.0;
    spawn_pattern(endless_easy[random()]);
    rest 4.0;

    spawn_rods();
    rest 3.75;
    retract_pistons();
    rest 0.25;
    spawn_rods();
    rest 4.0;

    retract_pistons();
    rest 1.0;
    despawn_pattern();
    rest 3.0;
    spawn_pattern(endless_easy[random()]);
    rest 4.0;

    spawn_rods();
    rest 3.75;
    retract_pistons();
    rest 0.25;
    spawn_rods();
    rest 4.0;

    return;
         */

        val list = mutableListOf<Event>()

        list.music(StoryMusicAssets.STEM_ID_BOSS_1_A1, 8, extraBeatDuration = EXTRA_BEATS_SPACING)

        repeat(2) {
            val pattern = patternPools.boss1_a1_patterns.iter.next()

            list
                .retractPistons()
                .rest(1.0f)
                .despawnPattern()
                .rest(3.0f)
                .spawnPattern(pattern)
                .rest(4.0f)

                .spawnRods()
                .rest(3.75f)
                .retractPistons()
                .rest(0.25f)
                .spawnRods()
                .rest(4.0f)
        }

        return list
    }
}

class BossScriptPhase1A2(phase1: BossScriptPhase1) : AbstractBossScriptPhase1Part(phase1) {

    override fun getEvents(): List<Event> {
        /*
    set_music(boss1_a2);

    retract_pistons();
    rest 1.0;
    despawn_pattern();
    rest 3.0;
    spawn_pattern(endless_medium[random()]);
    rest 4.0;

    spawn_rods();
    rest 3.75;
    retract_pistons();
    rest 0.25;
    spawn_rods();
    rest 4.0;

    retract_pistons();
    rest 1.0;
    despawn_pattern();
    rest 3.0;
    spawn_pattern(endless_medium[random()]);
    rest 4.0;

    spawn_rods();
    rest 3.75;
    retract_pistons();
    rest 0.25;
    spawn_rods();
    rest 4.0;
         */

        val list = mutableListOf<Event>()

        list.music(StoryMusicAssets.STEM_ID_BOSS_1_A2, 8, extraBeatDuration = EXTRA_BEATS_SPACING)

        repeat(2) {
            val pattern = patternPools.boss1_a2_patterns.iter.next()

            list
                .retractPistons()
                .rest(1.0f)
                .despawnPattern()
                .rest(3.0f)
                .spawnPattern(pattern)
                .rest(4.0f)

                .spawnRods()
                .rest(3.75f)
                .retractPistons()
                .rest(0.25f)
                .spawnRods()
                .rest(4.0f)
        }

        return list
    }
}
