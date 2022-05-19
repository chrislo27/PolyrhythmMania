package polyrhythmmania.editor.block.storymode

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
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.world.EventTextbox
import java.util.*


class BlockMemoStoryMode(engine: Engine)
    : Block(engine, BlockMemoStoryMode.BLOCK_TYPES) {
    
    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.allOf(BlockType::class.java)
    }

    var text: String = ""
    var text2: String = ""

    init {
        this.width = 1f
        this.defaultText.set("Memo\n(SM)")
    }

    override fun compileIntoEvents(): List<Event> {
        return listOf()
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(500f)
            
            val focusGroup = FocusGroup()
            ctxmenu.addMenuItem(LabelMenuItem.create("Memo text:", editor.editorPane.palette.markup))
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
                                this.text.set(this@BlockMemoStoryMode.text)
                                this.canInputNewlines.set(true)
                                this.text.addListener { t ->
                                    if (hasFocus.get()) {
                                        this@BlockMemoStoryMode.text = t.getOrCompute()
                                    }
                                }
                                hasFocus.addListener { f ->
                                    if (!f.getOrCompute()) {
                                        this.text.set(this@BlockMemoStoryMode.text)
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
            ctxmenu.addMenuItem(LabelMenuItem.create("Secondary text:", editor.editorPane.palette.markup))
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
                                this.text.set(this@BlockMemoStoryMode.text2)
                                this.canInputNewlines.set(true)
                                this.text.addListener { t ->
                                    if (hasFocus.get()) {
                                        this@BlockMemoStoryMode.text2 = t.getOrCompute()
                                    }
                                }
                                hasFocus.addListener { f ->
                                    if (!f.getOrCompute()) {
                                        this.text.set(this@BlockMemoStoryMode.text2)
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
        }
    }

    override fun copy(): BlockMemoStoryMode {
        return BlockMemoStoryMode(engine).also {
            this.copyBaseInfoTo(it)
            it.text = this.text
            it.text2 = this.text2
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("text", text)
        obj.add("text2", text2)
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        this.text = obj.getString("text", "")
        this.text2 = obj.getString("text2", "")
    }
}