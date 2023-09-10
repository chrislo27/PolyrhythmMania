package polyrhythmmania.editor.block.storymode

import com.eclipsesource.json.JsonObject
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.editor.block.BlockSpawnPattern
import polyrhythmmania.editor.block.CustomSpeedBlock
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.entity.EntityRodDecor
import java.util.*


class BlockSpawnPatternStoryMode(engine: Engine) : BlockSpawnPattern(engine), CustomSpeedBlock {

    companion object {

        const val DEFAULT_X_UNITS_PER_BEAT: Float = EntityRodDecor.DEFAULT_X_UNITS_PER_BEAT
    }

    override var xUnitsPerBeat: Float = DEFAULT_X_UNITS_PER_BEAT
    
    constructor(engine: Engine, xUnitsPerBeat: Float) : this(engine) {
        this.xUnitsPerBeat = xUnitsPerBeat
    }
    
    init {
        this.width = 4f
        this.defaultText.set("Spawn Pattern (SM)")
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

    override fun copy(): BlockSpawnPatternStoryMode {
        return BlockSpawnPatternStoryMode(engine).also {
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
        if (obj.get("beatsPerBlock") != null) {
            this.xUnitsPerBeat = 1f / obj.getFloat("beatsPerBlock", 0.5f).coerceAtLeast(0.25f)
        } else {
            this.xUnitsPerBeat = obj.getFloat("xUnitsPerBeat", EntityRodDecor.DEFAULT_X_UNITS_PER_BEAT).coerceAtLeast(0.25f)
        }
    }
}