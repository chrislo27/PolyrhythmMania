package polyrhythmmania.storymode.gamemode.boss.pattern

import java.util.*


class BossPatternPools(val random: Random) {

    //region Phase 1 - A


    // Phase 1 - A1
    private val boss1_a1_patterns_list = BossPatterns.endlessEasy
    val introPattern: Pattern = boss1_a1_patterns_list.first()
    val boss1_a1_patterns: PatternPool = boss1_a1_patterns_list.toPool(bannedFirst = introPattern)

    // Phase 1 - A2
    val boss1_a2_patterns: PatternPool = BossPatterns.endlessMedium.toPool()

    //endregion


    //region Phase 1 - B

    // Phase 1 - B1
    val boss1_b1_patterns: PatternPool = listOf(
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

    // Phase 1 - B2
    val boss1_b2_patterns: PatternPool = boss1_b1_patterns.copy()

    //endregion


    //region Phase 1 - C

    // Phase 1 - C (in-between)
    val boss1_c_x_patterns: PatternPool = boss1_b1_patterns.copy()

    // Phase 1 - C (Mus0)
    val boss1_c_mus0_patterns: PatternPool = listOf(
        BossPatterns.boss1_c_mus0_pat0,
        BossPatterns.boss1_c_mus0_pat1,
        // ...,
    ).toPool()

    // Phase 1 - C (Mus1)
    val boss1_c_mus1_patterns: PatternPool = listOf<Pattern>(
        // ...,
    ).toPool()

    // Phase 1 - C (Mus2)
    val boss1_c_mus2_patterns: PatternPool = listOf<Pattern>(
        // ...,
    ).toPool()

    //endregion


    //region Phase 1 - D

    // Phase 1 - D (Mus0)
    val boss1_d_mus0_patterns: PatternPool = listOf(
        BossPatterns.boss1_d_mus0_pat0,
        BossPatterns.boss1_d_mus0_pat1,
        // ...,
    ).toPool()

    // Phase 1 - D (Mus1)
    val boss1_d_mus1_patterns: PatternPool = listOf<Pattern>(
        // ...,
    ).toPool()

    // Phase 1 - D (Mus2)
    val boss1_d_mus2_patterns: PatternPool = listOf<Pattern>(
        // ...,
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


    //region Phase 1 - F

    // Phase 1 - F (Mus0)
    val boss1_f_mus0_patterns: PatternPool = listOf<Pattern>(
        // ...,
    ).toPool()

    // Phase 1 - F (Mus1)
    val boss1_f_mus1_patterns: PatternPool = listOf<Pattern>(
        // ...,
    ).toPool()

    // Phase 1 - F (Mus2)
    val boss1_f_mus2_patterns: PatternPool = listOf<Pattern>(
        // ...,
    ).toPool()

    //endregion

    
    val allPools: List<PatternPool> = listOf(
        boss1_a1_patterns,
        boss1_a2_patterns,
        boss1_b1_patterns,
        boss1_b2_patterns,
        boss1_c_x_patterns,
        boss1_c_mus0_patterns,
        boss1_c_mus1_patterns,
        boss1_c_mus2_patterns,
        boss1_d_mus0_patterns,
        boss1_d_mus1_patterns,
        boss1_d_mus2_patterns,
        boss1_e1_patterns,
        boss1_e2_patterns,
        boss1_f_mus0_patterns,
        boss1_f_mus1_patterns,
        boss1_f_mus2_patterns,
    )

    private fun List<Pattern>.toPool(bannedFirst: Pattern? = null): PatternPool =
        PatternPool(this, random, bannedFirst)

}
