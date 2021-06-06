package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.ui.ImageNode
import paintbox.ui.ImageRenderingMode
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.element.RectElement
import polyrhythmmania.editor.Editor


class PreviewPane(val upperPane: UpperPane) : Pane() {

    val editorPane: EditorPane = upperPane.editorPane
    val editor: Editor = upperPane.editor
    
    init {
        val parent: UIElement = RectElement(Color(0f, 1f, 0f, 1f)).apply { 
            this.border.set(Insets(2f, 0f, 2f, 2f))
            this.borderStyle.set(SolidBorder().apply { 
                this.color.bind { editorPane.palette.previewPaneBorder.use() }
            })
        }
        this += parent
        
        parent += ImageNode(TextureRegion(editor.previewFrameBuffer.colorBufferTexture).also { tr -> 
            tr.flip(false, true)
        }, renderingMode = ImageRenderingMode.FULL)
    }
}