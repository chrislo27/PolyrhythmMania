package polyrhythmmania.storymode.gamemode.boss.pattern

import polyrhythmmania.editor.block.BlockSelectiveSpawnPattern
import polyrhythmmania.editor.block.CubeType
import polyrhythmmania.editor.block.storymode.BlockSpawnPatternStoryMode
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event


// Upside   = D-pad row
// Downside = A row
data class Pattern(
    val rowUpside: List<CubeType>,
    val rowDownside: List<CubeType>,

    // rodXside = Beats per block (= 1 / xUnitsPerBeat), default 0.5
    val rodUpside: Float = DEFAULT_BEATS_PER_BLOCK,
    val rodDownside: Float = DEFAULT_BEATS_PER_BLOCK,

    // delayXside = Delay for everything in beats (pattern and rods)
    val delayUpside: Float = 0f,
    val delayDownside: Float = 0f,
    
    val rowUpsideTailEnd: List<CubeType>? = null,
    val rowDownsideTailEnd: List<CubeType>? = null,
    val silentMode: Boolean = false,
) {

    companion object {

        const val DEFAULT_BEATS_PER_BLOCK: Float = 1f / BlockSpawnPatternStoryMode.DEFAULT_X_UNITS_PER_BEAT

        private fun beatsPerBlockToXUnits(beatsPerBlock: Float): Float = 1f / beatsPerBlock
        
        private fun String.parsePattern(): List<CubeType> {
            return this
                .replace('.', CubeType.NO_CHANGE.character)
                .replace('^', CubeType.PISTON_OPEN.character)
                .map { c ->
                CubeType.CHAR_MAP[c]
                    ?: error("Unknown CubeType character '${c}', accepted: [${CubeType.VALUES.joinToString(separator = ", ") { "'${it}'" }}]")
            }
        }
    }

    val anyA: Boolean get() = rowDownside.isNotEmpty()
    val anyDpad: Boolean get() = rowUpside.isNotEmpty()
    val useSelectiveSpawn: Boolean get() = rowUpsideTailEnd != null && rowDownsideTailEnd != null

    constructor(
        rowUpside: String,
        rowDownside: String,
        rodUpside: Float = DEFAULT_BEATS_PER_BLOCK,
        rodDownside: Float = DEFAULT_BEATS_PER_BLOCK,
        delayUpside: Float = 0f,
        delayDownside: Float = 0f,
    ) : this(
        rowUpside.parsePattern(),
        rowDownside.parsePattern(),
        rodUpside,
        rodDownside,
        delayUpside,
        delayDownside
    )
    
    // Selective spawn
    constructor(
        rowUpside: String,
        rowUpsideTailEnd: String,
        rowDownside: String,
        rowDownsideTailEnd: String,
        silent: Boolean
    ) : this(
        rowUpside = rowUpside.parsePattern(),
        rowUpsideTailEnd = rowUpsideTailEnd.parsePattern(),
        rowDownside = rowDownside.parsePattern(),
        rowDownsideTailEnd = rowDownsideTailEnd.parsePattern(),
        silentMode = silent
    )

    private fun toBlock(engine: Engine, beat: Float, isUpside: Boolean): BlockSpawnPatternStoryMode {
        val xUnitsPerBeat = beatsPerBlockToXUnits(if (isUpside) rodUpside else rodDownside)

        return BlockSpawnPatternStoryMode(engine, xUnitsPerBeat).apply {
            this.beat = beat
            this.disableTailEnd.set(true)

            val patternData = this.patternData
            val patternDataTypes = if (isUpside) patternData.rowDpadTypes else patternData.rowATypes
            val unusedPatternDataTypes = if (!isUpside) patternData.rowDpadTypes else patternData.rowATypes
            unusedPatternDataTypes.forEachIndexed { index, _ -> unusedPatternDataTypes[index] = CubeType.NONE }

            val row = if (isUpside) rowUpside else rowDownside

            val anyInRow = row.isNotEmpty()
            val defaultCubeType = if (anyInRow) CubeType.PLATFORM else CubeType.NONE

            for (i in 0..<patternData.rowCount) {
                patternDataTypes[i] = row.getOrNull(i) ?: defaultCubeType
            }
            
            val lastIndexOfPiston = getLastPistonIndex(patternDataTypes)
            if (lastIndexOfPiston >= 0) {
                for (i in (lastIndexOfPiston + 1)..<patternData.rowCount) {
                    patternDataTypes[i] = CubeType.NONE
                }
            }
        }
    }

    private fun toSelectiveSpawnBlock(engine: Engine, beat: Float): BlockSelectiveSpawnPattern {
        return BlockSelectiveSpawnPattern(engine).apply {
            this.beat = beat
            this.isSilent.set(silentMode)

            for (i in 0..<patternData.rowCount) {
                patternData.rowATypes[i] = rowDownside.getOrNull(i) ?: CubeType.NO_CHANGE
                patternData.rowDpadTypes[i] = rowUpside.getOrNull(i) ?: CubeType.NO_CHANGE
            }
            for (i in 0..<tailEndData.rowCount) {
                tailEndData.rowATypes[i] = rowDownsideTailEnd?.getOrNull(i) ?: CubeType.NO_CHANGE
                tailEndData.rowDpadTypes[i] = rowUpsideTailEnd?.getOrNull(i) ?: CubeType.NO_CHANGE
            }
        }
    }

    fun toEvents(engine: Engine, beat: Float): List<Event> {
        return if (useSelectiveSpawn) {
            toSelectiveSpawnBlock(engine, beat).compileIntoEvents(shouldLastInTailEndAffectAll = false)
        } else {
            listOf(
                toBlock(engine, beat, true),
                toBlock(engine, beat, false),
            ).flatMap { block ->
                block.compileIntoEvents(delayDownside, delayUpside)
            }
        }
    }

    fun flip(): Pattern = this.copy(
        rowDownside = this.rowUpside, rowUpside = this.rowDownside,
        rodUpside = this.rodDownside, rodDownside = this.rodUpside,
        delayUpside = this.delayDownside, delayDownside = this.delayUpside,
        rowUpsideTailEnd = this.rowDownsideTailEnd, rowDownsideTailEnd = this.rowUpsideTailEnd
    )
    
    private fun getLastPistonIndex(list: List<CubeType>): Int {
        return list.indexOfLast { cubeType ->
            cubeType == CubeType.PISTON || cubeType == CubeType.PISTON_OPEN
        }
    }
    
    fun getLastPistonIndexUpside(): Int = getLastPistonIndex(this.rowUpside)
    fun getLastPistonIndexDownside(): Int = getLastPistonIndex(this.rowDownside)
}
