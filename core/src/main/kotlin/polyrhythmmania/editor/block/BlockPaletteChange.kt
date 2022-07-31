package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.SeparatorMenuItem
import paintbox.ui.contextmenu.SimpleMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.editor.block.data.PaletteTransitionData
import polyrhythmmania.editor.pane.dialog.PaletteEditDialog
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventPaletteChange
import polyrhythmmania.world.tileset.TilesetPalette
import java.util.*


class BlockPaletteChange(engine: Engine)
    : Block(engine, BlockPaletteChange.BLOCK_TYPES) {

    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
    }

    var tilesetPalette: TilesetPalette = engine.world.tilesetPalette.copy()
    val paletteTransitionData: PaletteTransitionData = PaletteTransitionData()

    init {
        this.width = 0.5f
        val text = Localization.getVar("block.paletteChange.name")
        this.defaultText.bind { text.use() }
    }

    override fun compileIntoEvents(): List<Event> {
        return listOf(EventPaletteChange(engine, this.beat,
                paletteTransitionData.paletteTransition.getOrCompute().copy(),
                tilesetPalette.copy()))
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(420f)
            ctxmenu.addMenuItem(SimpleMenuItem.create(Localization.getValue("blockContextMenu.paletteChange.edit"),
                    editor.editorPane.palette.markup).apply {
                this.onAction = {
                    val editorPane = editor.editorPane
                    editorPane.openDialog(PaletteEditDialog(editorPane, this@BlockPaletteChange.tilesetPalette,
                            engine.world.tilesetPalette, true,
                            "editor.dialog.tilesetPalette.title.block").prepareShow())
                }
            })
            
            ctxmenu.addMenuItem(SeparatorMenuItem())
            paletteTransitionData.createMenuItems(editor).forEach { ctxmenu.addMenuItem(it) }
        }
    }

    override fun copy(): BlockPaletteChange {
        return BlockPaletteChange(engine).also {
            this.copyBaseInfoTo(it)
            it.tilesetPalette = this.tilesetPalette.copy()
            it.paletteTransitionData.paletteTransition.set(this.paletteTransitionData.paletteTransition.getOrCompute().copy())
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("tileset", tilesetPalette.toJson())

        paletteTransitionData.writeToJson(obj)
    }

    override fun readFromJson(obj: JsonObject, editorFlags: EnumSet<EditorSpecialFlags>) {
        super.readFromJson(obj, editorFlags)
        tilesetPalette.fromJson(obj.get("tileset").asObject())
        paletteTransitionData.readFromJson(obj)
    }
}