package polyrhythmmania.editor.pane.dialog.help

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.control.*
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.editor.pane.dialog.EditorDialog
import kotlin.math.max


class HelpDialog(editorPane: EditorPane) : EditorDialog(editorPane) {
    

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.help.title").use() }

        bottomPane.addChild(Button("").apply {
            Anchor.BottomRight.configure(this)
            this.bounds.width.bind { bounds.height.use() }
            this.applyDialogStyleBottom()
            this.setOnAction {
                editorPane.closeDialog()
            }
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_linear")["x"])).apply {
                this.tint.bind { editorPane.palette.toolbarIconToolNeutralTint.use() }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("common.close")))
        })

        val scrollPane: ScrollPane = ScrollPane().apply { 
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(0f, 0f, 0f, 0f))
        }
        contentPane.addChild(scrollPane)
        
        val pane = Pane()
        val vbox = VBox().apply { 
            this.spacing.set(8f)
        }
        pane += vbox
        val blockHeight = 64f
        
        
        vbox.temporarilyDisableLayouts { 
//            vbox += createCheckbox("editor.dialog.settings.detailedMarkerUndo", "editor.dialog.settings.detailedMarkerUndo.tooltip", settings.editorDetailedMarkerUndo)
            vbox += Pane().apply { 
                this.bounds.height.set(32f)
            }
        }
        val last = vbox.children.last()
        vbox.bounds.height.set(last.bounds.y.getOrCompute() + last.bounds.height.getOrCompute())
        
        pane.bounds.height.bind { 
            max(vbox.bounds.height.use(), pane.parent.use()?.bounds?.height?.use() ?: 300f)
        }
        scrollPane.setContent(pane)

//        val hbox = HBox().apply {
//            Anchor.BottomCentre.configure(this)
//            this.align.set(HBox.Align.CENTRE)
//            this.spacing.set(16f)
//            this.bounds.width.set(700f)
//        }
//        bottomPane.addChild(hbox)
    }
}