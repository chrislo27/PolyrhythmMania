package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.pane.EditorPane

open class LongTrackPane(val allTracksPane: AllTracksPane, val hasContent: Boolean) : Pane() {
    
    val editorPane: EditorPane = allTracksPane.editorPane
    val trackView: TrackView = allTracksPane.trackView
    val editor: Editor = editorPane.editor
    
    val contentBgColor: Var<Color> = Var(Color(0f, 0f, 0f, 0f))
    val showContentBorder: Var<Boolean> = Var(false)
    val contentSection: UIElement
    val sidePanel: SidePanel
    
    init {
        val sidebarSection = Pane().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.width.bind { allTracksPane.sidebarWidth.use() }
        }
        addChild(sidebarSection)
        sidePanel = SidePanel(editorPane)
        sidebarSection += sidePanel
        contentSection = RectElement().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.x.bind { sidebarSection.bounds.x.use() + sidebarSection.bounds.width.use() }
            this.bounds.width.bind {
                (parent.use()?.let { p -> p.contentZone.width.use() } ?: 0f) - allTracksPane.sidebarWidth.use()
            }
            this.doClipping.set(true)
            
            this.border.bind { if (showContentBorder.use()) Insets(0f, 2f, 0f, 0f) else Insets.ZERO }
            this.borderStyle.set(SolidBorder().apply { this.color.bind { editorPane.palette.trackPaneBorder.use() }})

            this.color.bind { contentBgColor.use() }
        }
        if (hasContent)
            addChild(contentSection)
        
    }
}