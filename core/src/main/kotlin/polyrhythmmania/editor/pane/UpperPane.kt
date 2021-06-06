package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.Color
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.element.RectElement
import polyrhythmmania.editor.Editor


class UpperPane(val editorPane: EditorPane) : Pane() {

    val editor: Editor = editorPane.editor
    
    val previewPane: PreviewPane
    val toolbar: Toolbar
    val instantiatorPane: InstantiatorPane
    
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
        
        val rightPane = Pane().apply { 
            Anchor.TopRight.configure(this)
            this.bindHeightToParent()
            this.bounds.width.bind {
                (parent.use()?.let { p -> p.contentZone.width.use() } ?: 0f) - previewPane.bounds.width.use()
            }
            this.padding.set(Insets(2f))
        }
        mainSection += rightPane

        instantiatorPane = InstantiatorPane(this)
        rightPane += instantiatorPane

        toolbar = Toolbar(this)
        toolbarBacking += toolbar
    }
    
}