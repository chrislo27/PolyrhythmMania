package polyrhythmmania.editor.block.data

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import paintbox.ui.contextmenu.CustomMenuItem
import paintbox.ui.contextmenu.MenuItem
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.SpotlightActionType
import polyrhythmmania.editor.block.contextmenu.SpotlightActionMenuPane
import polyrhythmmania.world.World
import polyrhythmmania.world.spotlights.Spotlights


class SpotlightActionData(rowCount: Int, allowedActionTypes: List<SpotlightActionType>, starting: SpotlightActionType)
    : AbstractPatternBlockData<SpotlightActionType>(rowCount, allowedActionTypes, starting) {
    
    companion object {
        
        fun readFromJson(obj: JsonObject, allowedActionTypes: List<SpotlightActionType>, objName: String = "spotlightActionData"): SpotlightActionData? {
            val patternDataObj = obj.get(objName)
            if (patternDataObj != null && patternDataObj.isObject) {
                patternDataObj as JsonObject
                val rowCount: Int = patternDataObj.getInt("rowCount", 0)
                if (rowCount > 0 && rowCount <= Spotlights.NUM_ON_ROW) {
                    val newPatData = SpotlightActionData(rowCount, allowedActionTypes, allowedActionTypes.first())
                    val a = patternDataObj.get("a")
                    if (a != null && a.isArray) {
                        a as JsonArray
                        a.forEachIndexed { index, value ->
                            if (index < rowCount && value.isNumber) {
                                newPatData.rowATypes[index] = SpotlightActionType.INDEX_MAP[value.asInt()] ?: SpotlightActionType.NO_CHANGE
                            }
                        }
                    }
                    val dpad = patternDataObj.get("dpad")
                    if (dpad != null && dpad.isArray) {
                        dpad as JsonArray
                        dpad.forEachIndexed { index, value ->
                            if (index < rowCount && value.isNumber) {
                                newPatData.rowDpadTypes[index] = SpotlightActionType.INDEX_MAP[value.asInt()] ?: SpotlightActionType.NO_CHANGE
                            }
                        }
                    }

                    return newPatData
                }
            }
            
            return null
        }
    }

    override fun createMenuItems(editor: Editor, clearType: SpotlightActionType, beatIndexStart: Int): List<MenuItem> {
        return listOf(
                CustomMenuItem(SpotlightActionMenuPane(editor.editorPane, this, clearType, beatIndexStart)),
        )
    }
    
    fun writeToJson(obj: JsonObject, objName: String = "spotlightActionData") {
        obj.add(objName, Json.`object`().also { o ->
            this.writeRowsToJsonObj(o)
        })
    }
}
