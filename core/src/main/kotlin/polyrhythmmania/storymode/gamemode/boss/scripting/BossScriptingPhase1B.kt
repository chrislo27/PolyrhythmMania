package polyrhythmmania.storymode.gamemode.boss.scripting

import polyrhythmmania.engine.Event
import polyrhythmmania.storymode.music.StoryMusicAssets


class BossScriptPhase1B1(phase1: BossScriptPhase1) : AbstractBossScriptPhase1Part(phase1) {

    override fun getEvents(): List<Event> {
        /*
    set_music(boss1_b1);

    retract_pistons();
    rest 1.0;
    despawn_pattern();
    rest 3.0;
    spawn_pattern(boss1_b_patterns[random()]);
    rest 4.0;

    spawn_rods();
    rest 4.0;
    rest 2.0;
    retract_pistons();
    rest 1.0;
    despawn_pattern();
    rest 1.0;

    spawn_pattern(boss1_b_patterns[random()]);
    rest 4.0;
    spawn_rods();
    rest 3.75;
    despawn_pattern();
    rest 0.25;

    spawn_pattern(boss1_b_patterns[random()]);
    rest 4.0;
    spawn_rods();
    rest 4.0;

 */

        val list = mutableListOf<Event>()

        val patternPool = patternPools.boss1_b_patterns

        list.music(StoryMusicAssets.STEM_ID_BOSS_1_B1, 8, extraBeatDuration = EXTRA_BEATS_SPACING)

        list
            .retractPistons()
            .rest(1.0f)
            .despawnPattern()
            .rest(3.0f)
            .spawnPattern(patternPool.iter.next())
            .rest(4.0f)

            .spawnRods()
            .rest(4.0f)
            .rest(2.0f)
            .retractPistons()
            .rest(1.0f)
            .despawnPattern()
            .rest(1.0f)

            .spawnPattern(patternPool.iter.next())
            .rest(4.0f)
            .spawnRods()
            .rest(3.75f)
            .despawnPattern()
            .rest(0.25f)

            .spawnPattern(patternPool.iter.next())
            .rest(4.0f)
            .spawnRods()
            .rest(4.0f)

        return list
    }
}

class BossScriptPhase1B2(phase1: BossScriptPhase1) : AbstractBossScriptPhase1Part(phase1) {

    override fun getEvents(): List<Event> {
        /*
        
    set_music(boss1_b2);

    retract_pistons();
    rest 1.0;
    despawn_pattern();
    rest 3.0;
    spawn_pattern(boss1_b_patterns[random()]);
    rest 4.0;

    spawn_rods();
    rest 4.0;
    rest 2.0;
    retract_pistons();
    rest 1.0;
    despawn_pattern();
    rest 1.0;

    spawn_pattern(boss1_b_patterns[random()]);
    rest 4.0;
    spawn_rods();
    rest 3.75;
    retract_pistons();
    rest 0.25;

    spawn_rods();
    rest 4.0;
    rest 2.0;
    retract_pistons();
    rest 1.0;
    despawn_pattern();
    rest 1.0;
    
 */

        val list = mutableListOf<Event>()

        val patternPool = patternPools.boss1_b_patterns

        list.music(StoryMusicAssets.STEM_ID_BOSS_1_B2, 8, extraBeatDuration = EXTRA_BEATS_SPACING)

        list
            .retractPistons()
            .rest(1.0f)
            .despawnPattern()
            .rest(3.0f)
            .spawnPattern(patternPool.iter.next())
            .rest(4.0f)

            .spawnRods()
            .rest(4.0f)
            .rest(2.0f)
            .retractPistons()
            .rest(1.0f)
            .despawnPattern()
            .rest(1.0f)

            .spawnPattern(patternPool.iter.next())
            .rest(4.0f)
            .spawnRods()
            .rest(3.75f)
            .retractPistons()
            .rest(0.25f)

            .spawnRods()
            .rest(4.0f)
            .rest(2.0f)
            .retractPistons()
            .rest(1.0f)
            .despawnPattern()
            .rest(1.0f)

        return list
    }
}
