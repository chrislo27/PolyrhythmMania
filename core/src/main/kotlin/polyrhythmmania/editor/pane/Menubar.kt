package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import io.github.chrislo27.paintbox.packing.PackedSheet
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.ImageNode
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.control.ButtonSkin
import io.github.chrislo27.paintbox.ui.layout.HBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor


class Menubar(val editorPane: EditorPane) : Pane() {
    
    val editor: Editor = editorPane.editor
    
    init {
        val ioBox: HBox = HBox().apply { 
            this.spacing.set(4f)
            this.align.set(HBox.Align.LEFT)
            this.bounds.width.set((32f + this.spacing.getOrCompute()) * 3)
        }
        this.addChild(ioBox)
        ioBox.temporarilyDisableLayouts {
            ioBox += Button("").apply {
                this.padding.set(Insets.ZERO)
                this.bounds.width.set(32f)
                this.skinID.set(EditorSkins.BUTTON)
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_new"])).apply {
//                this.tint.bind { editorPane.palette.menubarIconTint.use() }
                }
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.new")))
            }
            ioBox += Button("").apply {
                this.padding.set(Insets.ZERO)
                this.bounds.width.set(32f)
                this.skinID.set(EditorSkins.BUTTON)
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_open"])).apply {
//                this.tint.bind { editorPane.palette.menubarIconTint.use() }
                }
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.open")))
            }
            ioBox += Button("").apply {
                this.padding.set(Insets.ZERO)
                this.bounds.width.set(32f)
                this.skinID.set(EditorSkins.BUTTON)
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_save"])).apply {
//                this.tint.bind { editorPane.palette.menubarIconTint.use() }
                }
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.save")))
            }
        }
    }
}