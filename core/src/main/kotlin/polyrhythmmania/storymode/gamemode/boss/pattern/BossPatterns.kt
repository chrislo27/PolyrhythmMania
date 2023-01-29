package polyrhythmmania.storymode.gamemode.boss.pattern


/**
 * NOTE: This pattern repository is kept entirely separate from [polyrhythmmania.gamemodes.endlessmode.EndlessPatterns]
 * so changes to one do not inadvertently affect the other.
 */
object BossPatterns {

    const val BOSS_INPUT_PATTERNS_VERSION: Int = 1

    /*
    From CubeType.kt:
    '-' -> NONE
    'P' -> PISTON
    '#' -> PLATFORM
    
    '_' -> NO_CHANGE
    'O' -> PISTON_OPEN
     */

    val boss1_x_pat0 = Pattern(
        rowUpside = "##P---P---",
        rowDownside = "P---P---##",
    )
    val boss1_x_pat1 = Pattern(
        rowUpside = "##P---P---",
        rowDownside = "P-P-P-P-##",
    )
    val boss1_x_pat2 = Pattern(
        rowUpside = "##P-#P-P-#",
        rowDownside = "P-P-P-P-##",
    )
    val boss1_x_pat3 = Pattern(
        rowUpside = "##P---P---",
        rowDownside = "P--P--P--#",
    )
    val boss1_x_pat4 = Pattern(
        rowUpside = "##P---P---",
        rowDownside = "P-#P-P-###",
    )
    val boss1_x_pat5 = Pattern(
        rowUpside = "##P-P-P-##",
        rowDownside = "P-#P-P-###",
    )
    val boss1_x_pat6 = Pattern(
        rowUpside = "##P-P-P-##",
        rowDownside = "#P-P-P-###",
    )
    val boss1_x_pat7 = Pattern(
        rowUpside = "##P---P---",
        rowDownside = "P---#P---#",
    )
    val boss1_x_pat8 = Pattern(
        rowUpside = "##P---P---",
        rowDownside = "P-#P-P-P-#",
    )
    val boss1_x_pat9 = Pattern(
        rowUpside = "##P---P---",
        rowDownside = "#P-P-P-P-#",
    )
    val boss1_x_pat10 = Pattern(
        rowUpside = "####P-P-##",
        rowDownside = "#P-P-P-###",
    )
    val boss1_x_pat11 = Pattern(
        rowUpside = "##P-P-P-##",
        rowDownside = "#P-P-P-P-#",
    )


    val boss1_c_mus0_pat0 = Pattern(
        rowUpside = "#P-#P-P-##",
        rowDownside = "P-P-P-P-##",
    )
    val boss1_c_mus0_pat1 = Pattern(
        rowUpside = "#P-#P-P-##",
        rowDownside = "P---P---##",
    )

    val boss1_d_mus0_pat0 = Pattern(
        rowUpside = "####P-P-P-",
        rowDownside = "P-P-P-P-P-",
        rodUpside = 1.5f,
        rodDownside = 1.5f,
        delayUpside = 0.5f,
        delayDownside = 0.5f
    )
    val boss1_d_mus0_pat1 = Pattern(
        rowUpside = "P-##P-##P-",
        rowDownside = "P-P-P-P-P-",
        rodUpside = 1.5f,
        rodDownside = 1.5f,
        delayUpside = 0.5f,
        delayDownside = 0.5f
    )

    val endlessEasy: List<Pattern> = listOf(
        Pattern(rowUpside = "##P---P---", rowDownside = "P---P---", flippable = true), // This should stay first
        Pattern(rowUpside = "P--PP-P-#", rowDownside = "", flippable = true),
        Pattern(rowUpside = "######P-", rowDownside = "P-P-P-P-", flippable = true),
        Pattern(rowUpside = "##P---P-", rowDownside = "P-P-P-P-", flippable = true),
        Pattern(rowUpside = "P-P-P-P-", rowDownside = "P-P-P-P-", flippable = true),
        Pattern(rowUpside = "P---P---", rowDownside = "P-P-P-P-", flippable = true),
        Pattern(rowUpside = "P-PP--##", rowDownside = "P--P--##", flippable = true),
        Pattern(rowUpside = "P----P-#", rowDownside = "####P--#", flippable = true),
        Pattern(rowUpside = "P-P---##", rowDownside = "####P-##", flippable = true),
        Pattern(rowUpside = "#P-P---#", rowDownside = "#P---P-#", flippable = true),
        Pattern(rowUpside = "##P--P-#", rowDownside = "##P--P-#", flippable = true),
        Pattern(rowUpside = "#P-P---#", rowDownside = "#P---P-#", flippable = true),
        Pattern(rowUpside = "P--P---#", rowDownside = "#P-P-P-#", flippable = true),
    )
    val endlessMedium: List<Pattern> = listOf(
        Pattern(rowUpside = "P-P-P-P-", rowDownside = "###P--P--", flippable = true),
        Pattern(rowUpside = "P-P-P-P-", rowDownside = "P--P--P--", flippable = true),
        Pattern(rowUpside = "P-##P-P-", rowDownside = "P--P--P--", flippable = true),
        Pattern(rowUpside = "P--P--", rowDownside = "####P-", flippable = true),
        Pattern(rowUpside = "P--P--P--", rowDownside = "####P-", flippable = true),
        Pattern(rowUpside = "P--P--", rowDownside = "P---P---", flippable = true),
        Pattern(rowUpside = "P--P--P--", rowDownside = "P---P---", flippable = true),
        Pattern(rowUpside = "#P-P-P-P-", rowDownside = "##P---P---", flippable = true),
        Pattern(rowUpside = "#P-##P-P-#", rowDownside = "##P-##P-##", flippable = true),
        Pattern(rowUpside = "P-P--P-#", rowDownside = "P--P-P-#", flippable = true),
        Pattern(rowUpside = "P-P-P-##", rowDownside = "#P-P-###", flippable = true),
        Pattern(rowUpside = "P-P-P-P-##", rowDownside = "#P-P-P-###", flippable = true),
        Pattern(rowUpside = "P---P-P-", rowDownside = "##P--P--", flippable = true),
        Pattern(rowUpside = "P---P---", rowDownside = "P-P--P--", flippable = true),
        Pattern(rowUpside = "#P--PP-#", rowDownside = "PP--PP-#", flippable = true),
        Pattern(rowUpside = "P--PP--#", rowDownside = "##P--P-#", flippable = true),
        Pattern(rowUpside = "PP---P--#", rowDownside = "##PP--P-#", flippable = true),
        Pattern(rowUpside = "#P---PP-", rowDownside = "P-PPP---", flippable = true),
        Pattern(rowUpside = "PP-P-PP-", rowDownside = "", flippable = true),
        Pattern(rowUpside = "PPPP-P-#", rowDownside = "", flippable = true),
    )

}
