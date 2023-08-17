package polyrhythmmania.editor.block

import polyrhythmmania.util.RodinSpecialChars
import polyrhythmmania.world.Row
import polyrhythmmania.world.World


enum class RowSetting(val jsonId: Int, val stringRepresentation: String) {

    ONLY_A(0, RodinSpecialChars.BORDERED_A),
    ONLY_DPAD(1, RodinSpecialChars.BORDERED_DPAD),
    BOTH(2, "${RodinSpecialChars.BORDERED_A}${RodinSpecialChars.BORDERED_DPAD}");

    companion object {
        val INDEX_MAP: Map<Int, RowSetting> = entries.associateBy { it.jsonId }

        fun getRows(setting: RowSetting, world: World): List<Row> {
            return when (setting) {
                ONLY_A -> listOf(world.rowA)
                ONLY_DPAD -> listOf(world.rowDpad)
                BOTH -> listOf(world.rowA, world.rowDpad)
            }
        }
    }
}