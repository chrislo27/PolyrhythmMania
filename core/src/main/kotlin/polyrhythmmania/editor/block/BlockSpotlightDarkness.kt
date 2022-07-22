package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.binding.BooleanVar
import paintbox.ui.contextmenu.CheckBoxMenuItem
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.data.SpotlightActionData
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.spotlights.EventSpotlightDarknessEnable
import java.util.*


class BlockSpotlightDarkness(engine: Engine) : AbstBlockSpotlight(engine, BlockSpotlightDarkness.BLOCK_TYPES) {

    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
    }

    val enabled: BooleanVar = BooleanVar(true)

    init {
        this.width = 1f
        this.defaultText.bind { Localization.getVar("block.spotlightDarkness.name").use() }
    }

    override fun compileIntoEvents(): List<Event> {
        return listOf(EventSpotlightDarknessEnable(engine, this.beat, this.enabled.get()))
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(300f)
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.spotlightDarkness"), editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(CheckBoxMenuItem.create(enabled,
                    Localization.getValue("blockContextMenu.spotlightDarkness.enable"),
                    editor.editorPane.palette.markup).apply {
                this.createTooltip = {
                    it.set(editor.editorPane.createDefaultTooltip(Localization.getValue("blockContextMenu.spotlightDarkness.enable.tooltip")))
                }
            })
        }
    }

    override fun copy(): BlockSpotlightDarkness {
        return BlockSpotlightDarkness(engine).also {
            this.copyBaseInfoTo(it)
            it.enabled.set(this.enabled.get())
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("enabled", enabled.get())
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        this.enabled.set(obj.getBoolean("enabled", true))
    }
}
