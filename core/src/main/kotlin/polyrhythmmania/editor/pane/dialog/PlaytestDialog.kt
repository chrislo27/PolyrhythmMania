package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.Button
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.PlayState
import polyrhythmmania.editor.pane.EditorPane


class PlaytestDialog(editorPane: EditorPane) : EditorDialog(editorPane, mergeTopAndContent = true) {

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.playtest.title").use() }

        bottomPane.addChild(Button("").apply {
            Anchor.BottomRight.configure(this)
            this.bindWidthToSelfHeight()
            this.applyDialogStyleBottom()
            this.setOnAction {
                attemptClose()
            }
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_linear")["x"])).apply {
                this.tint.bind { editorPane.palette.toolbarIconToolNeutralTint.use() }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("common.close")))
        })
        
        val vbox = VBox().apply { 
            Anchor.TopCentre.configure(this)
            this.bindHeightToParent(adjust = -40f)
            this.spacing.set(4f)
        }
        contentPane += vbox
        vbox.temporarilyDisableLayouts { 
            vbox += ImageNode(editor.previewTextureRegion).apply {
                Anchor.TopCentre.configure(this)
                val borderSize = 2f
                this.bindHeightToParent(multiplier = 0.9f, adjust = borderSize * 2)
                this.bounds.width.bind { (bounds.height.use() - borderSize * 2) * 16f / 9f + borderSize * 2 }
                this.border.set(Insets(borderSize))
                this.borderStyle.set(SolidBorder(Color.WHITE).apply {
                    this.roundedCorners.set(true)
                })
            }
            vbox += editorPane.toolbar.createPlaybackButtonSet().apply {
                Anchor.TopCentre.configure(this)
                this.bounds.height.set(32f)
            }
        }
        
        contentPane += TextLabel(editor.inputKeymapKeyboard.toKeyboardString(true, false), font = editorPane.palette.rodinDialogFont).apply { 
            Anchor.BottomCentre.configure(this)
            this.bounds.height.set(32f)
            this.textColor.set(Color.WHITE)
            this.renderAlign.set(Align.center)
        }

        val hbox = HBox().apply {
            Anchor.BottomCentre.configure(this)
            this.align.set(HBox.Align.CENTRE)
            this.spacing.set(16f)
            this.bounds.width.set(700f)
        }
        bottomPane.addChild(hbox)
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
        editor.setPlaytestingEnabled(false)
        editor.changePlayState(PlayState.STOPPED)
    }

    override fun canCloseDialog(): Boolean {
        return true
    }
}