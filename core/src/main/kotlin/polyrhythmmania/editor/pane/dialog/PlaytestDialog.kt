package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
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
import paintbox.util.DecimalFormats
import polyrhythmmania.Localization
import polyrhythmmania.editor.PlayState
import polyrhythmmania.editor.pane.EditorPane


class PlaytestDialog(editorPane: EditorPane) : EditorDialog(editorPane, mergeTopAndContent = true) {

    private val previewTr: TextureRegion = editor.previewTextureRegion
    
    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.playtest.title").use() }
        this.bgElement.bindWidthToParent()
        this.bgElement.bindHeightToParent()

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
            this.spacing.set(8f)
        }
        contentPane += vbox
        vbox.temporarilyDisableLayouts { 
            vbox += ImageNode(previewTr).apply {
                Anchor.TopCentre.configure(this)
                val borderSize = 2f
                this.bindHeightToParent(adjust = borderSize * 2 - (32f + 8f * 2))
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
        
        contentPane += TextLabel(editor.inputKeymapKeyboard.toKeyboardString(detailedDpad = true, withNewline = false),
                font = editorPane.palette.rodinDialogFont).apply { 
            Anchor.BottomCentre.configure(this)
            this.bounds.height.set(32f)
            this.textColor.set(Color.WHITE)
            this.renderAlign.set(Align.center)
        }

        val hbox = HBox().apply {
            Anchor.BottomCentre.configure(this)
            this.align.set(HBox.Align.CENTRE)
            this.spacing.set(16f)
            this.bounds.width.set(800f)
        }
        bottomPane.addChild(hbox)
        hbox += TextLabel(binding = {
            Localization.getVar("editor.dialog.playtest.currentBeat", Var.bind {
                listOf(DecimalFormats.format("0.000", editor.engineBeat.use()), DecimalFormats.format("0.000", editor.playbackStart.use()))
            }).use()
        }, font = editorPane.palette.musicDialogFont).apply {
            this.bounds.width.set(600f)
            this.textColor.set(Color.WHITE)
            this.renderAlign.set(Align.center)
            this.markup.set(editorPane.palette.markup)
        }
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
        editor.setPlaytestingEnabled(false)
        editor.changePlayState(PlayState.STOPPED)
        editor.renderer.forceUseOfMainFramebuffer.set(false)
    }

    override fun canCloseDialog(): Boolean {
        return true
    }
}
