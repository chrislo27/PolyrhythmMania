package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.engine.Engine
import java.util.*


class BlockSpawnPatternCustomSpeed(engine: Engine) : BlockSpawnPattern(engine), CustomSpeedBlock {

    override var xUnitsPerBeat: Float = CustomSpeedBlock.DEFAULT_X_UNITS_PER_BEAT

    init {
        this.width = 4f
        this.defaultText.bind { Localization.getVar("block.spawnPatternCustomSpeed.name").use() }
    }

    override fun getBeatsPerBlock(): Float {
        return 1f / this.xUnitsPerBeat
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return super.createContextMenu(editor).also { ctxmenu ->
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addToContextMenu(editor)
        }
    }

    override fun copy(): BlockSpawnPatternCustomSpeed {
        return BlockSpawnPatternCustomSpeed(engine).also {
            this.copyBaseInfoTo(it)
            for (i in 0..<ROW_COUNT) {
                it.patternData.rowATypes[i] = this.patternData.rowATypes[i]
                it.patternData.rowDpadTypes[i] = this.patternData.rowDpadTypes[i]
            }
            it.disableTailEnd.set(this.disableTailEnd.get())
            it.xUnitsPerBeat = this.xUnitsPerBeat
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("xUnitsPerBeat", this.xUnitsPerBeat)
    }

    override fun readFromJson(obj: JsonObject, editorFlags: EnumSet<EditorSpecialFlags>) {
        super.readFromJson(obj, editorFlags)
        this.xUnitsPerBeat =
            obj.getFloat("xUnitsPerBeat", CustomSpeedBlock.DEFAULT_X_UNITS_PER_BEAT).coerceAtLeast(CustomSpeedBlock.MIN_X_UNITS_PER_BEAT)
    }
}