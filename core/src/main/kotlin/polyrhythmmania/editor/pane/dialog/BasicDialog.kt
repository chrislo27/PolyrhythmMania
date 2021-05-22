package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.Tooltip
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.border.SolidBorder
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.element.RectElement
import io.github.chrislo27.paintbox.util.gdxutils.disposeQuietly
import io.github.chrislo27.paintbox.util.gdxutils.grey
import polyrhythmmania.editor.pane.EditorPane


class BasicDialog(editorPane: EditorPane) : DialogPane(editorPane) {

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
            this.borderStyle.set(SolidBorder(Color.WHITE))
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
        val tbPaneHeight = 76f + tbPanePadding + tbPaneBorder + tbPaneMargin
        topPane = Pane().apply { 
            Anchor.TopLeft.configure(this)
            this.bounds.height.set(tbPaneHeight)
            this.padding.set(Insets(0f, tbPanePadding, 0f, 0f))
            this.border.set(Insets(0f, tbPaneBorder, 0f, 0f))
            this.borderStyle.set(SolidBorder(Color.WHITE))
            this.margin.set(Insets(0f, tbPaneMargin, 0f, 0f))
        }
        bg.addChild(topPane)
        bottomPane = Pane().apply {
            Anchor.BottomLeft.configure(this)
            this.bounds.height.set(tbPaneHeight)
            this.padding.set(Insets(tbPanePadding, 0f, 0f, 0f))
            this.border.set(Insets(tbPaneBorder, 0f, 0f, 0f))
            this.borderStyle.set(SolidBorder(Color.WHITE))
            this.margin.set(Insets(tbPaneMargin, 0f, 0f, 0f))
        }
        bg.addChild(bottomPane)
        contentPane = Pane().apply {
            Anchor.CentreLeft.configure(this)
            this.bindHeightToParent(-1 * tbPaneHeight * 2)
        }
        bg.addChild(contentPane)
        
//        // DEBUG zone test
//        topPane.addChild(RectElement(Color.RED))
//        contentPane.addChild(RectElement(Color.GREEN))
//        bottomPane.addChild(RectElement(Color.BLUE))
        
        titleLabel = TextLabel("Test title abcxyz", font = editorPane.main.fontEditorDialogTitle).apply {
            this.textAlign.set(TextAlign.LEFT)
            this.renderAlign.set(Align.left)
            this.textColor.set(Color.WHITE)
        }
        topPane.addChild(titleLabel)


        bottomPane.addChild(Button("X").apply { // FIXME remove
            Anchor.BottomLeft.configure(this)
            this.bounds.width.set(64f)
            this.bounds.height.set(64f)
            this.setOnAction {
                editorPane.closeDialog()
            }
            this.tooltipElement.set(Tooltip("Test tooltip. Closes this dialog."))
        })
        
        
    }

}