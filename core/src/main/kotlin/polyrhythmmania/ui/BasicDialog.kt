package polyrhythmmania.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.border.SolidBorder
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.control.ButtonSkin
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.element.RectElement
import io.github.chrislo27.paintbox.util.gdxutils.grey
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.pane.EditorPane


open class BasicDialog(val main: PRManiaGame) : DialogPane() {

    val titleLabel: TextLabel
    val topPane: Pane
    val contentPane: Pane
    val bottomPane: Pane

    init {
        val backOverlay = RectElement(Color().grey(0.1f, 0.6f))
        this.addChild(backOverlay)
        val bg = RectElement(Color().grey(0f, 0.75f)).apply {
            Anchor.Centre.configure(this)
            this.bounds.width.set(1280f)
            this.bounds.height.set(720f)
            this.margin.set(Insets(8f))
            this.border.set(Insets(4f))
            this.borderStyle.set(SolidBorder(Color.WHITE).apply { 
                this.roundedCorners.set(true)
            })
            this.padding.set(Insets(40f))

//            this.addChild(RectElement(Color.CLEAR).apply { // DEBUG for padding check
//                this.border.set(Insets(1f))
//                this.borderStyle.set(SolidBorder(Color.WHITE))
//            })
        }
        backOverlay.addChild(bg)

        val tbPanePadding = 8f
        val tbPaneBorder = 4f
        val tbPaneMargin = 8f
        val topPaneHeight = 76f + tbPanePadding + tbPaneBorder + tbPaneMargin
        val botPaneHeight = 48f + tbPanePadding + tbPaneBorder + tbPaneMargin
        topPane = Pane().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.height.set(topPaneHeight)
            this.padding.set(Insets(0f, tbPanePadding, 0f, 0f))
            this.border.set(Insets(0f, tbPaneBorder, 0f, 0f))
            this.borderStyle.set(SolidBorder(Color.WHITE))
            this.margin.set(Insets(0f, tbPaneMargin, 0f, 0f))
        }
        bg.addChild(topPane)
        bottomPane = Pane().apply {
            Anchor.BottomLeft.configure(this)
            this.bounds.height.set(botPaneHeight)
            this.padding.set(Insets(tbPanePadding, 0f, 0f, 0f))
            this.border.set(Insets(tbPaneBorder, 0f, 0f, 0f))
            this.borderStyle.set(SolidBorder(Color.WHITE))
            this.margin.set(Insets(tbPaneMargin, 0f, 0f, 0f))
        }
        bg.addChild(bottomPane)
        contentPane = Pane().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.y.set(topPaneHeight)
            this.bindHeightToParent(-1 * (topPaneHeight + botPaneHeight))
        }
        bg.addChild(contentPane)

//        // DEBUG zone test
//        topPane.addChild(RectElement(Color.RED))
//        contentPane.addChild(RectElement(Color.GREEN))
//        bottomPane.addChild(RectElement(Color.BLUE))

        titleLabel = TextLabel("", font = main.fontEditorDialogTitle).apply {
            this.textAlign.set(TextAlign.LEFT)
            this.renderAlign.set(Align.left)
            this.textColor.set(Color.WHITE)
        }
        topPane.addChild(titleLabel)
        
    }
    
    fun Button.applyDialogStyleBottom() {
        val skin = (this.skin.getOrCompute() as? ButtonSkin) ?: return
        skin.roundedRadius.set(8)
        this.padding.set(Insets(8f))
    }
    
    fun Button.applyDialogStyleContent() {
        val skin = (this.skin.getOrCompute() as? ButtonSkin) ?: return
        skin.roundedRadius.set(4)
        this.padding.set(Insets(6f))
    }

}