package polyrhythmmania.editor.block

import com.badlogic.gdx.graphics.Color
import com.eclipsesource.json.JsonObject
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.contextmenu.*
import paintbox.ui.control.TextField
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.pane.dialog.TilesetEditDialog
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.util.DecimalFormats
import polyrhythmmania.world.EventTilesetChange
import polyrhythmmania.world.render.TilesetConfig
import java.util.*


class BlockTilesetChange(engine: Engine)
    : Block(engine, EnumSet.of(BlockType.VFX)) {

    var tilesetConfig: TilesetConfig = engine.world.tilesetConfig.copy()
    var duration: Float = 0.5f

    init {
        this.width = 1f
        val text = Localization.getVar("block.tilesetChange.name")
        this.defaultText.bind { text.use() }
    }

    override fun compileIntoEvents(): List<Event> {
        return listOf(EventTilesetChange(engine, this.beat, this.duration.coerceAtLeast(0f), tilesetConfig.copy()))
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(300f)
            ctxmenu.addMenuItem(SimpleMenuItem.create(Localization.getValue("blockContextMenu.tilesetChange.edit"),
                    editor.editorPane.palette.markup).apply {
                this.onAction = {
                    val editorPane = editor.editorPane
                    editorPane.openDialog(TilesetEditDialog(editorPane, this@BlockTilesetChange.tilesetConfig,
                            "editor.dialog.tileset.title.block"))
                }
            })
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.tilesetChange.transitionDuration"), editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(CustomMenuItem(
                    HBox().apply { 
                        this.bounds.height.set(32f)
                        this.spacing.set(4f)
                        this += RectElement(Color(0f, 0f, 0f, 1f)).apply { 
                            this.border.set(Insets(1f))
                            this.borderStyle.set(SolidBorder(Color.WHITE))
                            this.padding.set(Insets(2f))
                            this += TextField(font = editor.editorPane.palette.musicDialogFont).apply {
                                this.textColor.set(Color(1f, 1f, 1f, 1f))
                                this.text.set(durationToStr())
                                this.inputFilter.set({ c -> c in '0'..'9' || c == '.' })
                                this.text.addListener { t ->
                                    if (hasFocus.getOrCompute()) {
                                        try {
                                            val newValue = t.getOrCompute().toFloatOrNull()
                                            if (newValue != null) {
                                                duration = newValue
                                            }
                                        } catch (ignored: Exception) {}
                                    }
                                }
                                hasFocus.addListener { f ->
                                    if (!f.getOrCompute()) {
                                        this.text.set(durationToStr())
                                    }
                                }
                                this.setOnRightClick {
                                    text.set("")
                                    requestFocus()
                                }
                            }
                        }
                    }
            ))
        }
    }
    
    private fun durationToStr(): String {
        return DecimalFormats.format("0.0##", duration)
    }

    override fun copy(): BlockTilesetChange {
        return BlockTilesetChange(engine).also {
            this.copyBaseInfoTo(it)
            it.tilesetConfig = this.tilesetConfig.copy()
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("tileset", tilesetConfig.toJson())
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        tilesetConfig.fromJson(obj.get("tileset").asObject())
    }
}