package polyrhythmmania.editor.block

import polyrhythmmania.engine.Engine
import polyrhythmmania.world.Row
import polyrhythmmania.world.World


enum class RowSetting {
    
    ONLY_A,
    ONLY_DPAD,
    BOTH;
    
    companion object {
        fun getRows(setting: RowSetting, world: World): List<Row> {
            return when (setting) {
                ONLY_A -> listOf(world.rowA)
                ONLY_DPAD -> listOf(world.rowDpad)
                BOTH -> listOf(world.rowA, world.rowDpad)
            }
        }
    }
}