package polyrhythmmania.storymode.gamemode.boss.scripting

import polyrhythmmania.engine.Event
import polyrhythmmania.storymode.gamemode.boss.pattern.BossPatternPools
import polyrhythmmania.storymode.gamemode.boss.pattern.PatternPool
import polyrhythmmania.storymode.gamemode.boss.scripting.BossScriptPhase1C.VariantFactory
import polyrhythmmania.storymode.music.StoryMusicAssets
import kotlin.random.asKotlinRandom


class BossScriptPhase1C(phase1: BossScriptPhase1) : AbstractBossScriptPhase1Part(phase1) {

    private fun interface VariantFactory {
        fun create(): AbstractBossScriptPhase1CVariant
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
            VariantFactory { BossScriptPhase1CVar1(phase1) },
            VariantFactory { BossScriptPhase1CVar2(phase1) },
            VariantFactory { BossScriptPhase1CVar3(phase1) },
        )

        val randomVariantFactory = variants.random(this.random.asKotlinRandom())
        list.addAll(randomVariantFactory.create().getEvents())

        return list
    }
}

private abstract class AbstractBossScriptPhase1CVariant(
    phase1: BossScriptPhase1,
    val stemVariant: Int,
    val variantPatternPoolGetter: (BossPatternPools) -> PatternPool
) : AbstractBossScriptPhase1Part(phase1) {
    override fun getEvents(): List<Event> {
        /*
    set_music(boss1_c_var1);

    spawn_pattern(boss1_c_var1_patterns[random()], NO_FLIP);
    rest 4.0;
    spawn_rods();
    rest 3.5;
    despawn_pattern();
    rest 0.5;

    spawn_pattern(boss1_c_patterns[random()], NO_FLIP);
    rest 4.0;
    spawn_rods();
    rest 3.75;
    despawn_pattern();
    rest 0.25;

    spawn_pattern(boss1_c_var1_patterns[random()], FLIP);
    rest 4.0;
    spawn_rods();
    rest 3.5;
    despawn_pattern();
    rest 0.5;

    spawn_pattern(boss1_c_patterns[random()], FLIP);
    rest 4.0;
    spawn_rods();
    rest 4.0;

 */

        val list = mutableListOf<Event>()

        val mainPatternPool = patternPools.boss1_c_patterns
        val variantPatternPool = this.variantPatternPoolGetter(patternPools)

        list.music(StoryMusicAssets.STEM_ID_BOSS_1_C, 8, extraBeatDuration = EXTRA_BEATS_SPACING, specificVariant = stemVariant)

        list
            .spawnPattern(variantPatternPool.iter.next(), flipChance = NO_FLIP_CHANCE)
            .rest(4.0f)
            .spawnRods()
            .rest(3.5f)
            .despawnPattern()
            .rest(0.5f)
        
            .spawnPattern(mainPatternPool.iter.next(), flipChance = NO_FLIP_CHANCE)
            .rest(4.0f)
            .spawnRods()
            .rest(3.75f)
            .despawnPattern()
            .rest(0.25f)
        
            .spawnPattern(variantPatternPool.iter.next(), flipChance = ALWAYS_FLIP_CHANCE)
            .rest(4.0f)
            .spawnRods()
            .rest(3.5f)
            .despawnPattern()
            .rest(0.5f)

            .spawnPattern(mainPatternPool.iter.next(), flipChance = ALWAYS_FLIP_CHANCE)
            .rest(4.0f)
            .spawnRods()
            .rest(4.0f)

        return list
    }
}

private class BossScriptPhase1CVar1(phase1: BossScriptPhase1) : AbstractBossScriptPhase1CVariant(
    phase1, 1, { pools -> pools.boss1_c_var1_patterns }
)

private class BossScriptPhase1CVar2(phase1: BossScriptPhase1) : AbstractBossScriptPhase1CVariant(
    phase1, 2, { pools -> pools.boss1_c_var2_patterns }
)

private class BossScriptPhase1CVar3(phase1: BossScriptPhase1) : AbstractBossScriptPhase1CVariant(
    phase1, 3, { pools -> pools.boss1_c_var3_patterns }
)
