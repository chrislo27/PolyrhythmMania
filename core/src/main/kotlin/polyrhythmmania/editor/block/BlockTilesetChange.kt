package polyrhythmmania.editor.block

import com.badlogic.gdx.graphics.Color
import com.eclipsesource.json.JsonObject
import paintbox.binding.Var
import paintbox.ui.UIElement
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
import polyrhythmmania.ui.DecimalTextField
import polyrhythmmania.util.DecimalFormats
import polyrhythmmania.world.EventTilesetChange
import polyrhythmmania.world.tileset.TilesetConfig
import java.util.*


class BlockTilesetChange(engine: Engine)
    : Block(engine, BlockTilesetChange.BLOCK_TYPES) {
    
    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
    }

    var tilesetConfig: TilesetConfig = engine.world.tilesetConfig.copy()
    var duration: Float = 0.5f
    var pulseMode: Var<Boolean> = Var(false)
    var reverse: Var<Boolean> = Var(false)

    init {
        this.width = 0.5f
        val text = Localization.getVar("block.tilesetChange.name")
        this.defaultText.bind { text.use() }
    }

    override fun compileIntoEvents(): List<Event> {
        return listOf(EventTilesetChange(engine, this.beat, this.duration.coerceAtLeast(0f), tilesetConfig.copy(),
                pulseMode.getOrCompute(), reverse.getOrCompute()))
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(400f)
            ctxmenu.addMenuItem(SimpleMenuItem.create(Localization.getValue("blockContextMenu.tilesetChange.edit"),
                    editor.editorPane.palette.markup).apply {
                this.onAction = {
                    val editorPane = editor.editorPane
                    editorPane.openDialog(TilesetEditDialog(editorPane, this@BlockTilesetChange.tilesetConfig,
                            engine.world.tilesetConfig,
                            "editor.dialog.tileset.title.block").prepareShow())
                }
            })
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.tilesetChange.transitionDuration"), editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(CustomMenuItem(
                    HBox().apply { 
                        this.bounds.height.set(32f)
                        this.spacing.set(4f)

                        fun createTextField(): Pair<UIElement, TextField> {
                            val textField = DecimalTextField(startingValue = duration, decimalFormat = DecimalFormats["0.0##"],
                                    font = editor.editorPane.palette.musicDialogFont).apply {
                                this.allowNegatives.set(false)
                                this.textColor.set(Color(1f, 1f, 1f, 1f))
                                
                                this.value.addListener {
                                    duration = it.getOrCompute()
                                }
                            }
                            
                            return RectElement(Color(0f, 0f, 0f, 1f)).apply {
                                this.bindWidthToParent(adjust = -4f, multiplier = 0.333f)
                                this.border.set(Insets(1f))
                                this.borderStyle.set(SolidBorder(Color.WHITE))
                                this.padding.set(Insets(2f))
                                this += textField
                            } to textField
                        }

                        this += HBox().apply {
                            this.spacing.set(4f)
                            this += createTextField().first
                        }
                    }
            ))

            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(CheckBoxMenuItem.create(pulseMode,
                    Localization.getValue("blockContextMenu.tilesetChange.pulseMode"), editor.editorPane.palette.markup).apply {
                this.createTooltip = {
                    it.set(editor.editorPane.createDefaultTooltip(Localization.getValue("blockContextMenu.tilesetChange.pulseMode.tooltip")))
                }
            })
            ctxmenu.addMenuItem(CheckBoxMenuItem.create(reverse,
                    Localization.getValue("blockContextMenu.tilesetChange.reverse"), editor.editorPane.palette.markup).apply {
                this.createTooltip = {
                    it.set(editor.editorPane.createDefaultTooltip(Localization.getValue("blockContextMenu.tilesetChange.reverse.tooltip")))
                }
            })
        }
    }
    
    private fun durationToStr(): String {
        return DecimalFormats.format("0.0##", duration)
    }

    override fun copy(): BlockTilesetChange {
        return BlockTilesetChange(engine).also {
            this.copyBaseInfoTo(it)
            it.tilesetConfig = this.tilesetConfig.copy()
            it.duration = this.duration
            it.pulseMode.set(this.pulseMode.getOrCompute())
            it.reverse.set(this.reverse.getOrCompute())
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("tileset", tilesetConfig.toJson())
        obj.add("duration", duration)
        obj.add("pulse", pulseMode.getOrCompute())
        obj.add("reverse", reverse.getOrCompute())
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        tilesetConfig.fromJson(obj.get("tileset").asObject())
        val durationVal = obj.get("duration")
        if (durationVal != null && durationVal.isNumber) {
            duration = durationVal.asFloat().coerceAtLeast(0f)
        }
        val pulseVal = obj.get("pulse")
        if (pulseVal != null && pulseVal.isBoolean) {
            pulseMode.set(pulseVal.asBoolean())
        }
        val reverseVal = obj.get("reverse")
        if (reverseVal != null && reverseVal.isBoolean) {
            reverse.set(reverseVal.asBoolean())
        }
    }
}