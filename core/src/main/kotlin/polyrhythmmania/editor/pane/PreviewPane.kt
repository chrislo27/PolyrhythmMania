package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.element.RectElement
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor


class PreviewPane(val upperPane: UpperPane) : Pane() {

    val editorPane: EditorPane = upperPane.editorPane
    val editor: Editor = upperPane.editor
    
    val imageNode: ImageNode
    
    init {
        val parent: UIElement = RectElement(Color(0f, 1f, 0f, 0f)).apply { 
            this.border.set(Insets(2f, 0f, 2f, 2f))
            this.borderStyle.set(SolidBorder().apply { 
                this.color.bind { editorPane.palette.previewPaneBorder.use() }
            })
        }
        this += parent
        
        imageNode = ImageNode(editor.previewTextureRegion,
                renderingMode = ImageRenderingMode.FULL)
        parent += imageNode
        
        imageNode += RectElement(Color(0f, 0f, 0f, 0.75f)).apply {
            this.bounds.width.set(20f)
            this.bounds.height.set(20f)
            this.padding.set(Insets(2f))
            this += ImageIcon(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["informational"])).apply {
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.preview.tooltip")))
            }
        }
    }
}