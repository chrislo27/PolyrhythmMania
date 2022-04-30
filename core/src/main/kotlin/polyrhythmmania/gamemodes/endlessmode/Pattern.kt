package polyrhythmmania.gamemodes.endlessmode

import polyrhythmmania.editor.block.BlockSpawnPattern
import polyrhythmmania.editor.block.CubeType
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event

data class RowPattern(val row: List<CubeType>, val delay: Float = 0f)

data class Pattern(val rowA: RowPattern, val rowDpad: RowPattern, val difficulty: Difficulty,
                   val flippable: Boolean) {
    
    fun toBlock(engine: Engine, beat: Float): BlockSpawnPattern {
        return BlockSpawnPattern(engine).apply {
            this.beat = beat
            val patternData = this.patternData
            val anyA = rowA.row.isNotEmpty()
            val anyDpad = rowDpad.row.isNotEmpty()
            val defaultA = if (anyA) CubeType.PLATFORM else CubeType.NONE
            val defaultDpad = if (anyDpad) CubeType.PLATFORM else CubeType.NONE
            for (i in 0 until patternData.rowCount) {
                this.patternData.rowATypes[i] = rowA.row.getOrNull(i) ?: defaultA
                this.patternData.rowDpadTypes[i] = rowDpad.row.getOrNull(i) ?: defaultDpad
            }
        }
    }

    fun toEvents(engine: Engine, beat: Float): List<Event> {
        return toBlock(engine, beat).compileIntoEvents(rowA.delay, rowDpad.delay)
    }

    fun flip(): Pattern = this.copy(rowA = this.rowDpad, rowDpad = this.rowA)

    fun toPatternString(): String {
        var r = rowA.row.joinToString(separator = "") { it.character.toString() }

        if (rowDpad.row.isNotEmpty()) {
            r += "\n"
            r += rowDpad.row.joinToString(separator = "") { it.character.toString() }
        }

        return r
    }
}
