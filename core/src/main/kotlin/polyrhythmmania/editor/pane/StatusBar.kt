package polyrhythmmania.editor.pane

import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.UIElement
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.border.SolidBorder
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.element.RectElement
import polyrhythmmania.editor.Editor


class StatusBar(val editorPane: EditorPane) : Pane() {

    val editor: Editor = editorPane.editor
    
    val bg: UIElement
    val label: TextLabel
    
    init {
        bg = RectElement().apply {
            this.color.bind { editorPane.palette.statusBg.use() }
            this.border.set(Insets(1f, 0f, 0f, 0f))
            this.borderStyle.set(SolidBorder().apply {
                this.color.bind { editorPane.palette.statusBorder.use() }
            })
            this.padding.set(Insets(1f, 1f, 2f, 2f))
        }
        this += bg
        
        val parent: UIElement = bg
        
        label = TextLabel("", font = editor.main.mainFont).apply {
            this.renderAlign.set(Align.left)
            this.textAlign.set(TextAlign.LEFT)
            this.doXCompression.set(true)
            this.bgPadding.set(0f)
            this.text.bind { editorPane.statusBarMsg.use() }
            this.textColor.bind { editorPane.palette.statusTextColor.use() }
            this.markup.set(editorPane.palette.markup)
        }
        parent += label
    }
}