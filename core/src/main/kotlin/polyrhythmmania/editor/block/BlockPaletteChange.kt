package polyrhythmmania.editor.block

import com.badlogic.gdx.graphics.Color
import com.eclipsesource.json.JsonObject
import paintbox.binding.BooleanVar
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.contextmenu.*
import paintbox.ui.control.DecimalTextField
import paintbox.ui.control.TextField
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.pane.dialog.PaletteEditDialog
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import paintbox.util.DecimalFormats
import polyrhythmmania.world.EventPaletteChange
import polyrhythmmania.world.tileset.PaletteTransition
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
            ctxmenu.defaultWidth.set(400f)
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

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        tilesetPalette.fromJson(obj.get("tileset").asObject())
        paletteTransitionData.readFromJson(obj)
    }
}