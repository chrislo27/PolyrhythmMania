package polyrhythmmania.storymode.gamemode.boss.pattern

import java.util.*


class BossPatternPools(val random: Random) {

    //region Phase 1 - A


    // Phase 1 - A1
    private val boss1_a1_patterns_list = BossPatterns.endlessEasy
    val introPattern: Pattern = BossPatterns.boss1_x_pat0
    val boss1_a1_patterns: PatternPool = boss1_a1_patterns_list.toPool(bannedFirst = introPattern)

    // Phase 1 - A2
    val boss1_a2_patterns: PatternPool = BossPatterns.endlessMedium.toPool()

    //endregion


    //region Phase 1 - B

    val boss1_b_patterns: PatternPool = listOf(
        BossPatterns.boss1_x_pat0,
        BossPatterns.boss1_x_pat1,
        BossPatterns.boss1_x_pat2,
        BossPatterns.boss1_x_pat3,
        BossPatterns.boss1_x_pat4,
        BossPatterns.boss1_x_pat5,
        BossPatterns.boss1_x_pat6,
        BossPatterns.boss1_x_pat7,
        BossPatterns.boss1_x_pat8,
        BossPatterns.boss1_x_pat9,
    ).toPool()

    //endregion


    //region Phase 1 - C

    val boss1_c_patterns: PatternPool = listOf(
        BossPatterns.boss1_x_pat0,
        BossPatterns.boss1_x_pat1,
        BossPatterns.boss1_x_pat2,
        BossPatterns.boss1_x_pat3,
        BossPatterns.boss1_x_pat4,
        BossPatterns.boss1_x_pat5,
        BossPatterns.boss1_x_pat6,
        BossPatterns.boss1_x_pat7,
        BossPatterns.boss1_x_pat8,
        BossPatterns.boss1_x_pat9,
    ).toPool()

    val boss1_c_var1_patterns: PatternPool = listOf(
        BossPatterns.boss1_c_var1_pat0,
        BossPatterns.boss1_c_var1_pat1,
        BossPatterns.boss1_c_var1_pat2,
    ).toPool()

    val boss1_c_var2_patterns: PatternPool = listOf(
        BossPatterns.boss1_c_var2_pat0,
        BossPatterns.boss1_c_var2_pat1,
        BossPatterns.boss1_c_var2_pat2,
    ).toPool()

    val boss1_c_var3_patterns: PatternPool = listOf(
        BossPatterns.boss1_c_var3_pat0,
        BossPatterns.boss1_c_var3_pat1,
        BossPatterns.boss1_c_var3_pat2,
    ).toPool()

    //endregion


    //region Phase 1 - D

    val boss1_d_patterns: PatternPool = listOf(
        BossPatterns.boss1_x_pat6,
        BossPatterns.boss1_x_pat9,
        BossPatterns.boss1_x_pat10,
        BossPatterns.boss1_x_pat11,
        BossPatterns.boss1_x_pat12,
    ).toPool()
    
    // Phase 1 - D (Mus0)
    val boss1_d_var1_patterns: PatternPool = listOf(
        BossPatterns.boss1_d_var1_pat0,
        BossPatterns.boss1_d_var1_pat1,
    ).toPool()

    // Phase 1 - D (Mus1)
    val boss1_d_var2_patterns: PatternPool = listOf(
        BossPatterns.boss1_d_var2_pat0,
        BossPatterns.boss1_d_var2_pat1,
    ).toPool()

    // Phase 1 - D (Mus2)
    val boss1_d_var3_patterns: PatternPool = listOf(
        BossPatterns.boss1_d_var3_pat0,
        BossPatterns.boss1_d_var3_pat1,
    ).toPool()

    //endregion


    //region Phase 1 - E

    // Phase 1 - E1
    val boss1_e1_patterns: PatternPool = listOf<Pattern>(
        // ...,
    ).toPool()

    // Phase 1 - E2
    val boss1_e2_patterns: PatternPool = listOf<Pattern>(
        // ...,
    ).toPool()

    //endregion
    
    
    val allPools: List<PatternPool> = listOf(
        boss1_a1_patterns,
        boss1_a2_patterns,
        boss1_b_patterns,
        boss1_c_patterns,
        boss1_c_var1_patterns,
        boss1_c_var2_patterns,
        boss1_c_var3_patterns,
        boss1_d_patterns,
        boss1_d_var1_patterns,
        boss1_d_var2_patterns,
        boss1_d_var3_patterns,
        boss1_e1_patterns,
        boss1_e2_patterns,
    )

    private fun List<Pattern>.toPool(bannedFirst: Pattern? = null): PatternPool =
        PatternPool(this, random, bannedFirst)

}
