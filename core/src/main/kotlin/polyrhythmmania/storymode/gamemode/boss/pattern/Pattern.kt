package polyrhythmmania.storymode.gamemode.boss.pattern

import polyrhythmmania.editor.block.BlockSpawnPattern
import polyrhythmmania.editor.block.CubeType
import polyrhythmmania.editor.block.storymode.BlockSpawnPatternStoryMode
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event


// Upside   = D-pad row
// Downside = A row
data class Pattern(
    val rowUpside: List<CubeType>,
    val rowDownside: List<CubeType>,

    val flippable: Boolean = FLIPPABLE_BY_DEFAULT,

    // rodXside = Rod speed multiplier to xUnitsPerBeat
    val rodUpside: Float = 1f,
    val rodDownside: Float = 1f,

    // delayXside = Delay for everything in beats (pattern and rods)
    val delayUpside: Float = 0f,
    val delayDownside: Float = 0f,
) {

    companion object {

        private const val FLIPPABLE_BY_DEFAULT: Boolean = false
        const val DEFAULT_X_UNITS_PER_BEAT: Float = BlockSpawnPatternStoryMode.DEFAULT_X_UNITS_PER_BEAT

        private fun String.parsePattern(): List<CubeType> {
            return this.map { c ->
                CubeType.CHAR_MAP[c]
                    ?: error("Unknown CubeType character '${c}', accepted: [${CubeType.VALUES.joinToString(separator = ", ") { "'${it}'" }}]")
            }
        }
    }

    val anyA: Boolean get() = rowDownside.isNotEmpty()
    val anyDpad: Boolean get() = rowUpside.isNotEmpty()

    constructor(
        rowUpside: String,
        rowDownside: String,
        flippable: Boolean = FLIPPABLE_BY_DEFAULT,
        rodUpside: Float = 1f,
        rodDownside: Float = 1f,
        delayUpside: Float = 0f,
        delayDownside: Float = 0f,
    ) : this(
        rowUpside.parsePattern(),
        rowDownside.parsePattern(),
        flippable,
        rodUpside,
        rodDownside,
        delayUpside,
        delayDownside
    )

    private fun toBlock(engine: Engine, beat: Float, isUpside: Boolean): BlockSpawnPattern {
        val xUnitsPerBeat = DEFAULT_X_UNITS_PER_BEAT * (if (isUpside) rodUpside else rodDownside)

        return BlockSpawnPatternStoryMode(engine, xUnitsPerBeat).apply {
            this.beat = beat

            val patternData = this.patternData
            val patternDataTypes = if (isUpside) patternData.rowDpadTypes else patternData.rowATypes
            val unusedPatternDataTypes = if (!isUpside) patternData.rowDpadTypes else patternData.rowATypes
            unusedPatternDataTypes.forEachIndexed { index, _ -> unusedPatternDataTypes[index] = CubeType.NONE }

            val row = if (isUpside) rowUpside else rowDownside

            val anyInRow = row.isNotEmpty()
            val defaultCubeType = if (anyInRow) CubeType.PLATFORM else CubeType.NONE

            for (i in 0 until patternData.rowCount) {
                patternDataTypes[i] = row.getOrNull(i) ?: defaultCubeType
            }
        }
    }

    fun toEvents(engine: Engine, beat: Float): List<Event> {
        return listOf(
            toBlock(engine, beat, true),
            toBlock(engine, beat, false),
        ).flatMap { block ->
            block.compileIntoEvents(delayDownside, delayUpside)
        }
    }

    fun flip(): Pattern = this.copy(
        rowDownside = this.rowUpside, rowUpside = this.rowDownside,
        rodUpside = this.rodDownside, rodDownside = this.rodUpside,
        delayUpside = this.delayDownside, delayDownside = this.delayUpside
    )
    
    private fun getLastPistonIndex(list: List<CubeType>): Int {
        return list.indexOfLast { cubeType ->
            cubeType == CubeType.PISTON || cubeType == CubeType.PISTON_OPEN
        }
    }
    
    fun getLastPistonIndexUpside(): Int = getLastPistonIndex(this.rowUpside)
    fun getLastPistonIndexDownside(): Int = getLastPistonIndex(this.rowDownside)
}
