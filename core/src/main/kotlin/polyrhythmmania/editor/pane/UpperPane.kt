package polyrhythmmania.editor.pane

import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.UIElement
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.border.SolidBorder
import polyrhythmmania.editor.Editor


class UpperPane(val editorPane: EditorPane) : Pane() {

    val editor: Editor = editorPane.editor
    
    val previewPane: PreviewPane
    val toolbar: Toolbar
    
    init {
        
        val toolbarBacking: UIElement = Pane().apply {
            Anchor.BottomLeft.configure(this)
            this.bounds.height.set(40f)
        }
        this += toolbarBacking
        
        val mainSection: UIElement = Pane().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.height.bind { (parent.use()?.bounds?.height?.use() ?: 0f) - toolbarBacking.bounds.height.use() }
        }
        this += mainSection
        
        previewPane = PreviewPane(this).apply {
//            Anchor.TopCentre.configure(this)
            Anchor.TopLeft.configure(this)
            this.bounds.width.bind { this@apply.bounds.height.use() * (16f / 9f) }
        }
        mainSection += previewPane


        toolbar = Toolbar(this)
        toolbarBacking += toolbar
    }
    
}