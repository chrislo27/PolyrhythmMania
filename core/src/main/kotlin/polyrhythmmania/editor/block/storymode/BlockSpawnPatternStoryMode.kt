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
import polyrhythmmania.world.entity.EntityRodDecor


class BlockSpawnPatternStoryMode(engine: Engine) : BlockSpawnPattern(engine) {

    private var xUnitsPerBeat: Float = EntityRodDecor.DEFAULT_X_UNITS_PER_BEAT
    
    init {
        this.width = 4f
        this.defaultText.set("Spawn Pttn (SM)")
    }
    
    override fun getBeatsPerBlock(): Float {
        return 1f / this.xUnitsPerBeat
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return super.createContextMenu(editor).also { ctxmenu ->
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(LabelMenuItem.create("X-units per beat (default ${EntityRodDecor.DEFAULT_X_UNITS_PER_BEAT})", editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(CustomMenuItem(
                HBox().apply {
                    this.bounds.height.set(32f)
                    this.spacing.set(4f)
                    this += RectElement(Color(0f, 0f, 0f, 1f)).apply {
                        this.border.set(Insets(1f))
                        this.borderStyle.set(SolidBorder(Color.WHITE))
                        this.padding.set(Insets(2f))
                        this += DecimalTextField(startingValue = xUnitsPerBeat, decimalFormat = DecimalFormats["0.0##"],
                            font = editor.editorPane.palette.musicDialogFont).apply {
                            this.minimumValue.set(0.25f)
                            this.textColor.set(Color(1f, 1f, 1f, 1f))

                            this.value.addListener {
                                xUnitsPerBeat = it.getOrCompute()
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
            it.xUnitsPerBeat = this.xUnitsPerBeat
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("xUnitsPerBeat", this.xUnitsPerBeat)
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        if (obj.get("beatsPerBlock") != null) {
            this.xUnitsPerBeat = 1f / obj.getFloat("beatsPerBlock", 0.5f).coerceAtLeast(0.25f)
        } else {
            this.xUnitsPerBeat = obj.getFloat("xUnitsPerBeat", EntityRodDecor.DEFAULT_X_UNITS_PER_BEAT).coerceAtLeast(0.25f)
        }
    }
}