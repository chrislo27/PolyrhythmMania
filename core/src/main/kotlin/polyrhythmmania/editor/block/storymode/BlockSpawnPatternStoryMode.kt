package polyrhythmmania.editor.block.storymode

import com.badlogic.gdx.graphics.Color
import com.eclipsesource.json.JsonObject
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.CustomMenuItem
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.contextmenu.SeparatorMenuItem
import paintbox.ui.control.DecimalTextField
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.util.DecimalFormats
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.BlockSpawnPattern
import polyrhythmmania.engine.Engine


class BlockSpawnPatternStoryMode(engine: Engine) : BlockSpawnPattern(engine) {

    private var beatsPerBlock: Float = 0.5f
    
    init {
        this.width = 4f
        this.defaultText.set("Spawn Pat (SM)")
    }
    
    override fun getBeatsPerBlock(): Float {
        return this.beatsPerBlock
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return super.createContextMenu(editor).also { ctxmenu ->
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(LabelMenuItem.create("Beats per block (def. 0.5)", editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(CustomMenuItem(
                HBox().apply {
                    this.bounds.height.set(32f)
                    this.spacing.set(4f)
                    this += RectElement(Color(0f, 0f, 0f, 1f)).apply {
                        this.border.set(Insets(1f))
                        this.borderStyle.set(SolidBorder(Color.WHITE))
                        this.padding.set(Insets(2f))
                        this += DecimalTextField(startingValue = beatsPerBlock, decimalFormat = DecimalFormats["0.0##"],
                            font = editor.editorPane.palette.musicDialogFont).apply {
                            this.minimumValue.set(1 / 16f)
                            this.textColor.set(Color(1f, 1f, 1f, 1f))

                            this.value.addListener {
                                beatsPerBlock = it.getOrCompute()
                            }
                        }
                    }
                }
            ))
        }
    }

    override fun copy(): BlockSpawnPatternStoryMode {
        return BlockSpawnPatternStoryMode(engine).also {
            this.copyBaseInfoTo(it)
            for (i in 0 until ROW_COUNT) {
                it.patternData.rowATypes[i] = this.patternData.rowATypes[i]
                it.patternData.rowDpadTypes[i] = this.patternData.rowDpadTypes[i]
            }
            it.disableTailEnd.set(this.disableTailEnd.get())
            it.beatsPerBlock = this.beatsPerBlock
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("beatsPerBlock", this.beatsPerBlock)
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        this.beatsPerBlock = obj.getFloat("beatsPerBlock", 0.5f)
    }
}