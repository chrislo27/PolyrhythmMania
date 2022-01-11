package polyrhythmmania.editor.pane

import com.badlogic.gdx.utils.Align
import paintbox.font.TextAlign
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
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
            this.padding.set(Insets(1f, 1f, 4f, 4f))
        }
        this += bg
        
        val parent: UIElement = bg
        
        label = TextLabel("", font = editor.main.mainFont).apply {
            this.renderAlign.set(Align.left)
            this.textAlign.set(TextAlign.LEFT)
            this.doXCompression.set(true)
            this.bgPadding.set(Insets.ZERO)
            this.text.bind { editorPane.statusBarMsg.use() }
            this.textColor.bind { editorPane.palette.statusTextColor.use() }
            this.markup.set(editorPane.palette.markupStatusBar)
        }
        parent += label
    }
}