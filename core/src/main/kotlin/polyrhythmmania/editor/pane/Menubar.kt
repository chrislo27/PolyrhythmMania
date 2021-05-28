package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import io.github.chrislo27.paintbox.packing.PackedSheet
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.ImageNode
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.element.RectElement
import io.github.chrislo27.paintbox.ui.layout.HBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor


class Menubar(val editorPane: EditorPane) : Pane() {
    
    val editor: Editor = editorPane.editor
    
    val ioNew: Button
    val ioOpen: Button
    val ioSave: Button
    val undoButton: Button
    val redoButton: Button
    
    init {
        fun separator(): Pane {
            return Pane().apply { 
                this.bounds.width.set(2f)
                this.addChild(RectElement(Color.LIGHT_GRAY))
            }
        }
        
        val leftBox: HBox = HBox().apply { 
            this.spacing.set(4f)
            this.align.set(HBox.Align.LEFT)
            this.bounds.width.set((32f + this.spacing.getOrCompute()) * 6)
        }
        this.addChild(leftBox)
        
        ioNew = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_new"]))
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.new")))
        }
        ioOpen = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_open"]))
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.open")))
        }
        ioSave = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_save"]))
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.save")))
        }
        undoButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            val active: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_undo"])
            val inactive: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_undo_white"])
            this += ImageNode(null).apply {
                this.textureRegion.bind {
                    if (apparentDisabledState.use()) inactive else active
                }
                this.tint.bind {
                    if (apparentDisabledState.use()) Color.GRAY else Color.WHITE
                }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.undo")))
            this.disabled.bind { editor.undoStackSize.use() <= 0 }
            this.setOnAction { 
                editor.attemptUndo()
            }
        }
        redoButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            val active: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_undo"]).apply { flip(true, false) }
            val inactive: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_undo_white"]).apply { flip(true, false) }
            this += ImageNode(null).apply {
                this.textureRegion.bind {
                    if (apparentDisabledState.use()) inactive else active
                }
                this.tint.bind {
                    if (apparentDisabledState.use()) Color.GRAY else Color.WHITE
                }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.redo")))
            this.disabled.bind { editor.redoStackSize.use() <= 0 }
            this.setOnAction {
                editor.attemptRedo()
            }
        }
        
        leftBox.temporarilyDisableLayouts {
            leftBox += ioNew
            leftBox += ioOpen
            leftBox += ioSave
            leftBox += separator()
            leftBox += undoButton
            leftBox += redoButton
        }
    }
}