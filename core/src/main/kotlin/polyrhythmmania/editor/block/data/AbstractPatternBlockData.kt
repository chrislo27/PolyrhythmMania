package polyrhythmmania.editor.block.data

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.ui.contextmenu.MenuItem
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.CubeTypeLike


abstract class AbstractPatternBlockData<E : CubeTypeLike>(val rowCount: Int, val allowedTypes: List<E>, starting: E) {
    
    val rowATypes: MutableList<E> = MutableList(rowCount) { starting }
    val rowDpadTypes: MutableList<E> = MutableList(rowCount) { starting }

    abstract fun createMenuItems(editor: Editor, clearType: E, beatIndexStart: Int): List<MenuItem>

    protected open fun writeRowsToJsonObj(obj: JsonObject) {
        val patData = this
        obj.add("rowCount", patData.rowCount)
        obj.add("a", Json.array().also { a ->
            patData.rowATypes.forEach { cubeType ->
                a.add(cubeType.jsonId)
            }
        })
        obj.add("dpad", Json.array().also { a ->
            patData.rowDpadTypes.forEach { cubeType ->
                a.add(cubeType.jsonId)
            }
        })
    }
    
}
