package polyrhythmmania.gamemodes.endlessmode

import polyrhythmmania.editor.block.CubeType

object EndlessPatterns {
    
    const val ENDLESS_PATTERNS_VERSION: Int = 2

    /*
    From CubeType.kt:
    '-' -> NONE
    'P' -> PISTON
    '#' -> PLATFORM
    
    '_' -> NO_CHANGE
    'O' -> PISTON_OPEN
     */
    
    val allPatterns: List<Pattern> = listOf(
"""P---P---""".parsePattern(Difficulty.VERY_EASY, flippable = true),
"""##P---P---""".parsePattern(Difficulty.VERY_EASY, flippable = true),
"""P-P-P-P-""".parsePattern(Difficulty.VERY_EASY, flippable = true),
"""##P-P-P-""".parsePattern(Difficulty.VERY_EASY, flippable = true),
"""##P---P---""".parsePattern(Difficulty.VERY_EASY, flippable = true),
"""#P-P-P-P-""".parsePattern(Difficulty.VERY_EASY, flippable = true),


"""P--PP-P-#""".parsePattern(Difficulty.EASY, flippable = true),
"""P-P-P-P-
  |######P-""".trimMargin("|").parsePattern(Difficulty.EASY, flippable = true),
"""P-P-P-P-
  |##P---P-""".trimMargin("|").parsePattern(Difficulty.EASY, flippable = true),
"""P-P-P-P-
  |P-P-P-P-""".trimMargin("|").parsePattern(Difficulty.EASY, flippable = true),
"""P-P-P-P-
  |P---P---""".trimMargin("|").parsePattern(Difficulty.EASY, flippable = true),
"""P---P---
  |##P---P---""".trimMargin("|").parsePattern(Difficulty.EASY, flippable = true),
"""P--P--##
  |P-PP--##""".trimMargin("|").parsePattern(Difficulty.EASY, flippable = true),
"""####P--#
  |P----P-#""".trimMargin("|").parsePattern(Difficulty.EASY, flippable = true),
"""####P-##
  |P-P---##""".trimMargin("|").parsePattern(Difficulty.EASY, flippable = true),
"""#P---P-#
  |#P-P---#""".trimMargin("|").parsePattern(Difficulty.EASY, flippable = true),
"""##P--P-#
  |##P--P-#""".trimMargin("|").parsePattern(Difficulty.EASY, flippable = true),
"""#P---P-#
  |#P-P---#""".trimMargin("|").parsePattern(Difficulty.EASY, flippable = true),
"""#P-P-P-#
  |P--P---#""".trimMargin("|").parsePattern(Difficulty.EASY, flippable = true),


"""P-P-P-P-
  |###P--P--""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""P-P-P-P-
  |P--P--P--""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""P-##P-P-
  |P--P--P--""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""P--P--
  |####P-""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""P--P--P--
  |####P-""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""P--P--
  |P---P---""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""P--P--P--
  |P---P---""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""#P-P-P-P-
  |##P---P---""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""#P-##P-P-#
  |##P-##P-##""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""P-P--P-#
  |P--P-P-#""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""P-P-P-##
  |#P-P-###""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""P-P-P-P-##
  |#P-P-P-###""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""P---P-P-
  |##P--P--""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""P---P---
  |P-P--P--""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""#P--PP-#
  |PP--PP-#""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""P--PP--#
  |##P--P-#""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""PP-P-PP-""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""PP---P--#
  |##PP--P-#""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""#P---PP-
  |P-PPP---""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),
"""PPPP-P-#""".trimMargin("|").parsePattern(Difficulty.MEDIUM, flippable = true),


"""PPP-PPP-""".parsePattern(Difficulty.HARD, flippable = true),
"""PP--####
  |##P--P--""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""PP--####
  |##P-P-P-""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""PPP-PPP-
  |##P-##P-""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""PPP-####
  |####PPP-""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""PP--PP--##
  |##P---P---""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""P-P-PPP-##
  |P-P-PPP-##""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""P-P--PP-#
  |P--P--P--""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""P-P-PPP-#
  |P-P-##P-#""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""#P-PP----
  |P-P--PP-#""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""#P---P--#
  |P-PP--P-#""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""PP-P-PP-
  |##P-P---""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""##P-P---
  |PP-P-PP-""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""PP-P-PP-
  |##P-P-P-""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""P--P-PP-
  |#P---PP-""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""PP--P-P-
  |P-----P-""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""P--P----
  |P-PP--P-""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""##P---P-
  |PP-PPP--""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
"""##P---P-
  |P---PPP-""".trimMargin("|").parsePattern(Difficulty.HARD, flippable = true),
            
    )
    
    val patternsByDifficulty: Map<Difficulty, List<Pattern>> = allPatterns.groupBy { it.difficulty }
    
    fun String.parsePattern(difficulty: Difficulty, flippable: Boolean, delay1: Float = 0f, delay2: Float = 0f): Pattern {
        if (this.isEmpty()) error("Expected non-empty string for pattern")
        val cubeTypeLists: List<List<CubeType>> = this.lines().take(2).mapIndexed { lineIndex, line ->
            val list = MutableList(line.length) { CubeType.NONE }
            line.forEachIndexed { index, c ->
                val cubeType = CubeType.CHAR_MAP[c] ?: error("Unknown CubeType character '${c}', accepted: [${CubeType.VALUES.joinToString(separator = ", ") { "'${it}'" }}]")
                list[index] = cubeType
            }
            list
        }
        return Pattern(RowPattern(cubeTypeLists.first(), delay1), RowPattern(cubeTypeLists.getOrNull(1) ?: emptyList(), delay2),
                difficulty, flippable)
    }
    
}