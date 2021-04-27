package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import io.github.chrislo27.paintbox.ui.ImageNode
import io.github.chrislo27.paintbox.ui.ImageRenderingMode
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.UIElement
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.border.SolidBorder
import io.github.chrislo27.paintbox.ui.element.RectElement
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
        
        parent += ImageNode(TextureRegion(editor.frameBuffer.colorBufferTexture).also { tr -> 
            tr.flip(false, true)
        }, renderingMode = ImageRenderingMode.FULL)
    }
}