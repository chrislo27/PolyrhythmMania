package polyrhythmmania.storymode.gamemode.boss.scripting

import polyrhythmmania.engine.Event
import polyrhythmmania.storymode.gamemode.boss.pattern.BossPatterns
import polyrhythmmania.storymode.gamemode.boss.scripting.BossScriptPhase1F.VariantFactory
import polyrhythmmania.storymode.music.StoryMusicAssets
import kotlin.random.asKotlinRandom


class BossScriptPhase1F(phase1: BossScriptPhase1, val variantIndex: Int? = null) : AbstractBossScriptPhase1Part(phase1) {

    private fun interface VariantFactory {
        fun create(): AbstractBossScriptPhase1FVariant
    }
    
    override fun getEvents(): List<Event> {
        /*

Script boss1_d_variants[] = {
    boss1_d_var1,
    boss1_d_var2,
    boss1_d_var3
};

Script boss1_d = {
    run boss1_d_variants[random()];
    return;
};

        */

        val list = mutableListOf<Event>()
        
        val variants: List<VariantFactory> = listOf(
            VariantFactory { BossScriptPhase1FVar1(phase1) },
            VariantFactory { BossScriptPhase1FVar2(phase1) },
            VariantFactory { BossScriptPhase1FVar3(phase1) },
        )

        val randomVariantFactory = if (variantIndex != null) variants[variantIndex] else variants.random(this.random.asKotlinRandom())
        list.addAll(randomVariantFactory.create().getEvents())

        return list
    }
}

private abstract class AbstractBossScriptPhase1FVariant(
    phase1: BossScriptPhase1,
    val stemVariant: Int,
) : AbstractBossScriptPhase1Part(phase1)


private class BossScriptPhase1FVar1(phase1: BossScriptPhase1) : AbstractBossScriptPhase1FVariant(phase1, 1) {

    override fun getEvents(): List<Event> {
        /*
    set_music(boss1_f_var1);

    spawn_pattern(boss1_f_var1_pat0, NO_FLIP);
    rest 3.0;
    target_lights(UPSIDE,   "-1-3-5-7--");
    target_lights(DOWNSIDE, "0-----6---");
    change_light_strength(DARK1, 1.0);
    rest 1.0;

    spawn_rods();
    rest 3.0;
    target_lights(UPSIDE,   "-1---5----");
    target_lights(DOWNSIDE, "0-----6---");
    change_light_strength(DARK2, 1.0);
    rest 0.75;
    retract_pistons();
    rest 0.25;

    spawn_rods();
    rest 3.0;
    target_lights(UPSIDE,   "-----5----");
    target_lights(DOWNSIDE, "0---------");
    change_light_strength(DARK3, 1.0);
    rest 0.75;
    retract_pistons();
    rest 0.25;

    spawn_rods();
    rest 3.75;
    target_lights(UPSIDE,   "----------");
    target_lights(DOWNSIDE, "----------");
    change_light_strength(NORMAL, 0.375);
    despawn_pattern();
    rest 0.25;

    spawn_pattern(boss1_f_var1_pat1, NO_FLIP);
    rest 3.0;
    target_lights(UPSIDE,   "-1-3-5-7--");
    target_lights(DOWNSIDE, "0--3--6---");
    change_light_strength(DARK1, 1.0);
    rest 1.0;

    spawn_rods();
    rest 3.0;
    target_lights(UPSIDE,   "-1-3------");
    target_lights(DOWNSIDE, "---3--6---");
    change_light_strength(DARK2, 1.0);
    rest 0.75;
    retract_pistons();
    rest 0.25;

    spawn_rods();
    rest 3.0;
    target_lights(UPSIDE,   "-1--------");
    target_lights(DOWNSIDE, "------6---");
    change_light_strength(DARK3, 1.0);
    rest 0.75;
    retract_pistons();
    rest 0.25;

    spawn_rods();
    rest 3.75;
    target_lights(UPSIDE,   "----------");
    target_lights(DOWNSIDE, "----------");
    change_light_strength(NORMAL, 0.375);
    rest 0.25;
         */
        
        val list = mutableListOf<Event>()
        
        list.music(StoryMusicAssets.STEM_ID_BOSS_1_F, 8, extraBeatDuration = EXTRA_BEATS_SPACING, specificVariant = stemVariant)

        list
            .spawnPattern(BossPatterns.boss1_f_var1_pat0, flipChance = NO_FLIP_CHANCE)
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "-1-3-5-7--")
            .targetLights(SIDE_DOWNSIDE, "0-----6---")
            .changeLightStrength(LightStrength.DARK1, 1.0f)
            .rest(1.0f)
        
            .spawnRods()
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "-1---5----")
            .targetLights(SIDE_DOWNSIDE, "0-----6---")
            .changeLightStrength(LightStrength.DARK2, 1.0f)
            .rest(0.75f)
            .retractPistons()
            .rest(0.25f)
        
            .spawnRods()
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "-----5----")
            .targetLights(SIDE_DOWNSIDE, "0---------")
            .changeLightStrength(LightStrength.DARK3, 1.0f)
            .rest(0.75f)
            .retractPistons()
            .rest(0.25f)
            
            .spawnRods()
            .rest(3.75f)
            .targetLights(SIDE_UPSIDE, "----------")
            .targetLights(SIDE_DOWNSIDE, "----------")
            .changeLightStrength(LightStrength.NORMAL, 0.375f)
            .despawnPattern()
            .rest(0.25f)
        
        list
            .spawnPattern(BossPatterns.boss1_f_var1_pat1, flipChance = NO_FLIP_CHANCE)
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "-1-3-5-7--")
            .targetLights(SIDE_DOWNSIDE, "0--3--6---")
            .changeLightStrength(LightStrength.DARK1, 1.0f)
            .rest(1.0f)

            .spawnRods()
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "-1-3------")
            .targetLights(SIDE_DOWNSIDE, "---3--6---")
            .changeLightStrength(LightStrength.DARK2, 1.0f)
            .rest(0.75f)
            .retractPistons()
            .rest(0.25f)

            .spawnRods()
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "-1--------")
            .targetLights(SIDE_DOWNSIDE, "------6---")
            .changeLightStrength(LightStrength.DARK3, 1.0f)
            .rest(0.75f)
            .retractPistons()
            .rest(0.25f)

            .spawnRods()
            .rest(3.75f)
            .targetLights(SIDE_UPSIDE, "----------")
            .targetLights(SIDE_DOWNSIDE, "----------")
            .changeLightStrength(LightStrength.NORMAL, 0.375f)
            .despawnPattern()
            .rest(0.25f)


        return list
    }
}

private class BossScriptPhase1FVar2(phase1: BossScriptPhase1) : AbstractBossScriptPhase1FVariant(phase1, 2) {

    override fun getEvents(): List<Event> {
        /*
    set_music(boss1_f_var2);

    spawn_pattern(boss1_f_var2_pat0, NO_FLIP);
    rest 3.0;
    target_lights(UPSIDE,   "--2---6---");
    target_lights(DOWNSIDE, "0--3-5----");
    change_light_strength(DARK1, 1.0);
    rest 1.0;

    spawn_rods();
    rest 3.0;
    target_lights(UPSIDE,   "------6---");
    target_lights(DOWNSIDE, "0----5----");
    change_light_strength(DARK2, 1.0);
    rest 0.75;
    retract_pistons();
    rest 0.25;

    spawn_rods();
    rest 3.0;
    target_lights(UPSIDE,   "------6---");
    target_lights(DOWNSIDE, "0---------");
    change_light_strength(DARK3, 1.0);
    rest 0.75;
    retract_pistons();
    rest 0.25;

    spawn_rods();
    rest 3.75;
    target_lights(UPSIDE,   "----------");
    target_lights(DOWNSIDE, "----------");
    change_light_strength(NORMAL, 0.375);
    despawn_pattern();
    rest 0.25;

    spawn_pattern(boss1_f_var2_pat1, NO_FLIP);
    rest 3.0;
    target_lights(UPSIDE,   "--2---6---");
    target_lights(DOWNSIDE, "0--3-5-7--");
    change_light_strength(DARK1, 1.0);
    rest 1.0;

    spawn_rods();
    rest 3.0;
    target_lights(UPSIDE,   "--2-------");
    target_lights(DOWNSIDE, "---3---7--");
    change_light_strength(DARK2, 1.0);
    rest 0.75;
    retract_pistons();
    rest 0.25;

    spawn_rods();
    rest 3.0;
    target_lights(UPSIDE,   "--2-------");
    target_lights(DOWNSIDE, "-------7--");
    change_light_strength(DARK3, 1.0);
    rest 0.75;
    retract_pistons();
    rest 0.25;

    spawn_rods();
    rest 3.75;
    target_lights(UPSIDE,   "----------");
    target_lights(DOWNSIDE, "----------");
    change_light_strength(NORMAL, 0.375);
    rest 0.25;
         */

        val list = mutableListOf<Event>()

        list.music(StoryMusicAssets.STEM_ID_BOSS_1_F, 8, extraBeatDuration = EXTRA_BEATS_SPACING, specificVariant = stemVariant)

        list
            .spawnPattern(BossPatterns.boss1_f_var2_pat0, flipChance = NO_FLIP_CHANCE)
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "--2---6---")
            .targetLights(SIDE_DOWNSIDE, "0--3-5----")
            .changeLightStrength(LightStrength.DARK1, 1.0f)
            .rest(1.0f)

            .spawnRods()
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "------6---")
            .targetLights(SIDE_DOWNSIDE, "0----5----")
            .changeLightStrength(LightStrength.DARK2, 1.0f)
            .rest(0.75f)
            .retractPistons()
            .rest(0.25f)

            .spawnRods()
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "------6---")
            .targetLights(SIDE_DOWNSIDE, "0---------")
            .changeLightStrength(LightStrength.DARK3, 1.0f)
            .rest(0.75f)
            .retractPistons()
            .rest(0.25f)

            .spawnRods()
            .rest(3.75f)
            .targetLights(SIDE_UPSIDE, "----------")
            .targetLights(SIDE_DOWNSIDE, "----------")
            .changeLightStrength(LightStrength.NORMAL, 0.375f)
            .despawnPattern()
            .rest(0.25f)

        list
            .spawnPattern(BossPatterns.boss1_f_var2_pat1, flipChance = NO_FLIP_CHANCE)
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "--2---6---")
            .targetLights(SIDE_DOWNSIDE, "0--3-5-7--")
            .changeLightStrength(LightStrength.DARK1, 1.0f)
            .rest(1.0f)

            .spawnRods()
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "--2-------")
            .targetLights(SIDE_DOWNSIDE, "---3---7--")
            .changeLightStrength(LightStrength.DARK2, 1.0f)
            .rest(0.75f)
            .retractPistons()
            .rest(0.25f)

            .spawnRods()
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "--2-------")
            .targetLights(SIDE_DOWNSIDE, "-------7--")
            .changeLightStrength(LightStrength.DARK3, 1.0f)
            .rest(0.75f)
            .retractPistons()
            .rest(0.25f)

            .spawnRods()
            .rest(3.75f)
            .targetLights(SIDE_UPSIDE, "----------")
            .targetLights(SIDE_DOWNSIDE, "----------")
            .changeLightStrength(LightStrength.NORMAL, 0.375f)
            .despawnPattern()
            .rest(0.25f)


        return list
    }
}

private class BossScriptPhase1FVar3(phase1: BossScriptPhase1) : AbstractBossScriptPhase1FVariant(phase1, 3) {

    override fun getEvents(): List<Event> {
        /*
    set_music(boss1_f_var3);

    spawn_pattern(boss1_f_var3_pat0, NO_FLIP);
    rest 3.0;
    target_lights(UPSIDE,   "0--3--6---");
    target_lights(DOWNSIDE, "0-2-4-6---");
    change_light_strength(DARK1, 1.0);
    rest 1.0;

    spawn_rods();
    rest 3.0;
    target_lights(UPSIDE,   "0--3--6---");
    target_lights(DOWNSIDE, "0-2-4-6---");
    change_light_strength(DARK2, 1.0);
    rest 0.75;
    retract_pistons();
    rest 0.25;

    spawn_rods();
    rest 3.0;
    target_lights(UPSIDE,   "------6---");
    target_lights(DOWNSIDE, "0---------");
    change_light_strength(DARK3, 1.0);
    rest 0.75;
    retract_pistons();
    rest 0.25;

    spawn_rods();
    rest 3.75;
    target_lights(UPSIDE,   "----------");
    target_lights(DOWNSIDE, "----------");
    change_light_strength(NORMAL, 0.375);
    despawn_pattern();
    rest 0.25;

    spawn_pattern(boss1_f_var3_pat1, NO_FLIP);
    rest 3.0;
    target_lights(UPSIDE,   "01-3--6---");
    target_lights(DOWNSIDE, "0-2-4-6---");
    change_light_strength(DARK1, 1.0);
    rest 1.0;

    spawn_rods();
    rest 3.0;
    target_lights(UPSIDE,   "-1-3------");
    target_lights(DOWNSIDE, "--2---6---");
    change_light_strength(DARK2, 1.0);
    rest 0.75;
    retract_pistons();
    rest 0.25;

    spawn_rods();
    rest 3.0;
    target_lights(UPSIDE,   "---3------");
    target_lights(DOWNSIDE, "------6---");
    change_light_strength(DARK3, 1.0);
    rest 0.75;
    retract_pistons();
    rest 0.25;

    spawn_rods();
    rest 3.75;
    target_lights(UPSIDE,   "----------");
    target_lights(DOWNSIDE, "----------");
    change_light_strength(NORMAL, 0.375);
    rest 0.25;
         */

        val list = mutableListOf<Event>()

        list.music(StoryMusicAssets.STEM_ID_BOSS_1_F, 8, extraBeatDuration = EXTRA_BEATS_SPACING, specificVariant = stemVariant)
        
        list
            .spawnPattern(BossPatterns.boss1_f_var3_pat0, flipChance = NO_FLIP_CHANCE)
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "0--3--6---")
            .targetLights(SIDE_DOWNSIDE, "0-2-4-6---")
            .changeLightStrength(LightStrength.DARK1, 1.0f)
            .rest(1.0f)

            .spawnRods()
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "0-----6---")
            .targetLights(SIDE_DOWNSIDE, "0-----6---")
            .changeLightStrength(LightStrength.DARK2, 1.0f)
            .rest(0.75f)
            .retractPistons()
            .rest(0.25f)

            .spawnRods()
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "------6---")
            .targetLights(SIDE_DOWNSIDE, "0---------")
            .changeLightStrength(LightStrength.DARK3, 1.0f)
            .rest(0.75f)
            .retractPistons()
            .rest(0.25f)

            .spawnRods()
            .rest(3.75f)
            .targetLights(SIDE_UPSIDE, "----------")
            .targetLights(SIDE_DOWNSIDE, "----------")
            .changeLightStrength(LightStrength.NORMAL, 0.375f)
            .despawnPattern()
            .rest(0.25f)
        
        list
            .spawnPattern(BossPatterns.boss1_f_var3_pat1, flipChance = NO_FLIP_CHANCE)
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "01-3--6---")
            .targetLights(SIDE_DOWNSIDE, "0-2-4-6---")
            .changeLightStrength(LightStrength.DARK1, 1.0f)
            .rest(1.0f)

            .spawnRods()
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "-1-3------")
            .targetLights(SIDE_DOWNSIDE, "--2---6---")
            .changeLightStrength(LightStrength.DARK2, 1.0f)
            .rest(0.75f)
            .retractPistons()
            .rest(0.25f)

            .spawnRods()
            .rest(3.0f)
            .targetLights(SIDE_UPSIDE, "---3------")
            .targetLights(SIDE_DOWNSIDE, "------6---")
            .changeLightStrength(LightStrength.DARK3, 1.0f)
            .rest(0.75f)
            .retractPistons()
            .rest(0.25f)

            .spawnRods()
            .rest(3.75f)
            .targetLights(SIDE_UPSIDE, "----------")
            .targetLights(SIDE_DOWNSIDE, "----------")
            .changeLightStrength(LightStrength.NORMAL, 0.375f)
            .despawnPattern()
            .rest(0.25f)


        return list
    }
}
