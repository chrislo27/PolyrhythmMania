package polyrhythmmania.editor.pane

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.util.gdxutils.isAltDown
import paintbox.util.gdxutils.isControlDown
import paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor


class Menubar(val editorPane: EditorPane) : Pane() {
    
    val editor: Editor = editorPane.editor
    
    val ioNew: Button
    val ioOpen: Button
    val ioSave: Button
    val undoButton: Button
    val redoButton: Button
    
    val settingsButton: Button
    val exitButton: Button
    
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
        val rightBox: HBox = HBox().apply {
            Anchor.TopRight.configure(this)
            this.spacing.set(4f)
            this.align.set(HBox.Align.RIGHT)
            this.bounds.width.set((32f + this.spacing.getOrCompute()) * 3)
        }
        this.addChild(rightBox)
        
        ioNew = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_new"]))
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.new")))
            this.setOnAction {
                editor.attemptNewLevel()
            }
        }
        ioOpen = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_open"]))
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.open")))
            this.setOnAction {
                editor.attemptLoad(null)
            }
        }
        ioSave = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_save"]))
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.save")))
            this.setOnAction { 
                val control = Gdx.input.isControlDown()
                val alt = Gdx.input.isAltDown()
                val shift = Gdx.input.isShiftDown()
                editor.attemptSave(forceSaveAs = !control && alt && !shift)
            }
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
//                this.tint.bind {
//                    if (apparentDisabledState.use()) Color.GRAY else Color.WHITE
//                }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.redo")))
            this.disabled.bind { editor.redoStackSize.use() <= 0 }
            this.setOnAction {
                editor.attemptRedo()
            }
        }
        
        settingsButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["settings"])).apply { 
                this.tint.bind { editorPane.palette.menubarIconTint.use() }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.settings")))
            this.setOnAction {
                editor.attemptOpenSettingsDialog()
            }
        }
        exitButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_exit"]))
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.exit")))
            this.setOnAction {
                editor.attemptExitToTitle()
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
        rightBox.temporarilyDisableLayouts { 
            rightBox += settingsButton
            rightBox += separator()
            rightBox += exitButton
        }
    }
}