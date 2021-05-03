package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.border.SolidBorder
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.element.RectElement
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.pane.EditorPane

open class LongTrackPane(val allTracksPane: AllTracksPane, val hasContent: Boolean) : Pane() {
    
    val editorPane: EditorPane = allTracksPane.editorPane
    val trackView: TrackView = allTracksPane.trackView
    val editor: Editor = editorPane.editor
    
    val contentBgColor: Var<Color> = Var(Color(0f, 0f, 0f, 0f))
    val showContentBorder: Var<Boolean> = Var(false)
    val contentSection: Pane
    val sidePanel: SidePanel
    
    init {
        val sidebarSection = Pane().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.width.bind { allTracksPane.sidebarWidth.use() }
        }
        addChild(sidebarSection)
        sidePanel = SidePanel(editorPane)
        sidebarSection += sidePanel
        contentSection = Pane().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.x.bind { sidebarSection.bounds.x.use() + sidebarSection.bounds.width.use() }
            this.bounds.width.bind {
                (parent.use()?.let { p -> p.contentZone.width.use() } ?: 0f) - allTracksPane.sidebarWidth.use()
            }
            this.doClipping.set(true)
            
            this.border.bind { if (showContentBorder.use()) Insets(0f, 2f, 0f, 0f) else Insets.ZERO }
            this.borderStyle.set(SolidBorder().apply { this.color.bind { editorPane.palette.trackPaneBorder.use() }})
        }
        this.contentSection += RectElement().apply { this.color.bind { contentBgColor.use() } }
        if (hasContent)
            addChild(contentSection)
        
    }
}