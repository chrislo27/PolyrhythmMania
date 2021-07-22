package polyrhythmmania.editor.block

import com.badlogic.gdx.graphics.Color
import com.eclipsesource.json.JsonObject
import paintbox.binding.Var
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.contextmenu.*
import paintbox.ui.control.TextField
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.TextBox
import polyrhythmmania.util.DecimalFormats
import polyrhythmmania.world.EventTextbox
import java.util.*


class BlockTextbox(engine: Engine)
    : Block(engine, BlockTextbox.BLOCK_TYPES) {
    
    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
    }

    var text: String = ""
    val requireInput: Var<Boolean> = Var(false)
    var duration: Float = 2f

    init {
        this.width = 0.5f
        val text = Localization.getVar("block.textbox.name")
        this.defaultText.bind { text.use() }
    }

    override fun compileIntoEvents(): List<Event> {
        return listOf(EventTextbox(engine, this.beat, duration, TextBox(text, requireInput.getOrCompute())))
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(350f)
            
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.textbox.title"), editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(CustomMenuItem(
                    HBox().apply { 
                        this.bounds.height.set(32f)
                        this.spacing.set(4f)
                        this += RectElement(Color(0f, 0f, 0f, 1f)).apply { 
                            this.border.set(Insets(1f))
                            this.borderStyle.set(SolidBorder(Color.WHITE))
                            this.padding.set(Insets(2f))
                            this += TextField(font = editor.main.fontRodinFixed).apply {
                                this.textColor.set(Color(1f, 1f, 1f, 1f))
                                this.text.set(this@BlockTextbox.text)
                                this.canInputNewlines.set(true)
                                this.text.addListener { t ->
                                    if (hasFocus.getOrCompute()) {
                                        this@BlockTextbox.text = t.getOrCompute()
                                    }
                                }
                                hasFocus.addListener { f ->
                                    if (!f.getOrCompute()) {
                                        this.text.set(this@BlockTextbox.text)
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

            ctxmenu.addMenuItem(SeparatorMenuItem())
            
            ctxmenu.addMenuItem(CheckBoxMenuItem.create(requireInput,
                    Localization.getValue("blockContextMenu.textbox.requireInput"),
                    editor.editorPane.palette.markup))
            
            ctxmenu.addMenuItem(SeparatorMenuItem())

            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.textbox.duration"), editor.editorPane.palette.markup))
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

    override fun copy(): BlockTextbox {
        return BlockTextbox(engine).also {
            this.copyBaseInfoTo(it)
            it.text = this.text
            it.requireInput.set(this.requireInput.getOrCompute())
            it.duration = this.duration
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("text", text)
        obj.add("requireInput", requireInput.getOrCompute())
        obj.add("duration", duration)
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        this.text = obj.getString("text", "")
        this.requireInput.set(obj.getBoolean("requireInput", false))
        this.duration = obj.getFloat("duration", 2f)
    }
}