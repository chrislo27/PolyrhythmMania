package polyrhythmmania.editor.block

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import paintbox.ui.contextmenu.CustomMenuItem
import paintbox.ui.contextmenu.MenuItem
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.contextmenu.PatternMenuPane
import polyrhythmmania.world.World


class PatternBlockData(val rowCount: Int) {
    
    companion object {
        fun readFromJson(obj: JsonObject): PatternBlockData? {
            val patternDataObj = obj.get("patternData")
            if (patternDataObj != null && patternDataObj.isObject) {
                patternDataObj as JsonObject
                val rowCount: Int = patternDataObj.getInt("rowCount", 0)
                if (rowCount > 0 && rowCount < World.DEFAULT_ROW_LENGTH) {
                    val newPatData = PatternBlockData(rowCount)
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
    
    val rowATypes: Array<CubeType> = Array(rowCount) { CubeType.NONE }
    val rowDpadTypes: Array<CubeType> = Array(rowCount) { CubeType.NONE }

    fun createMenuItems(editor: Editor): List<MenuItem> {
        return listOf(
                CustomMenuItem(PatternMenuPane(editor.editorPane, this)),
        )
    }
    
    fun writeToJson(obj: JsonObject) {
        obj.add("patternData", Json.`object`().also { o ->
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