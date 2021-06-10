package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.Tooltip
import paintbox.ui.area.Insets
import paintbox.ui.control.*
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.editor.pane.EditorPane
import kotlin.math.max


class SettingsDialog(editorPane: EditorPane) : EditorDialog(editorPane) {
    
    val settings: Settings = editorPane.main.settings

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.settings.title").use() }

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
        
        fun createCheckbox(text: String, tooltip: String?, bindingVar: Var<Boolean>): CheckBox {
            return CheckBox(binding = { Localization.getVar(text).use() }, font = editorPane.palette.musicDialogFont).apply { 
                this.textLabel.textColor.set(Color.WHITE)
                this.imageNode.tint.set(Color.WHITE)
                this.bounds.height.set(blockHeight)
                if (tooltip != null) {
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar(tooltip)))
                }
                this.imageNode.padding.set(Insets(8f))
                this.checkedState.set(bindingVar.getOrCompute())
                this.checkedState.addListener {
                    bindingVar.set(it.getOrCompute())
                }
            }
        }
        
        vbox.temporarilyDisableLayouts { 
            vbox += createCheckbox("editorSettings.detailedMarkerUndo", "editorSettings.detailedMarkerUndo.tooltip", settings.editorDetailedMarkerUndo)
        }
        vbox.sizeHeightToChildren(300f)
        
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