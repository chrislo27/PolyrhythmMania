package polyrhythmmania.storymode.gamemode.boss.scripting

import polyrhythmmania.engine.Event
import polyrhythmmania.storymode.gamemode.boss.EntityRodPRStoryBoss
import polyrhythmmania.storymode.gamemode.boss.StoryBossGameMode
import polyrhythmmania.storymode.gamemode.boss.pattern.BossPatterns
import polyrhythmmania.storymode.music.StoryMusicAssets
import polyrhythmmania.world.tileset.TransitionCurve
import kotlin.random.asKotlinRandom


class BossScriptPhase1E(phase1: BossScriptPhase1) : AbstractBossScriptPhase1Part(phase1) {
    
    override fun getEvents(): List<Event> {
        /*

Script boss1_e = {
    set_music(boss1_e1);
    run boss1_e1_scripts1[random()];
    run boss1_e1_scripts2[random()];

    set_music(boss1_e2);
    run boss1_e2_scripts1[random()];
    run boss1_e2_scripts2[random()];
    run boss1_e2_scripts3[random()];

    return;
};

        */

        val list = mutableListOf<Event>()
        
        list.addFunctionAsEvent(BossScriptPhase1E1(phase1))
        list.addFunctionAsEvent(BossScriptPhase1E2(phase1))

        return list
    }
}

abstract class AbstractBossScriptPhase1EVariant(
    phase1: BossScriptPhase1,
) : AbstractBossScriptPhase1Part(phase1) {

    protected abstract val scriptFactories: List<List<() -> ScriptFunction>>

}

class BossScriptPhase1E1(phase1: BossScriptPhase1) : AbstractBossScriptPhase1EVariant(phase1) {

    override val scriptFactories: List<List<() -> ScriptFunction>> = listOf(
        listOf(
            //    boss1_e_stomp1,
            //    boss1_e_toss1,
            { BossScriptPhase1EStomp1(gamemode, script) },
            { BossScriptPhase1EToss1(gamemode, script) },
        ),
        listOf(
            //    boss1_e_spaceball1,
            //    boss1_e_stomp1,
            //    boss1_e_toss1,
            //    boss1_e_badminton1,
            { BossScriptPhase1ESpaceball1(gamemode, script) },
            { BossScriptPhase1EStomp1(gamemode, script) },
            { BossScriptPhase1EToss1(gamemode, script) },
            { BossScriptPhase1EBadminton1(phase1) },
        )
    )
    
    override fun getEvents(): List<Event> {
        /*
    set_music(boss1_e1);
    run boss1_e1_scripts1[random()];
    run boss1_e1_scripts2[random()];
         */
        
        val ktRandom = this.random.asKotlinRandom()
        val list = mutableListOf<Event>()
        
        list.music(StoryMusicAssets.STEM_ID_BOSS_1_E1, 8, extraBeatDuration = EXTRA_BEATS_SPACING)
        
        (0..1).forEach { stage ->
            list.addAll(scriptFactories[stage].random(ktRandom).invoke().getEvents())
        }

        return list
    }
}

class BossScriptPhase1E2(phase1: BossScriptPhase1) : AbstractBossScriptPhase1EVariant(phase1) {

    override val scriptFactories: List<List<() -> ScriptFunction>> = listOf(
        listOf(
            //    boss1_e_bunny1,
            { BossScriptPhase1EBunny1(gamemode, script) },
        ),
        listOf(
            //    boss1_e_samurai1,
            //    boss1_e_samurai2,
            { BossScriptPhase1ESamurai1(gamemode, script) },
            { BossScriptPhase1ESamurai2(gamemode, script) },
        ),
        listOf(
            //    boss1_e_spaceball1,
            //    boss1_e_spaceball2,
            //    boss1_e_tap1,
            //    boss1_e_toss1,
            //    boss1_e_badminton1,
            { BossScriptPhase1ESpaceball1(gamemode, script) },
            { BossScriptPhase1ESpaceball2(gamemode, script) },
            { BossScriptPhase1ETap1(gamemode, script) },
            { BossScriptPhase1EToss1(gamemode, script) },
            { BossScriptPhase1EBadminton1(phase1) },
        )
    )
    
    override fun getEvents(): List<Event> {
        /*
    set_music(boss1_e2);
    run boss1_e2_scripts1[random()];
    run boss1_e2_scripts2[random()];
    run boss1_e2_scripts3[random()];
         */

        val ktRandom = this.random.asKotlinRandom()
        val list = mutableListOf<Event>()
        
        list.music(StoryMusicAssets.STEM_ID_BOSS_1_E2, 8, extraBeatDuration = EXTRA_BEATS_SPACING)

        (0..2).forEach { stage ->
            list.addAll(scriptFactories[stage].random(ktRandom).invoke().getEvents())
        }

        return list
    }
}

private class BossScriptPhase1ESamurai1(
    gamemode: StoryBossGameMode,
    script: Script
) : BossScriptFunction(gamemode, script) {

    override fun getEvents(): List<Event> {
        /*
    spawn_pattern(boss1_e_samurai_pat0, NO_FLIP);
    rest 2.0;
    spawn_rods();
    rest 1.0;
    retract_pistons();
    rest 0.5;
    despawn_pattern();
    rest 0.5;

    spawn_pattern(boss1_e_samurai_pat0, FLIP);
    rest 2.0;
    spawn_rods();
    rest 1.0;
    retract_pistons();
    rest 0.5;
    despawn_pattern();
    rest 0.5;
 */
        
        val list = mutableListOf<Event>()

        list
            .spawnPattern(BossPatterns.boss1_e_samurai_pat0, flipChance = NO_FLIP_CHANCE)
            .rest(2.0f)
            .spawnRods()
            .rest(1.0f)
            .retractPistons()
            .rest(0.5f)
            .despawnPattern()
            .rest(0.5f)
        
        list
            .spawnPattern(BossPatterns.boss1_e_samurai_pat0, flipChance = ALWAYS_FLIP_CHANCE)
            .rest(2.0f)
            .spawnRods()
            .rest(1.0f)
            .retractPistons()
            .rest(0.5f)
            .despawnPattern()
            .rest(0.5f)

        return list
    }
}

private class BossScriptPhase1ESamurai2(
    gamemode: StoryBossGameMode,
    script: Script
) : BossScriptFunction(gamemode, script) {

    override fun getEvents(): List<Event> {
        /*
    spawn_pattern(boss1_e_samurai_pat1);
    rest 4.0;
    spawn_rods();
    rest 3.75;
    despawn_pattern();
    rest 0.25;
 */

        val list = mutableListOf<Event>()

        list
            .spawnPattern(BossPatterns.boss1_e_samurai_pat1)
            .rest(4.0f)
            .spawnRods()
            .rest(3.75f)
            .despawnPattern()
            .rest(0.25f)

        return list
    }
}

private class BossScriptPhase1EStomp1(
    gamemode: StoryBossGameMode,
    script: Script
) : BossScriptFunction(gamemode, script) {

    override fun getEvents(): List<Event> {
        /*
    spawn_pattern(boss1_e_stomp_pat0, NO_FLIP);
    rest 4.0;
    spawn_rod(DOWNSIDE);
    rest 3.5;
    retract_pistons();
    rest 0.5;

    spawn_rod(UPSIDE);
    spawn_rod(DOWNSIDE);
    rest 3.5;
    retract_pistons(DOWNSIDE);
    rest 0.5;
    
    spawn_rod(DOWNSIDE);
    rest 3.5;
    despawn_pattern();
    rest 0.5;
 */

        val list = mutableListOf<Event>()

        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnPattern(BossPatterns.boss1_e_stomp_pat0, flipChance = NO_FLIP_CHANCE)
                .rest(4.0f)
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 2)
                .rest(3.5f)
                .retractPistons()
                .rest(0.5f)
        }

        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnOneRod(SIDE_UPSIDE, damageVar, 1)
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 1)
                .rest(3.5f)
                .retractPistonsDownside()
                .rest(0.5f)
        }

        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 2)
                .rest(3.5f)
                .despawnPattern()
                .rest(0.5f)
        }

        return list
    }
}

private class BossScriptPhase1EBunny1(
    gamemode: StoryBossGameMode,
    script: Script
) : BossScriptFunction(gamemode, script) {

    override fun getEvents(): List<Event> {
        /*
    spawn_pattern(boss1_e_bunny_pat0);
    rest 3.0;
    spawn_rods();
    rest 1.0;
    rest 3.0;
    retract_pistons();
    rest 0.5;
    despawn_pattern();
    rest 0.5;
 */

        val list = mutableListOf<Event>()

        list
            .spawnPattern(BossPatterns.boss1_e_bunny_pat0)
            .rest(3.0f)
            .spawnRods()
            .rest(1.0f)
            .rest(3.0f)
            .retractPistons()
            .rest(0.5f)
            .despawnPattern()
            .rest(0.5f)

        return list
    }
}

private class BossScriptPhase1ETap1(
    gamemode: StoryBossGameMode,
    script: Script
) : BossScriptFunction(gamemode, script) {

    override fun getEvents(): List<Event> {
        /*
    spawn_pattern(boss1_e_tap_pat0, NO_FLIP);
    rest 4.0;
    spawn_rod(UPSIDE);
    spawn_rod(DOWNSIDE);
    rest 3.5;
    retract_pistons(DOWNSIDE);
    rest 0.5;

    retract_pistons(UPSIDE);
    spawn_rod(DOWNSIDE);
    rest 0.5;
    spawn_rod(UPSIDE);
    rest 2.5;
    retract_pistons(UPSIDE);
    
    spawn_rod(UPSIDE);
    rest 0.5;
    retract_pistons(DOWNSIDE);
    rest 0.5;
    spawn_rod(DOWNSIDE);
    rest 3.5;
    despawn_pattern();
    rest 0.5;
 */

        val list = mutableListOf<Event>()

        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnPattern(BossPatterns.boss1_e_tap_pat0, flipChance = NO_FLIP_CHANCE)
                .rest(4.0f)
                .spawnOneRod(SIDE_UPSIDE, damageVar, 1)
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 1)
                .rest(3.5f)
                .retractPistonsDownside()
                .rest(0.5f)
        }

        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .retractPistonsUpside()
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 1)
                .rest(0.5f)
                .spawnOneRod(SIDE_UPSIDE, damageVar, 1)
                .rest(2.5f)
                .retractPistonsUpside()
        }

        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnOneRod(SIDE_UPSIDE, damageVar, 1)
                .rest(0.5f)
                .retractPistonsDownside()
                .rest(0.5f)
        }

        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 1)
                .rest(3.5f)
                .despawnPattern()
                .rest(0.5f)
        }

        return list
    }
}

private class BossScriptPhase1EToss1(
    gamemode: StoryBossGameMode,
    script: Script
) : BossScriptFunction(gamemode, script) {

    override fun getEvents(): List<Event> {
        /*
    spawn_pattern(boss1_e_toss_pat0, NO_FLIP);
    rest 4.0;
    spawn_rod(UPSIDE);
    rest 4.0;

    spawn_pattern(boss1_e_toss_pat1, NO_FLIP);
    retract_pistons(UPSIDE);
    spawn_rod(UPSIDE);
    rest 4.0;
    retract_pistons(UPSIDE);
    
    spawn_rod(UPSIDE);
    spawn_rod(DOWNSIDE);
    rest 3.75;
    despawn_pattern();
    rest 0.25;
 */

        val list = mutableListOf<Event>()
        
        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnPattern(BossPatterns.boss1_e_toss_pat0, flipChance = NO_FLIP_CHANCE)
                .rest(4.0f)
                .spawnOneRod(SIDE_UPSIDE, damageVar, 1)
                .rest(4.0f)
        }
        
        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnPattern(BossPatterns.boss1_e_toss_pat1, flipChance = NO_FLIP_CHANCE)
                .retractPistonsUpside()
                .spawnOneRod(SIDE_UPSIDE, damageVar, 1)
                .rest(4.0f)
                .retractPistonsUpside()
        }
        
        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnOneRod(SIDE_UPSIDE, damageVar, 1)
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 1)
                .rest(3.75f)
                .despawnPattern()
                .rest(0.25f)
        }

        return list
    }
}

private class BossScriptPhase1ESpaceball1(
    gamemode: StoryBossGameMode,
    script: Script
) : BossScriptFunction(gamemode, script) {

    override fun getEvents(): List<Event> {
        /*
    spawn_pattern(boss1_e_spaceball_pat0, NO_FLIP);
    rest 4.0;
    spawn_rods();
    rest 3.0;
    retract_pistons();
    rest 1.0;

    despawn_pattern(UPSIDE);
    spawn_rod(DOWNSIDE);
    selective_spawn(boss1_e_spaceball_ssp_start, NO_FLIP);
    rest 0.5;
    selective_spawn(boss1_e_spaceball_ssp_flicker0, NO_FLIP);
    rest 0.5;
    selective_spawn(boss1_e_spaceball_ssp_flicker1, NO_FLIP);
    rest 0.5;
    selective_spawn(boss1_e_spaceball_ssp_flicker0, NO_FLIP);
    rest 0.5;
    selective_spawn(boss1_e_spaceball_ssp_flicker1, NO_FLIP);
    rest 1.0;
    selective_spawn(boss1_e_spaceball_ssp_end, NO_FLIP);
    rest 1.0;
    rest 2.0;
    retract_pistons();
    rest 1.0;
    despawn_pattern();
    rest 1.0;
 */

        val list = mutableListOf<Event>()

        list
            .spawnPattern(BossPatterns.boss1_e_spaceball_pat0, flipChance = NO_FLIP_CHANCE)
            .rest(4.0f)
            .spawnRods()
            .rest(3.0f)
            .retractPistons()
            .rest(1.0f)


        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .despawnPatternUpside()
                .spawnPattern(BossPatterns.boss1_e_spaceball_ssp_start, flipChance = NO_FLIP_CHANCE)
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 2)
                .rest(0.5f)
                .spawnPattern(BossPatterns.boss1_e_spaceball_ssp_flicker0, flipChance = NO_FLIP_CHANCE)
                .rest(0.5f)
                .spawnPattern(BossPatterns.boss1_e_spaceball_ssp_flicker1, flipChance = NO_FLIP_CHANCE)
                .rest(0.5f)
                .spawnPattern(BossPatterns.boss1_e_spaceball_ssp_flicker0, flipChance = NO_FLIP_CHANCE)
                .rest(0.5f)
                .spawnPattern(BossPatterns.boss1_e_spaceball_ssp_flicker1, flipChance = NO_FLIP_CHANCE)
                .rest(1.0f)
                .spawnPattern(BossPatterns.boss1_e_spaceball_ssp_end, flipChance = NO_FLIP_CHANCE)
                .rest(1.0f)
                .rest(2.0f)
                .retractPistons()
                .rest(1.0f)
                .despawnPattern()
                .rest(1.0f)
        }

        return list
    }
}

private class BossScriptPhase1ESpaceball2(
    gamemode: StoryBossGameMode,
    script: Script
) : BossScriptFunction(gamemode, script) {

    override fun getEvents(): List<Event> {
        /*
    spawn_pattern(boss1_e_spaceball_pat0, FLIP);
    rest 4.0;
    spawn_rods();
    rest 3.0;
    retract_pistons();
    rest 1.0;

    despawn_pattern(DOWNSIDE);
    spawn_rod(UPSIDE);
    selective_spawn(boss1_e_spaceball_ssp_start, FLIP);
    rest 0.5;
    selective_spawn(boss1_e_spaceball_ssp_flicker0, FLIP);
    rest 0.5;
    selective_spawn(boss1_e_spaceball_ssp_flicker1, FLIP);
    rest 0.5;
    selective_spawn(boss1_e_spaceball_ssp_flicker0, FLIP);
    rest 0.5;
    selective_spawn(boss1_e_spaceball_ssp_flicker1, FLIP);
    rest 1.0;
    selective_spawn(boss1_e_spaceball_ssp_end, FLIP);
    rest 1.0;
    rest 2.0;
    retract_pistons();
    rest 1.0;
    despawn_pattern();
    rest 1.0;
 */

        val list = mutableListOf<Event>()
        
        list
            .spawnPattern(BossPatterns.boss1_e_spaceball_pat0, flipChance = ALWAYS_FLIP_CHANCE)
            .rest(4.0f)
            .spawnRods()
            .rest(3.0f)
            .retractPistons()
            .rest(1.0f)


        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .despawnPatternDownside()
                .spawnPattern(BossPatterns.boss1_e_spaceball_ssp_start, flipChance = ALWAYS_FLIP_CHANCE)
                .spawnOneRod(SIDE_UPSIDE, damageVar, 2)
                .rest(0.5f)
                .spawnPattern(BossPatterns.boss1_e_spaceball_ssp_flicker0, flipChance = ALWAYS_FLIP_CHANCE)
                .rest(0.5f)
                .spawnPattern(BossPatterns.boss1_e_spaceball_ssp_flicker1, flipChance = ALWAYS_FLIP_CHANCE)
                .rest(0.5f)
                .spawnPattern(BossPatterns.boss1_e_spaceball_ssp_flicker0, flipChance = ALWAYS_FLIP_CHANCE)
                .rest(0.5f)
                .spawnPattern(BossPatterns.boss1_e_spaceball_ssp_flicker1, flipChance = ALWAYS_FLIP_CHANCE)
                .rest(1.0f)
                .spawnPattern(BossPatterns.boss1_e_spaceball_ssp_end, flipChance = ALWAYS_FLIP_CHANCE)
                .rest(1.0f)
                .rest(2.0f)
                .retractPistons()
                .rest(1.0f)
                .despawnPattern()
                .rest(1.0f)
        }

        return list
    }
}

private class BossScriptPhase1EBadminton1(
    phase1: BossScriptPhase1
) : AbstractBossScriptPhase1Part(phase1) {
    
    companion object {
        val LIGHT_NORMAL: LightStrength = LightStrength(1.0f, 0.0f)
        val LIGHT_DARK: LightStrength = LightStrength(0.25f, 1.0f)
    }

    override fun getEvents(): List<Event> {
        /*
    spawn_pattern(boss1_e_badminton_pat0, NO_FLIP);
    rest 4.0;
    spawn_rods();
    rest 3.5;
    despawn_pattern(DOWNSIDE);
    rest 0.5;

    retract_pistons(UPSIDE);
    spawn_rod(UPSIDE, 0.5);
    selective_spawn(boss1_e_badminton_ssp_start, NO_FLIP);
    target_lights(UPSIDE,   "----------");
    target_lights(DOWNSIDE, "------6---");
    change_light_strength(boss1_e_badminton_light_strength_dark, 1.0, SLOW_TO_FAST);
    rest 1.0;
    selective_spawn(boss1_e_badminton_ssp_cue, NO_FLIP);
    rest 0.5;
    spawn_rod(DOWNSIDE, 0.25);
    rest 0.5;
    selective_spawn(boss1_e_badminton_ssp_end, NO_FLIP);
    rest 1.0;
    change_light_strength(boss1_e_badminton_light_strength_normal, 1.0, POWER_5_OUT);
    rest 1.0;
    rest 2.0;
    retract_pistons();
    rest 1.0;
    despawn_pattern();
    rest 1.0;
 */

        val list = mutableListOf<Event>()
        
        list
            .spawnPattern(BossPatterns.boss1_e_badminton_pat0, flipChance = NO_FLIP_CHANCE)
            .rest(4.0f)
            .spawnRods()
            .rest(3.5f)
            .despawnPatternDownside()
            .rest(0.5f)


        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .retractPistonsUpside()
                .spawnOneRod(SIDE_UPSIDE, damageVar, 1, beatsPerBlock = 0.5f)
                .spawnPattern(BossPatterns.boss1_e_badminton_ssp_start, flipChance = NO_FLIP_CHANCE)
                .targetLights(SIDE_UPSIDE, "----------")
                .targetLights(SIDE_DOWNSIDE, "------6---")
                .changeLightStrength(LIGHT_DARK, 1.0f, TransitionCurve.SLOW_FAST)
                .rest(1.0f)
                .spawnPattern(BossPatterns.boss1_e_badminton_ssp_cue, flipChance = NO_FLIP_CHANCE)
                .rest(0.5f)
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 1, beatsPerBlock = 0.25f)
                .rest(0.5f)
                .spawnPattern(BossPatterns.boss1_e_badminton_ssp_end, flipChance = NO_FLIP_CHANCE)
                .rest(1.0f)
                .changeLightStrength(LIGHT_NORMAL, 1.0f, TransitionCurve.POW5_OUT)
                .rest(1.0f)
                .rest(2.0f)
                .retractPistons()
                .rest(1.0f)
                .despawnPattern()
                .rest(1.0f)
        }

        return list
    }
}
