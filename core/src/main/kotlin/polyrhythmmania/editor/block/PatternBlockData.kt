package polyrhythmmania.editor.block

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import paintbox.ui.contextmenu.CustomMenuItem
import paintbox.ui.contextmenu.MenuItem
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.contextmenu.PatternMenuPane
import polyrhythmmania.world.World


class PatternBlockData(val rowCount: Int, val allowedCubeTypes: List<CubeType>, starting: CubeType) {
    
    companion object {
        val GENERAL_CUBE_TYPES: List<CubeType> = listOf(CubeType.NONE, CubeType.PISTON, CubeType.PLATFORM)
        val SELECTIVE_SPAWN_CUBE_TYPES: List<CubeType> = listOf(CubeType.NO_CHANGE, CubeType.NONE, CubeType.PISTON,
                CubeType.PLATFORM, CubeType.PISTON_OPEN)
        
        fun readFromJson(obj: JsonObject, allowedCubeTypes: List<CubeType>, objName: String = "patternData"): PatternBlockData? {
            val patternDataObj = obj.get(objName)
            if (patternDataObj != null && patternDataObj.isObject) {
                patternDataObj as JsonObject
                val rowCount: Int = patternDataObj.getInt("rowCount", 0)
                if (rowCount > 0 && rowCount < World.DEFAULT_ROW_LENGTH) {
                    val newPatData = PatternBlockData(rowCount, allowedCubeTypes, allowedCubeTypes.first())
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
    
    val rowATypes: Array<CubeType> = Array(rowCount) { starting }
    val rowDpadTypes: Array<CubeType> = Array(rowCount) { starting }

    fun createMenuItems(editor: Editor, clearType: CubeType, beatIndexStart: Int): List<MenuItem> {
        return listOf(
                CustomMenuItem(PatternMenuPane(editor.editorPane, this, clearType, beatIndexStart)),
        )
    }
    
    fun writeToJson(obj: JsonObject, objName: String = "patternData") {
        obj.add(objName, Json.`object`().also { o ->
            val patData = this
            o.add("rowCount", patData.rowCount)
            o.add("a", Json.array().also { a ->
                patData.rowATypes.forEach { cubeType ->
                    a.add(cubeType.jsonId)
                }
            })
            o.add("dpad", Json.array().also { a ->
                patData.rowDpadTypes.forEach { cubeType ->
                    a.add(cubeType.jsonId)
                }
            })
        })
    }

}