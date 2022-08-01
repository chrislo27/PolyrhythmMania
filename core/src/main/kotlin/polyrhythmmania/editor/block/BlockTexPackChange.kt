package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.ui.contextmenu.ContextMenu
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.editor.block.data.TexPackSourceData
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventChangeTexturePack
import java.util.*


class BlockTexPackChange(engine: Engine)
    : Block(engine, BlockTexPackChange.BLOCK_TYPES), FlashingLightsWarnable {
    
    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
    }
    
    val texPackSrcData: TexPackSourceData = TexPackSourceData()

    init {
        this.width = 0.5f
        val text = Localization.getVar("block.texPackChange.name")
        this.defaultText.bind { text.use() }
    }
    
    override fun compileIntoEvents(): List<Event> {
        return listOf(EventChangeTexturePack(engine, this.beat, texPackSrcData.texPackSrc.getOrCompute()))
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(350f)
            texPackSrcData.createMenuItems(editor).forEach { ctxmenu.addMenuItem(it) }
        }
    }

    override fun copy(): BlockTexPackChange {
        return BlockTexPackChange(engine).also { 
            this.copyBaseInfoTo(it)
            it.texPackSrcData.texPackSrc.set(texPackSrcData.texPackSrc.getOrCompute())
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        texPackSrcData.writeToJson(obj)
    }

    override fun readFromJson(obj: JsonObject, editorFlags: EnumSet<EditorSpecialFlags>) {
        super.readFromJson(obj, editorFlags)
        texPackSrcData.readFromJson(obj)
    }
}
