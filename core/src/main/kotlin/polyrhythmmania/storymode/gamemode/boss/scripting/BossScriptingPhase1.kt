package polyrhythmmania.storymode.gamemode.boss.scripting

import polyrhythmmania.engine.Event
import polyrhythmmania.storymode.gamemode.boss.StoryBossGameMode
import polyrhythmmania.storymode.music.StemID
import polyrhythmmania.storymode.music.StoryMusicAssets


class BossScriptPhase1(gamemode: StoryBossGameMode, script: Script) : BossScriptFunction(gamemode, script) {

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

    run boss2_main;
         */

        return mutableListOf<Event>()
            .addFunctionAsEvent(BossScriptPhase1A1(this))
            .addFunctionAsEvent(LoopingSegment())
    }

    private inner class LoopingSegment : BossScriptFunction(gamemode, script) {

        override fun getEvents(): List<Event> {
            /*
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
             */
            
            class PlaceholderMusicStem(val stemID: StemID) : AbstractBossScriptPhase1Part(this@BossScriptPhase1) {

                override fun getEvents(): List<Event> {
                    return mutableListOf<Event>()
                        .music(stemID, 8 + EXTRA_MEASURES_SPACING)
                        .rest(32.0f)
                }
            }

            val stemList = listOf(
                BossScriptPhase1B2(this@BossScriptPhase1),
                PlaceholderMusicStem(StoryMusicAssets.STEM_ID_BOSS_1_C),
                PlaceholderMusicStem(StoryMusicAssets.STEM_ID_BOSS_1_B1),
                PlaceholderMusicStem(StoryMusicAssets.STEM_ID_BOSS_1_B2),
                PlaceholderMusicStem(StoryMusicAssets.STEM_ID_BOSS_1_D),
                PlaceholderMusicStem(StoryMusicAssets.STEM_ID_BOSS_1_E1),
                PlaceholderMusicStem(StoryMusicAssets.STEM_ID_BOSS_1_E2),
                PlaceholderMusicStem(StoryMusicAssets.STEM_ID_BOSS_1_F),
                PlaceholderMusicStem(StoryMusicAssets.STEM_ID_BOSS_1_A2),
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

class BossScriptPhase1A1(phase1: BossScriptPhase1) : AbstractBossScriptPhase1Part(phase1) {

    override fun getEvents(): List<Event> {
        /*
    set_music(mus_story_boss1_a1);

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
    return;
         */

        val list = mutableListOf<Event>()

        list.music(StoryMusicAssets.STEM_ID_BOSS_1_A1, 8 + EXTRA_MEASURES_SPACING)

        repeat(2) {
            var pattern = patternPools.boss1_a1_patterns.iter.next()
            if (pattern.flippable && gamemode.random.nextBoolean()) {
                pattern = pattern.flip()
            }

            list
                .rest(1.0f)
                .despawnPattern()
                .rest(3.0f)
                .spawnPattern(pattern)
                .rest(4.0f)

                .spawnRods(pattern)
                .rest(3.75f)
                .retractPistons()
                .rest(0.25f)
                .spawnRods(pattern)
                .rest(4.0f)

                .retractPistons()
        }

        return list
    }
}

class BossScriptPhase1A2(phase1: BossScriptPhase1) : AbstractBossScriptPhase1Part(phase1) {

    override fun getEvents(): List<Event> {
        /*
    set_music(mus_story_boss1_a2);

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
    return;
         */

        val list = mutableListOf<Event>()

        list.music(StoryMusicAssets.STEM_ID_BOSS_1_A2, 8 + EXTRA_MEASURES_SPACING)

        repeat(2) {
            var pattern = patternPools.boss1_a2_patterns.iter.next()
            if (pattern.flippable && gamemode.random.nextBoolean()) {
                pattern = pattern.flip()
            }

            list
                .rest(1.0f)
                .despawnPattern()
                .rest(3.0f)
                .spawnPattern(pattern)
                .rest(4.0f)

                .spawnRods(pattern)
                .rest(3.75f)
                .retractPistons()
                .rest(0.25f)
                .spawnRods(pattern)
                .rest(4.0f)

                .retractPistons()
        }

        return list
    }
}


class BossScriptPhase1B2(phase1: BossScriptPhase1) : AbstractBossScriptPhase1Part(phase1) {

    override fun getEvents(): List<Event> {
        /*
Script boss1_b2 = {
    set_music(mus_story_boss1_b2);

    rest 1.0;
    despawn_pattern();
    rest 3.0;
    spawn_pattern(boss1_b2_patterns[random()]);
    rest 4.0;

    spawn_rods();
    rest 4.0;
    rest 2.0;
    retract_pistons();
    rest 1.0;
    despawn_pattern();
    rest 1.0;

    spawn_pattern(boss1_b2_patterns[random()]);
    rest 4.0;
    spawn_rods();
    rest 3.75;
    despawn_pattern();
    rest 0.25;

    spawn_pattern(boss1_b2_patterns[random()]);
    rest 4.0;
    spawn_rods();
    rest 4.0;

    retract_pistons();
    return;
};
 */

        val list = mutableListOf<Event>()

        val patternPool = patternPools.boss1_b2_patterns
        val pattern1 = patternPool.iter.next()
        val pattern2 = patternPool.iter.next()
        val pattern3 = patternPool.iter.next()

        list.music(StoryMusicAssets.STEM_ID_BOSS_1_B2, 8 + EXTRA_MEASURES_SPACING)
        
        list
            .rest(1.0f)
            .despawnPattern()
            .rest(3.0f)
            .spawnPattern(pattern1)
            .rest(4.0f)
            
            .spawnRods(pattern1)
            .rest(4.0f)
            .rest(2.0f)
            .retractPistons()
            .rest(1.0f)
            .despawnPattern()
            .rest(1.0f)
        
            .spawnPattern(pattern2)
            .rest(4.0f)
            .spawnRods(pattern2)
            .rest(3.75f)
            .despawnPattern()
            .rest(0.25f)
            
            .spawnPattern(pattern3)
            .rest(4.0f)
            .spawnRods(pattern3)
            .rest(4.0f)
        
            .retractPistons()

        return list
    }
}
