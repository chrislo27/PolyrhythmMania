package polyrhythmmania.editor.block

import polyrhythmmania.world.Row
import polyrhythmmania.world.World


enum class RowSetting(val jsonId: Int) {

    ONLY_A(0),
    ONLY_DPAD(1),
    BOTH(2);

    companion object {
        val VALUES: List<RowSetting> = values().toList()
        val INDEX_MAP: Map<Int, RowSetting> = VALUES.associateBy { it.jsonId }

        fun getRows(setting: RowSetting, world: World): List<Row> {
            return when (setting) {
                ONLY_A -> listOf(world.rowA)
                ONLY_DPAD -> listOf(world.rowDpad)
                BOTH -> listOf(world.rowA, world.rowDpad)
            }
        }
    }
}