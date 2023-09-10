package polyrhythmmania.editor.block.storymode

import com.eclipsesource.json.JsonObject
import paintbox.binding.BooleanVar
import paintbox.ui.contextmenu.CheckBoxMenuItem
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.editor.block.BlockDeployRod
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.editor.block.CustomSpeedBlock
import polyrhythmmania.editor.block.RowSetting
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventDeployRod
import polyrhythmmania.world.entity.EntityRodDecor
import java.util.*


class BlockDeployRodStoryMode(engine: Engine, blockTypes: EnumSet<BlockType> = BLOCK_TYPES)
    : BlockDeployRod(engine, blockTypes), CustomSpeedBlock {

    override var xUnitsPerBeat: Float = EntityRodDecor.DEFAULT_X_UNITS_PER_BEAT
    val defective: BooleanVar = BooleanVar(false)
    
    init {
        this.defaultText.bind { 
            if (defective.use()) "Defect. Rod" else "Rod (SM)"
        }
        this.defaultTextSecondLine.bind { 
            if (defective.use()) "(SM)" else ""
        }
    }

    override fun compileIntoEvents(): List<Event> {
        val b = this.beat - 4
        return RowSetting.getRows(rowData.rowSetting.getOrCompute(), engine.world).map { row ->
            EventDeployRod(engine, row, b, isDefective = this.defective.get()) { rod ->
                rod.xUnitsPerBeat = this.xUnitsPerBeat.coerceAtLeast(0.25f)
            }
        }
    }
    
    override fun copy(): BlockDeployRodStoryMode {
        return BlockDeployRodStoryMode(engine).also {
            this.copyBaseInfoTo(it)
            it.rowData.rowSetting.set(this.rowData.rowSetting.getOrCompute())
            it.xUnitsPerBeat = this.xUnitsPerBeat
        }
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return super.createContextMenu(editor).also { ctxmenu ->
            ctxmenu.defaultWidth.set(352f)
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(CheckBoxMenuItem.create(defective, "Defective?", editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addToContextMenu(editor)
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("xUnitsPerBeat", this.xUnitsPerBeat)
        if (this.defective.get()) {
            obj.add("defective", true)
        }
    }

    override fun readFromJson(obj: JsonObject, editorFlags: EnumSet<EditorSpecialFlags>) {
        super.readFromJson(obj, editorFlags)
        this.xUnitsPerBeat = obj.getFloat("xUnitsPerBeat", EntityRodDecor.DEFAULT_X_UNITS_PER_BEAT).coerceAtLeast(0.25f)
        this.defective.set(obj.getBoolean("defective", false))
    }
}