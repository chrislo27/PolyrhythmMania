package polyrhythmmania.editor.block

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.eclipsesource.json.JsonObject
import paintbox.binding.BooleanVar
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.contextmenu.*
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.TextBox
import polyrhythmmania.engine.TextBoxStyle
import paintbox.util.DecimalFormats
import polyrhythmmania.world.EventTextbox
import java.util.*


class BlockTextbox(engine: Engine)
    : Block(engine, BlockTextbox.BLOCK_TYPES) {
    
    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
        val DEFAULT_STYLE: TextBoxStyle = TextBoxStyle.DIALOGUE
    }

    var text: String = ""
    val requireInput: BooleanVar = BooleanVar(false)
    var duration: Float = 2f
    var style: TextBoxStyle = DEFAULT_STYLE

    init {
        this.width = 0.5f
        val text = Localization.getVar("block.textbox.name")
        this.defaultText.bind { text.use() }
    }

    override fun compileIntoEvents(): List<Event> {
        return listOf(EventTextbox(engine, this.beat, duration,
                TextBox(text, requireInput.get(), style = this.style)))
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(400f)
            
            val focusGroup = FocusGroup()
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
                                    if (hasFocus.get()) {
                                        this@BlockTextbox.text = t.getOrCompute()
                                    }
                                }
                                hasFocus.addListener { f ->
                                    if (!f.getOrCompute()) {
                                        this.text.set(this@BlockTextbox.text)
                                    }
                                }
                                this.setOnRightClick {
                                    requestFocus()
                                    text.set("")
                                }
                                focusGroup.addFocusable(this)
                            }
                        }
                    }
            ))

            ctxmenu.addMenuItem(SeparatorMenuItem())
            
            val combobox = ComboBox(TextBoxStyle.VALUES, style).also { combobox ->
                combobox.markup.set(editor.editorPane.palette.markup)
                combobox.itemStringConverter.set { 
                    Localization.getValue("blockContextMenu.textbox.style.${it.name.lowercase(Locale.ROOT)}")
                }
                combobox.selectedItem.addListener {
                    this.style = it.getOrCompute()
                }
            }
            val comboboxPane = HBox().also { hbox ->
                hbox.spacing.set(8f)
                hbox.bounds.height.set(32f)
                hbox += TextLabel(Localization.getValue("blockContextMenu.textbox.style")).apply {
                    this.markup.set(editor.editorPane.palette.markup)
                    this.renderAlign.set(Align.right)
                    this.bounds.width.set(64f)
                }
                hbox += combobox.apply {
                    this.bindWidthToParent(adjust = -72f)
                }
            }
            ctxmenu.addMenuItem(CustomMenuItem(comboboxPane))
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
                            this += DecimalTextField(startingValue = duration, decimalFormat = DecimalFormats["0.0##"],
                                    font = editor.editorPane.palette.musicDialogFont).apply {
                                this.minimumValue.set(0f)
                                this.textColor.set(Color(1f, 1f, 1f, 1f))

                                this.value.addListener {
                                    duration = it.getOrCompute()
                                }
                                focusGroup.addFocusable(this)
                            }
                        }
                    }
            ))
        }
    }

    override fun copy(): BlockTextbox {
        return BlockTextbox(engine).also {
            this.copyBaseInfoTo(it)
            it.text = this.text
            it.requireInput.set(this.requireInput.get())
            it.duration = this.duration
            it.style = this.style
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("text", text)
        obj.add("requireInput", requireInput.get())
        obj.add("duration", duration)
        obj.add("style", style.jsonId)
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        this.text = obj.getString("text", "")
        this.requireInput.set(obj.getBoolean("requireInput", false))
        this.duration = obj.getFloat("duration", 2f)
        this.style = TextBoxStyle.JSON_MAPPING[obj.getInt("style", 0)] ?: DEFAULT_STYLE
    }
}