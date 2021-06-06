package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import polyrhythmmania.editor.pane.EditorPane


class SidePanel(val editorPane: EditorPane) : Pane() {
    
    val sidebarBgColor: Var<Color> = Var(Color(0f, 0f, 0f, 0f))
    val titleText: Var<String> = Var("")
    val sidebarSection: Pane
    val titleLabel: TextLabel
    
    init {
        sidebarSection = Pane().apply {
            Anchor.TopLeft.configure(this)
            this.border.set(Insets(0f, 2f, 0f, 2f))
            this.borderStyle.set(SolidBorder().apply { this.color.bind { editorPane.palette.trackPaneBorder.use() }})
            this.doClipping.set(true)
            this.addChild(RectElement().apply { this.color.bind { sidebarBgColor.use() } })
        }
        addChild(sidebarSection)

        titleLabel = TextLabel("", font = editorPane.palette.sidePanelFont).apply {
            this.text.bind { titleText.use() }
            this.textColor.bind { editorPane.palette.trackPaneTextColor.use() }
            this.bgPadding.set(Insets.ZERO)
            this.padding.set(Insets(6f, 2f, 2f, 2f))
            this.renderAlign.set(Align.topLeft)
            this.textAlign.set(TextAlign.LEFT)
        }
        sidebarSection += titleLabel
    }
}