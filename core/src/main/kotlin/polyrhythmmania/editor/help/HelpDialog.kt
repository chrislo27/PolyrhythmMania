package polyrhythmmania.editor.help

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.font.Markup
import paintbox.font.TextAlign
import paintbox.font.TextBlock
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.Palette
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.editor.pane.dialog.EditorDialog


class HelpDialog(editorPane: EditorPane) : EditorDialog(editorPane) {

    val helpData: HelpData = EditorHelpData.createHelpData()
    val scrollPane: ScrollPane
    private val renderer: DocumentRenderer by lazy { HelpDocRenderer(this) }

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.help.title").use() }

        scrollPane = ScrollPane().apply {
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(0f, 0f, 0f, 0f))
        }
        contentPane.addChild(scrollPane)
        
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
            this.setOnRightClick {
                if (Paintbox.debugMode) { // DEBUG
                    val doc = helpData.currentDocument.getOrCompute()
                    if (doc != null) {
                        scrollPane.setContent(renderer.renderDocument(helpData, doc))
                    } else {
                        scrollPane.setContent(RectElement(Color.RED))
                    }
                }
            }
        })


        helpData.currentDocument.addListener { d ->
            val doc = d.getOrCompute()
            if (doc != null) {
                scrollPane.setContent(renderer.renderDocument(helpData, doc))
            } else {
                scrollPane.setContent(RectElement(Color.RED))
            }
        }

        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.align.set(HBox.Align.LEFT)
            this.spacing.set(12f)
            this.bounds.width.set(900f)
        }
        bottomPane.addChild(hbox)
        hbox.temporarilyDisableLayouts { 
            hbox += Button("").apply {
                this.bounds.width.set(48f)
                this.applyDialogStyleBottom()
                this.setOnAction {
                    helpData.backUp()
                }
                this.disabled.bind { !helpData.hasBack.use() }
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_help")["arrow_right"])).apply {
                    this.rotation.set(180f)
                    this.tint.bind { editorPane.palette.toolbarIconToolNeutralTint.use() }
                }
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.help.back")))
            }
            hbox += Button("").apply {
                this.bounds.width.set(48f)
                this.applyDialogStyleBottom()
                this.setOnAction {
                    helpData.forward()
                }
                this.disabled.bind { !helpData.hasForward.use() }
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_help")["arrow_right"])).apply {
                    this.tint.bind { editorPane.palette.toolbarIconToolNeutralTint.use() }
                }
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.help.forward")))
            }
            hbox += Button("").apply {
                this.bounds.width.set(48f)
                this.applyDialogStyleBottom()
                this.setOnAction {
                    helpData.goToRoot()
                }
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_help")["home"])).apply {
                    this.tint.bind { editorPane.palette.toolbarIconToolNeutralTint.use() }
                }
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.help.home")))
            }
        }
    }
    
    init {
        helpData.resetToRoot()
    }

    class HelpDocRenderer(val dialog: HelpDialog) : DocumentRenderer() {
        private val main: PRManiaGame get() = dialog.main
        private val palette: Palette = dialog.editorPane.palette
        private val markup: Markup = palette.markup

        override fun renderLayer(helpData: HelpData, layer: Layer): UIElement {
            return when (layer) {
                is LayerTitle -> {
                    TextLabel(binding = { Localization.getVar(layer.text).use() }, font = main.fontEditorDialogTitle).apply {
                        this.renderAlign.set(Align.center)
                        this.textAlign.set(TextAlign.CENTRE)
                        this.setScaleXY(0.75f)
                        this.textColor.set(Color.WHITE)
                        this.bounds.height.set(80f)
                        this.padding.set(Insets(2f, 2f, 8f, 8f))
                    }
                }
                is LayerParagraph -> {
                    TextLabel(binding = { Localization.getVar(layer.text).use() }).apply {
                        this.markup.set(this@HelpDocRenderer.markup)
                        this.renderAlign.set(Align.topLeft)
                        this.textAlign.set(TextAlign.LEFT)
                        this.textColor.set(Color.WHITE)
                        this.doLineWrapping.set(true)
                        this.padding.set(Insets(4f, 16f, 8f, 8f))
                        this.bounds.height.set(layer.allocatedHeight)
                    }
                }
            }
        }
    }
}