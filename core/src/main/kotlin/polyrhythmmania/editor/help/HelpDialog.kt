package polyrhythmmania.editor.help

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import paintbox.Paintbox
import paintbox.font.Markup
import paintbox.font.PaintboxFont
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.Palette
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.editor.pane.dialog.EditorDialog


class HelpDialog(editorPane: EditorPane) : EditorDialog(editorPane), Disposable {

    val helpData: HelpData = EditorHelpData.createHelpData()
    val scrollPane: ScrollPane
    private val renderer: DocumentRenderer by lazy { HelpDocRenderer(this) }

    init {
        this.titleLabel.text.bind { 
            val currentDocTitle = helpData.currentDocument.use()?.title
            Localization.getValue(currentDocTitle ?: "editor.dialog.help.title")
        }

        scrollPane = ScrollPane().apply {
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(0f, 0f, 0f, 0f))
            this.vBar.blockIncrement.set(64f)
        }
        contentPane.addChild(scrollPane)
        
        bottomPane.addChild(Button("").apply {
            Anchor.BottomRight.configure(this)
            this.bounds.width.bind { bounds.height.useF() }
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

    override fun dispose() {
        renderer.disposeQuietly()
    }
}


class HelpDocRenderer(val dialog: HelpDialog) : DocumentRenderer() {
    private val main: PRManiaGame get() = dialog.main
    private val palette: Palette = dialog.editorPane.palette
    private val markup: Markup = palette.markupInstantiatorDesc
    private val defaultFont: PaintboxFont = markup.defaultTextRun.font
    
    private val cachedTextures: MutableMap<String, Texture> = mutableMapOf()
    
    private fun getTexture(path: String): Texture {
        return cachedTextures.getOrPut(path) {
            val file = Gdx.files.internal(path)
            (if (!file.exists()) {
                Paintbox.LOGGER.warn("Missing help doc texture: $path")
                Texture("textures/help/missing.png")
            } else {
                Texture(file)
            }).apply {
                setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
        }
    }

    override fun dispose() {
        cachedTextures.values.toList().forEach { it.disposeQuietly() }
        cachedTextures.clear()
    }

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
            is LayerVbox -> {
                Pane().apply {
                    val rowSpacing = 8f
                    val vbox = VBox().apply {
                        Anchor.TopLeft.configure(this)
                        this.spacing.set(rowSpacing)
                    }
                    this += vbox
                    vbox.temporarilyDisableLayouts {
                        layer.layers.forEach { innerLayer -> 
                            vbox += renderLayer(helpData, innerLayer)
                        }
                    }
                    vbox.sizeHeightToChildren(1f)
                    this.bounds.height.set(vbox.bounds.height.get())
                }
                
            }
            is LayerCol2 -> {
                Pane().apply { 
                    val colSpacing = 8f
                    val left = VBox().apply { 
                        Anchor.TopLeft.configure(this)
                        this.bindWidthToParent(multiplier = 0.5f, adjust = -colSpacing)
                    }
                    this += left
                    val right = VBox().apply {
                        Anchor.TopRight.configure(this)
                        this.bindWidthToParent(multiplier = 0.5f, adjust = -colSpacing)
                    }
                    this += right
                    
                    if (layer.left != null) {
                        left += renderLayer(helpData, layer.left)
                    }
                    left.sizeHeightToChildren(1f)
                    if (layer.right != null) {
                        right += renderLayer(helpData, layer.right)
                    }
                    right.sizeHeightToChildren(1f)
                    this.bounds.height.set(maxOf(left.bounds.height.get(), right.bounds.height.get()))
                }
            }
            is LayerCol3 ->  {
                Pane().apply {
                    val colSpacing = 8f
                    val third = 1f / 3
                    val left = VBox().apply {
                        Anchor.TopLeft.configure(this)
                        this.bindWidthToParent(multiplier = third, adjust = -colSpacing)
                    }
                    this += left
                    val mid = VBox().apply {
                        Anchor.TopCentre.configure(this)
                        this.bindWidthToParent(multiplier = third, adjust = -colSpacing)
                    }
                    this += mid
                    val right = VBox().apply {
                        Anchor.TopRight.configure(this)
                        this.bindWidthToParent(multiplier = third, adjust = -colSpacing)
                    }
                    this += right

                    if (layer.left != null) {
                        left += renderLayer(helpData, layer.left)
                    }
                    left.sizeHeightToChildren(1f)
                    if (layer.mid != null) {
                        mid += renderLayer(helpData, layer.mid)
                    }
                    mid.sizeHeightToChildren(1f)
                    if (layer.right != null) {
                        right += renderLayer(helpData, layer.right)
                    }
                    right.sizeHeightToChildren(1f)
                    this.bounds.height.set(maxOf(left.bounds.height.get(), mid.bounds.height.get(), right.bounds.height.get()))
                }
            }
            is LayerCol3Asymmetric -> {
                Pane().apply {
                    val third = 1f / 3
                    val colSpacing = 8f
                    val left = VBox().apply {
                        Anchor.TopLeft.configure(this)
                        this.bindWidthToParent(multiplier = if (layer.moreLeft) (third * 2) else third, adjust = -colSpacing)
                    }
                    this += left
                    val right = VBox().apply {
                        Anchor.TopRight.configure(this)
                        this.bindWidthToParent(multiplier = if (!layer.moreLeft) (third * 2) else third, adjust = -colSpacing)
                    }
                    this += right

                    if (layer.left != null) {
                        left += renderLayer(helpData, layer.left)
                    }
                    left.sizeHeightToChildren(1f)
                    if (layer.right != null) {
                        right += renderLayer(helpData, layer.right)
                    }
                    right.sizeHeightToChildren(1f)
                    this.bounds.height.set(maxOf(left.bounds.height.get(), right.bounds.height.get()))
                }
            }
            is LayerButton -> {
                Button(binding = { Localization.getVar(layer.text).use() }, font = defaultFont).apply { 
                    with(dialog) {
                        this@apply.applyDialogStyleContent()
                    }
                    this.bounds.height.set(48f)
                    this.setOnAction { 
                        if (layer.external) {
                            Gdx.net.openURI(layer.link)
                        } else {
                            val layerID = layer.link
                            val doc = helpData.documents[layerID]
                            if (doc != null)
                                helpData.goToDoc(doc)
                        }
                    }
                }
            }
            is LayerImage -> {
                ImageNode(TextureRegion(getTexture(layer.texturePath))).apply { 
                    this.bounds.height.set(layer.allocatedHeight)
                }
            }
        }
    }
}
