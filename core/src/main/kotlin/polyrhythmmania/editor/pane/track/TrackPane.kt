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
import polyrhythmmania.editor.track.Track


open class TrackPane(val allTracksPane: AllTracksPane) : Pane() {
    
    val editorPane: EditorPane = allTracksPane.editorPane
    val trackView: TrackView = allTracksPane.trackView
    val editor: Editor = editorPane.editor
    
    val sidebarBgColor: Var<Color> = Var(Color(0f, 0f, 0f, 0f))
    val contentBgColor: Var<Color> = Var(Color(0f, 0f, 0f, 0f))
    val titleText: Var<String> = Var("")
    val sidebarSection: Pane
    val contentSection: Pane
    val titleLabel: TextLabel
    
    init {
        this.border.set(Insets(0f, 2f, 0f, 0f))
        this.borderStyle.set(SolidBorder().apply { this.color.bind { editorPane.palette.trackPaneBorder.use() }})
        
        sidebarSection = Pane().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.width.bind { allTracksPane.sidebarWidth.use() }
            this.border.set(Insets(0f, 0f, 0f, 2f))
            this.borderStyle.set(SolidBorder().apply { this.color.bind { editorPane.palette.trackPaneBorder.use() }})
            this.doClipping.set(true)
        }
        this.sidebarSection += RectElement().apply { this.color.bind { sidebarBgColor.use() } }
        addChild(sidebarSection)
        contentSection = Pane().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.x.bind { sidebarSection.bounds.x.use() + sidebarSection.bounds.width.use() }
            this.bounds.width.bind {
                (parent.use()?.let { p -> p.contentZone.width.use() } ?: 0f) - allTracksPane.sidebarWidth.use()
            }
            this.doClipping.set(true)
        }
        this.contentSection += RectElement().apply { this.color.bind { contentBgColor.use() } }
        addChild(contentSection)

        
        titleLabel = TextLabel("", font = editorPane.main.mainFontBordered).apply { 
            this.text.bind { titleText.use() }
            this.textColor.bind { editorPane.palette.trackPaneText.use() }
            this.bgPadding.set(0f)
            this.padding.set(Insets(6f, 2f, 2f, 2f))
            this.renderAlign.set(Align.topLeft)
            this.textAlign.set(TextAlign.LEFT)
        }
        sidebarSection += titleLabel
    }
}