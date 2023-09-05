package polyrhythmmania.editor.help

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import paintbox.Paintbox
import paintbox.binding.ContextBinding
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
import paintbox.ui.control.Button
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.Palette
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.editor.pane.dialog.EditorDialog
import polyrhythmmania.ui.PRManiaSkins


class HelpDialog(editorPane: EditorPane) : EditorDialog(editorPane), Disposable {

    val helpData: HelpData = EditorHelpData.createHelpData()
    val scrollPane: ScrollPane
    private val renderer: DocumentRenderer by lazy { HelpDocRenderer(this) }

    init {
        this.titleLabel.text.bind { 
            val currentDocTitle = helpData.currentDocument.use()?.title
            if (currentDocTitle != null) {
                Localization.getValue(currentDocTitle)
            } else {
                Localization.getValue("editor.dialog.help.title")
            }
        }

        scrollPane = ScrollPane().apply {
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBar.unitIncrement.set(64f)
            this.vBar.blockIncrement.set(100f)
            this.vBar.skinID.set(PRManiaSkins.SCROLLBAR_SKIN)
        }
        contentPane.addChild(scrollPane)

        
        val rightHbox = HBox().apply {
            Anchor.BottomRight.configure(this)
            this.align.set(HBox.Align.RIGHT)
            this.spacing.set(12f)
            this.bounds.width.set(300f)
        }
        bottomPane.addChild(rightHbox)
        rightHbox.temporarilyDisableLayouts { 
            rightHbox += Button("").apply {
                this.bounds.width.set(48f)
                this.applyDialogStyleBottom()
                this.setOnAction {
                    scrollPane.vBar.setValue(0f)
                }
                this.disabled.bind { scrollPane.vBar.value.use() <= 0f }
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_help")["arrow_right"])).apply {
                    this.rotation.set(90f)
                    this.tint.bind { editorPane.palette.toolbarIconToolNeutralTint.use() }
                }
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.help.goToTop")))
            }
            rightHbox += Button("").apply {
                this.bounds.width.set(48f)
                this.applyDialogStyleBottom()
                this.setOnAction {
                    attemptClose()
                }
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_linear")["x"])).apply {
                    this.tint.bind { editorPane.palette.toolbarIconToolNeutralTint.use() }
                }
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("common.close")))
                this.setOnRightClick {
                    if (Paintbox.debugMode.get()) { // DEBUG
                        val doc = helpData.currentDocument.getOrCompute()
                        if (doc != null) {
                            scrollPane.setContent(renderer.renderDocument(helpData, doc))
                        } else {
                            scrollPane.setContent(RectElement(Color.RED))
                        }
                    }
                }
            }
        }


        helpData.currentDocument.addListener { d ->
            val doc = d.getOrCompute()
            val newContent = if (doc != null) {
                renderer.renderDocument(helpData, doc)
            } else {
                RectElement(Color.RED)
            }
            if (scrollPane.getContent() != newContent) {
                scrollPane.vBar.setValue(0f)
                scrollPane.setContent(newContent)
            }
        }

        val leftHbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.align.set(HBox.Align.LEFT)
            this.spacing.set(12f)
            this.bounds.width.set(700f)
        }
        bottomPane.addChild(leftHbox)
        leftHbox.temporarilyDisableLayouts { 
            leftHbox += Button("").apply {
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
            leftHbox += Button("").apply {
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
            leftHbox += Button("").apply {
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

    override fun canCloseDialog(): Boolean {
        return true
    }

    override fun onCloseDialog() {
    }
}


class HelpDocRenderer(val dialog: HelpDialog) : DocumentRenderer() {
    private val main: PRManiaGame get() = dialog.main
    private val palette: Palette = dialog.editorPane.palette
    private val markup: Markup = palette.markupHelp
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
                    this.renderAlign.set(layer.renderAlign)
                    this.textAlign.set(layer.textAlign)
                    this.textColor.set(Color.WHITE)
                    this.doLineWrapping.set(true)
                    this.padding.set(Insets(4f, 4f, 8f, 8f) + (layer.padding ?: Insets.ZERO))
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
                    val leftProp = layer.leftProportion
                    val rightProp = 1f - leftProp
                    val left = VBox().apply { 
                        Anchor.TopLeft.configure(this)
                        this.bindWidthToParent(multiplier = leftProp, adjust = -colSpacing)
                    }
                    this += left
                    val right = VBox().apply {
                        Anchor.TopRight.configure(this)
                        this.bindWidthToParent(multiplier = rightProp, adjust = -colSpacing)
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
                val textBinding: ContextBinding<String> = if (layer is LayerButtonWithNewIndicator) {
                    {
                        (if (layer.newIndicator.value.use())
                                (Localization.getVar("common.newIndicator").use() + " ")
                        else "") + Localization.getVar(layer.text).use()
                    }
                } else {
                    {
                        Localization.getVar(layer.text).use()
                    }
                }
                Button(binding = textBinding, font = defaultFont).apply {
                    with(dialog) {
                        this@apply.applyDialogStyleContent()
                    }
                    this.bounds.height.set(48f)
                    this.setOnAction {
                        if (layer is LayerButtonWithNewIndicator) {
                            layer.newIndicator.value.set(false)
                            main.settings.persist()
                        }
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
