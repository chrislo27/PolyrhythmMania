package polyrhythmmania.storymode.gamemode.boss.scripting

import polyrhythmmania.engine.Event
import polyrhythmmania.storymode.gamemode.boss.EntityRodPRStoryBoss
import polyrhythmmania.storymode.gamemode.boss.scripting.BossScriptPhase1D.VariantFactory
import polyrhythmmania.storymode.music.StoryMusicAssets
import kotlin.random.asKotlinRandom


class BossScriptPhase1D(phase1: BossScriptPhase1, val variantIndex: Int? = null) : AbstractBossScriptPhase1Part(phase1) {

    private fun interface VariantFactory {
        fun create(): AbstractBossScriptPhase1DVariant
    }
    
    override fun getEvents(): List<Event> {
        /*

Script boss1_c_variants[] = {
    boss1_c_var1,
    boss1_c_var2,
    boss1_c_var3
};

Script boss1_c = {
    run boss1_c_variants[random()];
    return;
};

        */

        val list = mutableListOf<Event>()
        
        val variants: List<VariantFactory> = listOf(
            VariantFactory { BossScriptPhase1DVar1(phase1) },
            VariantFactory { BossScriptPhase1DVar2(phase1) },
            VariantFactory { BossScriptPhase1DVar3(phase1) },
        )

        val randomVariantFactory = if (variantIndex != null) variants[variantIndex] else variants.random(this.random.asKotlinRandom())
        list.addAll(randomVariantFactory.create().getEvents())

        return list
    }
}

private abstract class AbstractBossScriptPhase1DVariant(
    phase1: BossScriptPhase1,
    val stemVariant: Int,
) : AbstractBossScriptPhase1Part(phase1)

private class BossScriptPhase1DVar1(phase1: BossScriptPhase1) : AbstractBossScriptPhase1DVariant(phase1, 1) {

    override fun getEvents(): List<Event> {
        /*
    set_music(boss1_d_var1);

    spawn_pattern(boss1_d_var1_patterns[random()], NO_FLIP);
    rest 4.0;
    rest 0.125;
    spawn_rod(UPSIDE, 1.5);
    spawn_rod(DOWNSIDE, 1.5);
    rest 0.875;
    rest 3.0;
    despawn_pattern();

    spawn_pattern(boss1_d_patterns[random()]);
    rest 4.0;
    spawn_rods();
    rest 3.75;
    despawn_pattern();
    rest 0.25;

    spawn_pattern(boss1_d_var1_patterns[random()]);
    rest 4.0;
    rest 0.125;
    spawn_rod(UPSIDE, 1.5);
    spawn_rod(DOWNSIDE, 1.5);
    rest 0.875;
    rest 3.0;
    despawn_pattern();

    spawn_pattern(boss1_d_patterns[random()]);
    rest 4.0;
    spawn_rods();
    rest 3.75;
    despawn_pattern();
    rest 0.25;
         */
        
        val list = mutableListOf<Event>()

        val mainPatternPool = patternPools.boss1_d_patterns
        val variantPatternPool = patternPools.boss1_d_var1_patterns
        
        list.music(StoryMusicAssets.STEM_ID_BOSS_1_D, 8, extraBeatDuration = EXTRA_BEATS_SPACING, specificVariant = stemVariant)

        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnPattern(variantPatternPool.iter.next(), flipChance = NO_FLIP_CHANCE)
                .rest(4.0f)
                .rest(0.125f)
                .spawnOneRod(SIDE_UPSIDE, damageVar, 1, speedMultiplier = 1.5f)
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 1, speedMultiplier = 1.5f)
                .rest(0.875f)
                .rest(3.0f)
                .despawnPattern()
        }
        
        run {
            list
                .spawnPattern(mainPatternPool.iter.next())
                .rest(4.0f)
                .spawnRods()
                .rest(3.75f)
                .despawnPattern()
                .rest(0.25f)
        }
        
        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnPattern(variantPatternPool.iter.next())
                .rest(4.0f)
                .rest(0.125f)
                .spawnOneRod(SIDE_UPSIDE, damageVar, 1, speedMultiplier = 1.5f)
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 1, speedMultiplier = 1.5f)
                .rest(0.875f)
                .rest(3.0f)
                .despawnPattern()
        }
        
        run {
            list
                .spawnPattern(mainPatternPool.iter.next())
                .rest(4.0f)
                .spawnRods()
                .rest(3.75f)
                .despawnPattern()
                .rest(0.25f)
        }

        return list
    }
}

private class BossScriptPhase1DVar2(phase1: BossScriptPhase1) : AbstractBossScriptPhase1DVariant(phase1, 2) {

    override fun getEvents(): List<Event> {
        /*
    set_music(boss1_d_var2);

    spawn_pattern(boss1_d_var2_patterns[random()], NO_FLIP);
    rest 4.0;
    spawn_rod(UPSIDE, 1.5);
    spawn_rod(DOWNSIDE, 1.5);
    rest 4.0;
    despawn_pattern();

    spawn_pattern(boss1_d_patterns[random()]);
    rest 4.0;
    spawn_rods();
    rest 3.75;
    despawn_pattern();
    rest 0.25;

    spawn_pattern(boss1_d_var2_patterns[random()]);
    rest 4.0;
    spawn_rod(UPSIDE, 1.5);
    spawn_rod(DOWNSIDE, 1.5);
    rest 4.0;
    despawn_pattern();

    spawn_pattern(boss1_d_patterns[random()]);
    rest 4.0;
    spawn_rods();
    rest 3.75;
    despawn_pattern();
    rest 0.25;
         */

        val list = mutableListOf<Event>()

        val mainPatternPool = patternPools.boss1_d_patterns
        val variantPatternPool = patternPools.boss1_d_var2_patterns

        list.music(StoryMusicAssets.STEM_ID_BOSS_1_D, 8, extraBeatDuration = EXTRA_BEATS_SPACING, specificVariant = stemVariant)

        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnPattern(variantPatternPool.iter.next(), flipChance = NO_FLIP_CHANCE)
                .rest(4.0f)
                .spawnOneRod(SIDE_UPSIDE, damageVar, 1, speedMultiplier = 1.5f)
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 1, speedMultiplier = 1.5f)
                .rest(4.0f)
                .despawnPattern()
        }

        run {
            list
                .spawnPattern(mainPatternPool.iter.next())
                .rest(4.0f)
                .spawnRods()
                .rest(3.75f)
                .despawnPattern()
                .rest(0.25f)
        }

        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnPattern(variantPatternPool.iter.next())
                .rest(4.0f)
                .spawnOneRod(SIDE_UPSIDE, damageVar, 1, speedMultiplier = 1.5f)
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 1, speedMultiplier = 1.5f)
                .rest(4.0f)
                .despawnPattern()
        }

        run {
            list
                .spawnPattern(mainPatternPool.iter.next())
                .rest(4.0f)
                .spawnRods()
                .rest(3.75f)
                .despawnPattern()
                .rest(0.25f)
        }

        return list
    }
}

private class BossScriptPhase1DVar3(phase1: BossScriptPhase1) : AbstractBossScriptPhase1DVariant(phase1, 3) {

    override fun getEvents(): List<Event> {
        /*
    set_music(boss1_d_var3);

    spawn_pattern(boss1_d_var3_patterns[random()], NO_FLIP);
    rest 4.0;
    spawn_rod(DOWNSIDE, 1.0);
    rest 0.125;
    spawn_rod(UPSIDE, 1.5);
    rest 0.875;
    rest 3.0;
    despawn_pattern();

    spawn_pattern(boss1_d_patterns[random()]);
    rest 4.0;
    spawn_rods();
    rest 3.75;
    despawn_pattern();
    rest 0.25;

    spawn_pattern(boss1_d_var3_patterns[random()], FLIP);
    rest 4.0;
    spawn_rod(UPSIDE, 1.0);
    rest 0.125;
    spawn_rod(DOWNSIDE, 1.5);
    rest 0.875;
    rest 3.0;
    despawn_pattern();

    spawn_pattern(boss1_d_patterns[random()]);
    rest 4.0;
    spawn_rods();
    rest 3.75;
    despawn_pattern();
    rest 0.25;
         */

        val list = mutableListOf<Event>()

        val mainPatternPool = patternPools.boss1_d_patterns
        val variantPatternPool = patternPools.boss1_d_var3_patterns

        list.music(StoryMusicAssets.STEM_ID_BOSS_1_D, 8, extraBeatDuration = EXTRA_BEATS_SPACING, specificVariant = stemVariant)

        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnPattern(variantPatternPool.iter.next(), flipChance = NO_FLIP_CHANCE)
                .rest(4.0f)
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 1, speedMultiplier = 1.0f)
                .rest(0.125f)
                .spawnOneRod(SIDE_UPSIDE, damageVar, 1, speedMultiplier = 1.5f)
                .rest(0.875f)
                .rest(3.0f)
                .despawnPattern()
        }

        run {
            list
                .spawnPattern(mainPatternPool.iter.next())
                .rest(4.0f)
                .spawnRods()
                .rest(3.75f)
                .despawnPattern()
                .rest(0.25f)
        }

        run {
            val damageVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            list
                .spawnPattern(variantPatternPool.iter.next(), flipChance = ALWAYS_FLIP_CHANCE)
                .rest(4.0f)
                .spawnOneRod(SIDE_UPSIDE, damageVar, 1, speedMultiplier = 1.0f)
                .rest(0.125f)
                .spawnOneRod(SIDE_DOWNSIDE, damageVar, 1, speedMultiplier = 1.5f)
                .rest(0.875f)
                .rest(3.0f)
                .despawnPattern()
        }

        run {
            list
                .spawnPattern(mainPatternPool.iter.next())
                .rest(4.0f)
                .spawnRods()
                .rest(3.75f)
                .despawnPattern()
                .rest(0.25f)
        }

        return list
    }
}
