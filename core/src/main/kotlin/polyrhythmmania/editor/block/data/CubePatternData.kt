package polyrhythmmania.editor.block.data

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import paintbox.ui.contextmenu.CustomMenuItem
import paintbox.ui.contextmenu.MenuItem
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.CubeType
import polyrhythmmania.editor.block.contextmenu.CubePatternMenuPane
import polyrhythmmania.world.World


class CubePatternData(rowCount: Int, allowedCubeTypes: List<CubeType>, starting: CubeType)
    : AbstractPatternBlockData<CubeType>(rowCount, allowedCubeTypes, starting) {
    
    companion object {
        val GENERAL_CUBE_TYPES: List<CubeType> = listOf(CubeType.NONE, CubeType.PISTON, CubeType.PLATFORM)
        val SELECTIVE_SPAWN_CUBE_TYPES: List<CubeType> = listOf(CubeType.NO_CHANGE, CubeType.NONE, CubeType.PISTON,
                CubeType.PLATFORM, CubeType.PISTON_OPEN, CubeType.RETRACT_PISTON)
        
        fun readFromJson(obj: JsonObject, allowedCubeTypes: List<CubeType>, objName: String = "patternData"): CubePatternData? {
            val patternDataObj = obj.get(objName)
            if (patternDataObj != null && patternDataObj.isObject) {
                patternDataObj as JsonObject
                val rowCount: Int = patternDataObj.getInt("rowCount", 0)
                if (rowCount > 0 && rowCount < World.DEFAULT_ROW_LENGTH) {
                    val newPatData = CubePatternData(rowCount, allowedCubeTypes, allowedCubeTypes.first())
                    val a = patternDataObj.get("a")
                    if (a != null && a.isArray) {
                        a as JsonArray
                        a.forEachIndexed { index, value ->
                            if (index < rowCount && value.isNumber) {
                                newPatData.rowATypes[index] = CubeType.INDEX_MAP[value.asInt()] ?: CubeType.NONE
                            }
                        }
                    }
                    val dpad = patternDataObj.get("dpad")
                    if (dpad != null && dpad.isArray) {
                        dpad as JsonArray
                        dpad.forEachIndexed { index, value ->
                            if (index < rowCount && value.isNumber) {
                                newPatData.rowDpadTypes[index] = CubeType.INDEX_MAP[value.asInt()] ?: CubeType.NONE
                            }
                        }
                    }

                    return newPatData
                }
            }
            
            return null
        }
    }

    override fun createMenuItems(editor: Editor, clearType: CubeType, beatIndexStart: Int): List<MenuItem> {
        return listOf(
                CustomMenuItem(CubePatternMenuPane(editor.editorPane, this, clearType, beatIndexStart)),
        )
    }
    
    fun writeToJson(obj: JsonObject, objName: String = "patternData") {
        obj.add(objName, Json.`object`().also { o ->
            this.writeRowsToJsonObj(o)
        })
    }
}
