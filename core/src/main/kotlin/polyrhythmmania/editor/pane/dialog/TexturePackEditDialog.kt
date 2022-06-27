package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import net.lingala.zip4j.ZipFile
import paintbox.Paintbox
import paintbox.binding.BooleanVar
import paintbox.binding.IntVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.filechooser.FileExtFilter
import paintbox.filechooser.TinyFDWrapper
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.contextmenu.SeparatorMenuItem
import paintbox.ui.contextmenu.SimpleMenuItem
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.grey
import paintbox.util.gdxutils.openFileExplorer
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.container.TexPackSrcSelectorMenuPane
import polyrhythmmania.container.TexturePackSource
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.ui.PRManiaSkins
import polyrhythmmania.world.tileset.CustomTexturePack
import polyrhythmmania.world.tileset.StockTexturePacks
import polyrhythmmania.world.tileset.TexturePack
import polyrhythmmania.world.tileset.TilesetRegion
import java.io.File
import java.util.*
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import kotlin.concurrent.thread


class TexturePackEditDialog(
        editorPane: EditorPane, 
) : EditorDialog(editorPane) {

    companion object {
        val LIST_ENTRIES: List<ListEntry> = listOf(
                ListEntry.Category("terrain"),
                ListEntry.Region("cube_border"),
                ListEntry.Region("cube_border_platform"),
                ListEntry.Region("cube_border_z"),
                ListEntry.Region("cube_face_x"),
                ListEntry.Region("cube_face_y"),
                ListEntry.Region("cube_face_z"),
                ListEntry.Region("platform"),
                ListEntry.Region("platform_with_line"),
                ListEntry.Region("red_line"),
                
                ListEntry.Category("rods"),
                ListEntry.Region("rods_borders"),
                ListEntry.Region("rods_fill"),
                ListEntry.Region("explosion_0"),
                ListEntry.Region("explosion_1"),
                ListEntry.Region("explosion_2"),
                ListEntry.Region("explosion_3"),
                
                ListEntry.Category("pistons"),
                ListEntry.Region("piston_a"),
                ListEntry.Region("piston_a_extended"),
                ListEntry.Region("piston_a_extended_face_x"),
                ListEntry.Region("piston_a_extended_face_z"),
                ListEntry.Region("piston_a_partial"),
                ListEntry.Region("piston_a_partial_face_x"),
                ListEntry.Region("piston_a_partial_face_z"),
                ListEntry.Region("piston_dpad"),
                ListEntry.Region("piston_dpad_extended"),
                ListEntry.Region("piston_dpad_extended_face_x"),
                ListEntry.Region("piston_dpad_extended_face_z"),
                ListEntry.Region("piston_dpad_partial"),
                ListEntry.Region("piston_dpad_partial_face_x"),
                ListEntry.Region("piston_dpad_partial_face_z"),
                
                ListEntry.Category("signs"),
                ListEntry.Region("sign_a"),
                ListEntry.Region("sign_a_shadow"),
                ListEntry.Region("sign_dpad"),
                ListEntry.Region("sign_dpad_shadow"),
                ListEntry.Region("sign_bo"),
                ListEntry.Region("sign_bo_shadow"),
                ListEntry.Region("sign_ta"),
                ListEntry.Region("sign_ta_shadow"),
                ListEntry.Region("sign_n"),
                ListEntry.Region("sign_n_shadow"),
                ListEntry.Region("indicator_a"),
                ListEntry.Region("indicator_dpad"),
                ListEntry.Region("input_feedback_0"),
                ListEntry.Region("input_feedback_1"),
                ListEntry.Region("input_feedback_2"),
                
                ListEntry.Category("backgrounds"),
                ListEntry.Region("background_back"),
                ListEntry.Region("background_middle"),
                ListEntry.Region("background_fore"),
        )
        val DARK_GREY = Color().grey(0.31f)
    }
    
    sealed class ListEntry(val localizationKey: String) {
        class Category(val categoryID: String) : ListEntry("editor.dialog.texturePack.objectCategory.${categoryID}")
        class Region(val id: String) : ListEntry("editor.dialog.texturePack.object.${id}")
    }
    
    private enum class PreviewBg(val localizationKey: String) {
        CHECKERED("editor.dialog.texturePack.preview.background.checkered"),
        WHITE("editor.dialog.texturePack.preview.background.white"),
        GREY("editor.dialog.texturePack.preview.background.grey"),
        BLACK("editor.dialog.texturePack.preview.background.black"),
    }
    
    /**
     * 0-indexed.
     */
    val customPackIndex: IntVar = IntVar(0)
    val customPackFromContainer: Var<CustomTexturePack?> get() = editor.container.customTexturePacks[customPackIndex.get()]
    
    private val isFileChooserOpen: BooleanVar = BooleanVar(false)
    private val isMessageVisible: BooleanVar = BooleanVar(false)
    
    private val baseTexturePack: Var<TexturePack> = Var(StockTexturePacks.gba)
    private val customTexturePack: Var<CustomTexturePack> = Var(CustomTexturePack(UUID.randomUUID().toString(), baseTexturePack.getOrCompute().id))
    private val onTexturePackUpdated: BooleanVar = BooleanVar(false)
    private val currentEntry: Var<ListEntry.Region> = Var(LIST_ENTRIES.first { it is ListEntry.Region } as ListEntry.Region)
    
    private val currentMsg: Var<String> = Var("")
    
    private val previewPane: PreviewPane
    private val texPackSelectorPane: TexPackSrcSelectorMenuPane

    init {
        baseTexturePack.addListener { tp ->
            customTexturePack.getOrCompute().fallbackID = tp.getOrCompute().id
        }
        
        this.titleLabel.text.bind {
            Localization.getVar("editor.dialog.texturePack.title", Var { listOf(customPackIndex.use() + 1) }).use()
        }


        val hideMainContent = BooleanVar {
            isFileChooserOpen.use() || isMessageVisible.use()
        }
        val contentPaneContainer = Pane().apply {
            this.visible.bind { !hideMainContent.use() }
        }
        contentPane += contentPaneContainer
        val bottomPaneContainer = Pane().apply {
            this.visible.bind { !hideMainContent.use() }
        }
        bottomPane += bottomPaneContainer
        
        bottomPaneContainer.addChild(Button("").apply {
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
        
        // Close file chooser msg
        contentPane += Pane().apply { 
            this.visible.bind { isFileChooserOpen.use() }
            this += TextLabel(binding = { Localization.getVar("common.closeFileChooser").use() }).apply {
                this.markup.set(editorPane.palette.markup)
                this.textColor.set(Color.WHITE.cpy())
                this.renderAlign.set(Align.center)
                this.textAlign.set(TextAlign.CENTRE)
            }
        }
        // Generic msg
        contentPane += Pane().apply { 
            this.visible.bind { !isFileChooserOpen.use() && isMessageVisible.use() }
            this += TextLabel(binding = { currentMsg.use() }).apply {
                this.markup.set(editorPane.palette.markup)
                this.textColor.set(Color.WHITE.cpy())
                this.renderAlign.set(Align.center)
                this.textAlign.set(TextAlign.CENTRE)
                this.bindHeightToParent(adjust = -48f)
            }
            this += Button(binding = { Localization.getVar("common.ok").use() }, font = editorPane.palette.musicDialogFont).apply { 
                this.bounds.height.set(40f)
                this.bounds.width.set(150f)
                Anchor.BottomCentre.configure(this)
                this.applyDialogStyleContent()
                this.setOnAction { 
                    isMessageVisible.set(false)
                }
            }
        }

        val scrollPaneWidthProportion = 0.4f
        val scrollPane: ScrollPane = ScrollPane().apply {
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.bindWidthToParent(multiplier = scrollPaneWidthProportion)
            this.vBar.blockIncrement.set(64f)
            this.vBar.skinID.set(PRManiaSkins.SCROLLBAR_SKIN)
        }
        contentPaneContainer.addChild(scrollPane)

        val listVbox = VBox().apply {
            this.spacing.set(1f)
        }

        listVbox.temporarilyDisableLayouts {
            val toggleGroup = ToggleGroup()
            var firstRegionEntry = true
            LIST_ENTRIES.forEach { listEntry -> 
                listVbox += when (listEntry) {
                    is ListEntry.Category -> {
                        TextLabel(binding = { Localization.getVar(listEntry.localizationKey).use() },
                                font = main.fontEditorDialogTitle).apply {
                            this.textColor.set(Color.WHITE.cpy())
                            this.margin.set(Insets(0f, 0f, 8f, 8f))
                            this.padding.set(this.padding.getOrCompute().copy(bottom = 4f))
                            this.setScaleXY(0.5f)
                            this.renderAlign.set(Align.bottomLeft)
                            this.bounds.height.set(54f)
                        }
                    }
                    is ListEntry.Region -> {
                        val textBinding: ReadOnlyVar<String> = Var.bind {
                            onTexturePackUpdated.use()
                            (if (customTexturePack.use().getOrNull(listEntry.id) != null)
                                "[font=rodin color=CYAN]★[] " else "") +
                                    Localization.getVar(listEntry.localizationKey).use() + 
                                    "[color=LIGHT_GRAY scale=0.8f]\n${listEntry.id}[]"
                        }
                        RadioButton(binding = { textBinding.use() }, font = editorPane.palette.musicDialogFont).apply {
                            this.textLabel.margin.set(Insets(0f, 0f, 0f, 8f))
                            this.textLabel.markup.set(editorPane.palette.markup)
                            this.imageNode.padding.set(Insets(6f, 6f, 6f, 10f))
                            this.color.set(Color.WHITE.cpy())
                            this.bounds.height.set(48f)
                            toggleGroup.addToggle(this)
                            this.onSelected = {
                                currentEntry.set(listEntry)
                            }
                            if (firstRegionEntry) {
                                firstRegionEntry = false
                                selectedState.set(true)
                            }
                        }
                    }
                }
            }
        }
        listVbox.sizeHeightToChildren(300f)
        scrollPane.setContent(listVbox)

        previewPane = PreviewPane().apply {
            Anchor.TopRight.configure(this)
            this.bindWidthToParent(multiplier = 0.6f, adjust = -8f)
            this.margin.set(Insets(0f, 0f, 10f, 0f))
        }
        contentPaneContainer.addChild(previewPane)
        
        val bottomLeftHbox = HBox().apply {
            this.spacing.set(8f)
            this.bindWidthToParent(multiplier = scrollPaneWidthProportion, adjust = -8f)
        }
        bottomLeftHbox.temporarilyDisableLayouts {
            bottomLeftHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_new"]))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.new")))
                this.setOnAction {
                    editor.attemptOpenGenericContextMenu(ContextMenu().also { ctxmenu ->
                        ctxmenu.defaultWidth.set(300f)
                        ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("editor.dialog.texturePack.button.new.confirm"), editorPane.palette.markup))
                        ctxmenu.addMenuItem(SeparatorMenuItem())
                        ctxmenu.addMenuItem(SimpleMenuItem.create(Localization.getValue("editor.dialog.texturePack.button.new.confirm.yes"),
                                editorPane.palette.markup).apply {
                            this.onAction = {
                                val old = customTexturePack.getOrCompute()
                                val oldTextures = old.getAllUniqueTextures()
                                
                                customTexturePack.set(CustomTexturePack(UUID.randomUUID().toString(), baseTexturePack.getOrCompute().id))
                                syncThisCustomPackWithContainer()
                                
                                Gdx.app.postRunnable { 
                                    oldTextures.forEach { it.disposeQuietly() }
                                }
                                
                                onTexturePackUpdated.invert()
                            }
                        })
                        ctxmenu.addMenuItem(SimpleMenuItem.create(Localization.getValue("editor.dialog.texturePack.button.new.confirm.no"),
                                editorPane.palette.markup).apply {
                            this.onAction = {}
                        })
                    })
                }
            }
            bottomLeftHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_open"]))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.loadAll")))
                this.setOnAction {
                    fileChooserSelectLoadAllFile { file ->
                        var zipFile: ZipFile? = null
                        try {
                            zipFile = ZipFile(file)
                            val readResult = CustomTexturePack.readFromStream(zipFile)
                            val newPack = readResult.createAndLoadTextures()
                            
                            val oldPack = customTexturePack.getOrCompute()
                            val oldTextures = oldPack.getAllUniqueTextures()
                            customTexturePack.set(newPack)
                            syncThisCustomPackWithContainer()
                            Gdx.app.postRunnable {
                                oldTextures.forEach { it.disposeQuietly() }
                            }
                            
                            pushMessage(Localization.getValue("editor.dialog.texturePack.message.loadAll.success", readResult.formatVersion.toString()))
                            onTexturePackUpdated.invert()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            pushMessage(Localization.getValue("editor.dialog.texturePack.message.loadAll.error", e.javaClass.name))
                        } finally {
                            zipFile?.close()
                        }
                    }
                }
            }
            bottomLeftHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_save"]))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.saveAll")))
                this.setOnAction {
                    fileChooserSelectSaveAllFile { file ->
                        try {
                            file.createNewFile()
                            file.outputStream().use { fos ->
                                ZipOutputStream(fos).use { zip ->
                                    customTexturePack.getOrCompute().writeToOutputStream(zip)
                                }
                            }
                            pushMessage(Localization.getValue("editor.dialog.texturePack.message.saveAll.success"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            pushMessage(Localization.getValue("editor.dialog.texturePack.message.saveAll.error", e.javaClass.name))
                        }
                    }
                }
            }
            bottomLeftHbox += RectElement(DARK_GREY).apply {
                this.bounds.width.set(2f)
            }
            bottomLeftHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_export"]))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.exportAllTextures")))
                this.setOnAction {
                    val ctp = customTexturePack.getOrCompute()
                    if (ctp.isEmpty()) {
                        pushMessage(Localization.getValue("editor.dialog.texturePack.message.exportedCustom.none"))
                    } else {
                        try {
                            val outputFolder: File = PRMania.MAIN_FOLDER.resolve("texpack_output/")
                            outputFolder.deleteRecursively()
                            if (!outputFolder.exists()) {
                                outputFolder.mkdirs()
                            }

                            val tgaWriter = ImageIO.getImageWritersByFormatName("TGA").next()
                            try {
                                ctp.getAllTilesetRegions().forEach { region ->
                                    val outputFile = outputFolder.resolve("${region.id}.tga")
                                    outputFile.createNewFile()
                                    outputFile.outputStream().use { fos ->
                                        CustomTexturePack.writeTextureAsTGA(region.texture, fos, tgaWriter)
                                    }
                                }
                            } finally {
                                tgaWriter.dispose()
                            }

                            pushMessage(Localization.getValue("editor.dialog.texturePack.message.exportedCustom.success", outputFolder.absolutePath.replace("\\", "\\\\")))
                            Gdx.app.postRunnable {
                                Gdx.net.openFileExplorer(outputFolder)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            pushMessage(Localization.getValue("editor.dialog.texturePack.message.exportedTextures.error", e.javaClass.name))
                        }
                    }
                }
            }
            bottomLeftHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_import"]))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.importTextures")))
                this.setOnAction {
                    fileChooserSelectTexturesForImport { files ->
                        // Will be on the gdx thread
                        importTextures(files)
                    }
                }
            }
            bottomLeftHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_export_base"]))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.exportAllBaseTextures")))
                this.setOnAction {
                    try {
                        val outputFolder: File = PRMania.MAIN_FOLDER.resolve("texpack_output/")
                        outputFolder.deleteRecursively()
                        if (!outputFolder.exists()) {
                            outputFolder.mkdirs()
                        }
                        
                        val baseID = customTexturePack.getOrCompute().fallbackID
                        val fileTypes = listOf("tga", "png")
                        CustomTexturePack.ALLOWED_LIST.forEach { regionID ->
                            for (fileType in fileTypes) {
                                val fh = Gdx.files.internal("textures/world/${baseID}/parts/${regionID}.${fileType}")
                                if (!fh.exists()) continue
                                val outputFile: File = outputFolder.resolve("${regionID}.${fileType}")
                                outputFile.createNewFile()
                                fh.copyTo(FileHandle(outputFile))
                            }
                        }
                        
                        pushMessage(Localization.getValue("editor.dialog.texturePack.message.exportedBase.success", outputFolder.absolutePath.replace("\\", "\\\\")))
                        Gdx.app.postRunnable { 
                            Gdx.net.openFileExplorer(outputFolder)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        pushMessage(Localization.getValue("editor.dialog.texturePack.message.exportedTextures.error", e.javaClass.name))
                    }
                }
            }
        }
        bottomPaneContainer.addChild(bottomLeftHbox)
        bottomPaneContainer.addChild(RectElement(DARK_GREY).apply { 
            this.bounds.width.set(2f)
            this.bounds.x.bind { (parent.use()?.bounds?.width?.use() ?: 0f) * scrollPaneWidthProportion - 8f }
        })


        val bottomRightHbox = HBox().apply {
            this.spacing.set(8f)
            this.bindWidthToParent(multiplierBinding = { 1f - scrollPaneWidthProportion }, adjustBinding = { -8f * 2 - bounds.height.use() })
            this.bounds.x.bind { (parent.use()?.bounds?.width?.use() ?: 0f) * 0.4f + 8f }
        }
        bottomRightHbox.temporarilyDisableLayouts {
            bottomRightHbox += VBox().apply {
                this.spacing.set(2f)
                this.bounds.width.set(340f)
                
                this += TextLabel(binding = { Localization.getVar("editor.dialog.texturePack.stock").use() },
                        font = editorPane.palette.musicDialogFont).apply {
                    this.bindHeightToParent(multiplier = 0.4f, adjust = -1f)
                    this.markup.set(editorPane.palette.markup)
                    this.textColor.set(Color.WHITE.cpy())
                }
                val basePackSource: TexturePackSource = StockTexturePacks.getTexturePackSource(baseTexturePack.getOrCompute()) ?: TexturePackSource.StockGBA
                texPackSelectorPane = TexPackSrcSelectorMenuPane(editorPane, basePackSource,
                        legalValues = TexturePackSource.VALUES_NON_CUSTOM) { src ->
                    baseTexturePack.set(StockTexturePacks.getPackFromSource(src) ?: StockTexturePacks.gba)
                    onTexturePackUpdated.invert()
                }.apply {
                    this.bindHeightToParent(multiplier = 0.6f, adjust = -1f)
                    this.bindWidthToParent(multiplier = 0.9f)
                }
                this += texPackSelectorPane
            }
        }
        bottomPaneContainer.addChild(bottomRightHbox)
    }
    
    
    fun prepareShow(): TexturePackEditDialog {
        val currentCustom = customPackFromContainer.getOrCompute()
        if (currentCustom != null) {
            customTexturePack.set(currentCustom)
            baseTexturePack.set(StockTexturePacks.allPacksByID[currentCustom.fallbackID] ?: StockTexturePacks.gba)
            texPackSelectorPane.combobox.selectedItem.set(StockTexturePacks.getTexturePackSource(baseTexturePack.getOrCompute()) ?: TexturePackSource.StockGBA)
            
            onTexturePackUpdated.invert()
        }
        return this
    }

    override fun canCloseDialog(): Boolean {
        return !isFileChooserOpen.get()
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
        syncThisCustomPackWithContainer()
    }
    
    fun pushMessage(msg: String) {
        isMessageVisible.set(true)
        currentMsg.set(msg)
    }
    
    fun syncThisCustomPackWithContainer() {
        val ctp = customTexturePack.getOrCompute()
        customPackFromContainer.set(ctp)
        editor.container.setTexturePackFromSource()
    }
    
    private fun importTextures(files: List<File>) {
        val ctp = customTexturePack.getOrCompute()
        val basePack = StockTexturePacks.allPacksByIDWithDeprecations[ctp.fallbackID] ?: StockTexturePacks.gba
        val orderedFiles = files.sortedBy { it.name }
        try {
            val oldTextureSet = ctp.getAllUniqueTextures()
            val acceptedFiles: MutableList<File> = mutableListOf()
            val toApply = mutableListOf<TilesetRegion>()
            for (regionID in CustomTexturePack.ALLOWED_LIST) {
                val toImportFile = orderedFiles.firstOrNull { it.nameWithoutExtension == regionID } ?: continue
                Paintbox.LOGGER.debug("Importing texture: ${toImportFile.name}")

                // Load in as a texture and tileset region.
                val baseRegion: TilesetRegion = basePack[regionID]
                val newTexture = Texture(FileHandle(toImportFile))
                val newRegion = TilesetRegion(baseRegion.id, TextureRegion(newTexture), baseRegion.spacing)
                toApply += newRegion

                acceptedFiles += toImportFile
            }
            val numberImported = acceptedFiles.size
            
            toApply.forEach { newRegion ->
                ctp.remove(ctp.getOrNull(newRegion.id))
                ctp.add(newRegion)
            }
            
            // Dispose the textures that have stopped being used
            val texturesRemoved = oldTextureSet - ctp.getAllUniqueTextures().toSet() 
            if (texturesRemoved.isNotEmpty()) {
                texturesRemoved.forEach { it.disposeQuietly() }
            }

            if (numberImported < files.size) {
                pushMessage(Localization.getValue("editor.dialog.texturePack.message.importTextures.someIgnored",
                        numberImported, files.size - numberImported,
                        (files - acceptedFiles.toSet()).joinToString(separator = ", ") { it.name }))
            } else {
                pushMessage(Localization.getValue("editor.dialog.texturePack.message.importTextures.success", numberImported))
            }
            
            onTexturePackUpdated.invert() // Updates the listing
            syncThisCustomPackWithContainer()
        } catch (e: Exception) {
            e.printStackTrace()
            pushMessage(Localization.getValue("editor.dialog.texturePack.message.importTextures.error", e.javaClass.name))
        }
    }

    fun fileChooserSelectTexturesForImport(action: (List<File>) -> Unit) {
        isFileChooserOpen.set(true)
        editorPane.main.restoreForExternalDialog { completionCallback ->
            thread(isDaemon = true) {
                val filter = FileExtFilter(Localization.getValue("fileChooser.texturePack.filter.textureFiles"),
                        listOf("*.tga", "*.png")).copyWithExtensionsInDesc()
                TinyFDWrapper.openMultipleFiles(Localization.getValue("fileChooser.texturePack.importCustomTextures"),
                        main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_TEXPACK_IMPORT_TEX_TO_DIR)
                                ?: main.getDefaultDirectory(),
                        filter) { files: List<File>? ->
                    completionCallback()

                    if (files != null && files.isNotEmpty()) {
                        Gdx.app.postRunnable {
                            action(files)
                        }
                        main.persistDirectory(PreferenceKeys.FILE_CHOOSER_TEXPACK_IMPORT_TEX_TO_DIR, files.first().parentFile)
                    }

                    Gdx.app.postRunnable {
                        isFileChooserOpen.set(false)
                    }
                }
            }
        }
    }

    fun fileChooserSelectSaveAllFile(action: (File) -> Unit) {
        isFileChooserOpen.set(true)
        editorPane.main.restoreForExternalDialog { completionCallback ->
            thread(isDaemon = true) {
                val filter = FileExtFilter(Localization.getValue("fileChooser.texturePack.filter.texturePackFile"),
                        listOf("*.zip")).copyWithExtensionsInDesc()
                TinyFDWrapper.saveFile(Localization.getValue("fileChooser.texturePack.exportEntire"),
                        (main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_TEXPACK_ENTIRE)
                                ?: main.getDefaultDirectory()).resolve("texturepack.zip"), filter) { file: File? ->
                    completionCallback()

                    if (file != null) {
                        val fileWithCorrectExt = if (!file.extension.equals("zip", ignoreCase = true))
                            (File(file.absolutePath + ".zip"))
                        else file
                        
                        Gdx.app.postRunnable {
                            action(fileWithCorrectExt)
                        }
                        main.persistDirectory(PreferenceKeys.FILE_CHOOSER_TEXPACK_ENTIRE, file.parentFile)
                    }

                    Gdx.app.postRunnable {
                        isFileChooserOpen.set(false)
                    }
                }
            }
        }
    }

    fun fileChooserSelectLoadAllFile(action: (File) -> Unit) {
        isFileChooserOpen.set(true)
        editorPane.main.restoreForExternalDialog { completionCallback ->
            thread(isDaemon = true) {
                val filter = FileExtFilter(Localization.getValue("fileChooser.texturePack.filter.texturePackFile"),
                        listOf("*.zip")).copyWithExtensionsInDesc()
                TinyFDWrapper.openFile(Localization.getValue("fileChooser.texturePack.importEntire"),
                        main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_TEXPACK_ENTIRE)
                                ?: main.getDefaultDirectory(), filter) { file: File? ->
                    completionCallback()

                    if (file != null) {
                        Gdx.app.postRunnable {
                            action(file)
                        }
                        main.persistDirectory(PreferenceKeys.FILE_CHOOSER_TEXPACK_ENTIRE, file.parentFile)
                    }

                    Gdx.app.postRunnable {
                        isFileChooserOpen.set(false)
                    }
                }
            }
        }
    }
    
    inner class PreviewPane : Pane() {
        
        private val updateVar: BooleanVar = BooleanVar(false)
        val vbox: VBox = VBox().apply {
            this.spacing.set(2f)
        }

        val title: TextLabel = TextLabel(binding = { Localization.getVar(currentEntry.use().localizationKey).use() }).apply {
            this.markup.set(editorPane.palette.markupInstantiatorSummary)
            this.textColor.set(Color(1f, 1f, 1f, 1f))
            this.bounds.height.set(40f)
        }
        val filename: TextLabel
        
        val removeButton: Button
        
        init {
            this += vbox

            val filenameTextVar = Localization.getVar("editor.dialog.texturePack.preview.filename", Var { listOf(currentEntry.use().id) })
            val filenameTextTooltipVar = Localization.getVar("editor.dialog.texturePack.preview.filename.tooltip", Var { listOf(currentEntry.use().id) })
            filename = TextLabel(binding = { filenameTextVar.use() }).apply {
                this.markup.set(editorPane.palette.markup)
                this.textColor.set(Color.LIGHT_GRAY.cpy())
                this.bounds.height.set(26f)
                this.tooltipElement.set(editorPane.createDefaultTooltip(filenameTextTooltipVar))
            }
            
            val buttonBar = HBox().apply {
                this.spacing.set(8f)
                this.bounds.height.set(36f)
            }
            
            removeButton = Button("").apply {
                this.applyDialogStyleContent()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(2f))
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_trash"])).also { img -> 
                    img.tint.bind {
                        if (apparentDisabledState.use()) Color(0.5f, 0.5f, 0.5f, 0.25f) else Color(1f, 1f, 1f, 1f)
                    }
                }
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.removeTexture")))
                this.setOnAction {
                    val ctp = customTexturePack.getOrCompute()
                    val current = currentEntry.getOrCompute()
                    val oldTextureSet = ctp.getAllUniqueTextures()
                    
                    ctp.remove(ctp.getOrNull(current.id))

                    // Dispose the textures that have stopped being used
                    val texturesRemoved = oldTextureSet - ctp.getAllUniqueTextures().toSet()
                    if (texturesRemoved.isNotEmpty()) {
                        texturesRemoved.forEach { it.disposeQuietly() }
                    }
                    
                    onTexturePackUpdated.invert()
                }
            }
            
            buttonBar.temporarilyDisableLayouts {
                fun separator(): RectElement {
                    return RectElement(DARK_GREY).apply {
                        this.bounds.width.set(6f)
                        this.margin.set(Insets(0f, 0f, 2f, 2f))
                    }
                }
                
                buttonBar += removeButton
                buttonBar += separator()
                
                val filterToggleGroup = ToggleGroup()
                buttonBar += TextLabel(binding = { Localization.getVar("editor.dialog.texturePack.button.filtering").use() }).apply {
                    this.markup.set(editorPane.palette.markup)
                    this.renderAlign.set(Align.right)
                    this.bounds.width.set(100f)
                    this.textColor.set(Color.WHITE.cpy())
                }

                buttonBar += RadioButton(binding = { Localization.getVar("editor.dialog.texturePack.button.filtering.nearest").use() },
                        font = editorPane.palette.musicDialogFont).apply {
                    this.bounds.width.set(120f)
                    this.textLabel.markup.set(editorPane.palette.markup)
                    val tint: ReadOnlyVar<Color> = Var {
                        if (apparentDisabledState.use()) Color.DARK_GRAY.cpy() else Color.WHITE.cpy()
                    }
                    this.color.bind { tint.use() }
                    this.imageNode.padding.set(Insets(3f))
                    filterToggleGroup.addToggle(this)
                    this.disabled.bind {
                        updateVar.use()
                        val ctp = customTexturePack.use()
                        val current = currentEntry.use()
                        ctp.getOrNull(current.id) == null
                    }
                    this.setOnAction { 
                        selectedState.set(true)
                        val entry = currentEntry.getOrCompute()
                        val filter = Texture.TextureFilter.Nearest
                        customTexturePack.getOrCompute().getOrNull(entry.id)?.texture?.setFilter(filter, filter)
                    }
                    this.selectedState.set(true)
                    updateVar.addListener {
                        val entry = currentEntry.getOrCompute()
                        val region = customTexturePack.getOrCompute().getOrNull(entry.id) ?: baseTexturePack.getOrCompute().getOrNull(entry.id)
                        if (region != null) {
                            this.selectedState.set(region.texture.magFilter == Texture.TextureFilter.Nearest)
                        }
                    }
                }
                buttonBar += RadioButton(binding = { Localization.getVar("editor.dialog.texturePack.button.filtering.linear").use() },
                        font = editorPane.palette.musicDialogFont).apply {
                    this.bounds.width.set(120f)
                    this.textLabel.markup.set(editorPane.palette.markup)
                    val tint: ReadOnlyVar<Color> = Var {
                        if (apparentDisabledState.use()) Color.DARK_GRAY.cpy() else Color.WHITE.cpy()
                    }
                    this.color.bind { tint.use() }
                    this.imageNode.padding.set(Insets(3f))
                    filterToggleGroup.addToggle(this)
                    this.disabled.bind {
                        updateVar.use()
                        val ctp = customTexturePack.use()
                        val current = currentEntry.use()
                        ctp.getOrNull(current.id) == null
                    }
                    this.setOnAction {
                        selectedState.set(true)
                        val entry = currentEntry.getOrCompute()
                        val filter = Texture.TextureFilter.Linear
                        customTexturePack.getOrCompute().getOrNull(entry.id)?.texture?.setFilter(filter, filter)
                    }
                    updateVar.addListener {
                        val entry = currentEntry.getOrCompute()
                        val region = customTexturePack.getOrCompute().getOrNull(entry.id) ?: baseTexturePack.getOrCompute().getOrNull(entry.id)
                        if (region != null) {
                            this.selectedState.set(region.texture.magFilter == Texture.TextureFilter.Linear)
                        }
                    }
                }
                buttonBar += Button(binding = { Localization.getVar("editor.dialog.texturePack.button.filtering.applyToAll").use() }).apply {
                    this.applyDialogStyleContent()
                    this.markup.set(editorPane.palette.markup)
                    this.bounds.width.set(125f)
                    this.padding.set(Insets(2f))
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.filtering.applyToAll.tooltip")))
                    this.setOnAction {
                        val entry = currentEntry.getOrCompute()
                        val ctp = customTexturePack.getOrCompute()
                        val currentFilter = ctp.getOrNull(entry.id)?.texture?.minFilter

                        if (currentFilter != null) {
                            ctp.getAllTilesetRegions().forEach { r ->
                                r.texture.setFilter(currentFilter, currentFilter)
                            }
                        }

                        onTexturePackUpdated.invert()
                    }
                    this.disabled.bind {
                        updateVar.use()
                        val ctp = customTexturePack.use()
                        val current = currentEntry.use()
                        ctp.getOrNull(current.id) == null
                    }
                }
                
            }
            
            
            val showBox = Pane().apply { 
                Anchor.TopCentre.configure(this)
                this.bounds.width.set(256f)
                this.bounds.height.set(256f)
            }
            val transparencyNode = ImageNode(null, ImageRenderingMode.FULL).also { im ->
                im.textureRegion.sideEffecting(TextureRegion(PRManiaGame.instance.colourPickerTransparencyGrid)) { tr ->
                    tr?.setRegion(0, 0, im.bounds.width.use().toInt(), im.bounds.height.use().toInt())
                    tr
                }
            }
            val solidColorNode = RectElement(Color.CLEAR)
            val regionImageNode = ImageNode(binding = {
                updateVar.use()
                val entry = currentEntry.use()
                customTexturePack.use().getOrNull(entry.id) ?: baseTexturePack.use().getOrNull(entry.id)
            }, renderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO)
            
            fun setPreviewBg(new: PreviewBg) {
                when (new) {
                    PreviewBg.CHECKERED -> solidColorNode.color.set(Color.CLEAR)
                    PreviewBg.WHITE -> solidColorNode.color.set(Color.WHITE)
                    PreviewBg.GREY -> solidColorNode.color.set(Color.GRAY)
                    PreviewBg.BLACK -> solidColorNode.color.set(Color.BLACK)
                }
            }
            
            showBox += transparencyNode
            transparencyNode += solidColorNode
            solidColorNode += regionImageNode
            
            val bgOptionsBox = HBox().apply { 
                this.spacing.set(8f)
                this.bounds.height.set(32f)
            }
            bgOptionsBox.temporarilyDisableLayouts { 
                bgOptionsBox += TextLabel(binding = { Localization.getVar("editor.dialog.texturePack.preview.background").use() }).apply {
                    this.markup.set(editorPane.palette.markup)
                    this.renderAlign.set(Align.right)
                    this.bounds.width.set(200f)
                    this.textColor.set(Color.WHITE.cpy())
                }
                bgOptionsBox += ComboBox(PreviewBg.values().toList(), PreviewBg.CHECKERED, font = editorPane.palette.musicDialogFont).apply {
                    this.bounds.width.set(200f)
                    this.itemStringConverter.set(StringConverter { 
                        Localization.getValue(it.localizationKey)
                    })
                    this.onItemSelected = { item ->
                        setPreviewBg(item)
                    }
                    setPreviewBg(this.selectedItem.getOrCompute())
                }
            }


            fun separator(): RectElement {
                return RectElement(DARK_GREY).apply {
                    this.bounds.height.set(6f)
                    this.margin.set(Insets(2f, 2f, 0f, 0f))
                }
            }
            vbox.temporarilyDisableLayouts {
                vbox += title
                vbox += filename
                vbox += separator()
                vbox += buttonBar
                vbox += separator()
                vbox += showBox
                vbox += separator().apply { this.color.set(Color.CLEAR) }
                vbox += bgOptionsBox
            }
        }
        
        init {
            onTexturePackUpdated.addListener { updateVar.invert() }
            currentEntry.addListener { updateVar.invert() }
            
            updateVar.addListener {
                update()
            }
            Gdx.app.postRunnable {
                update()
            }
        }
        
        fun update() {
            val ctp = customTexturePack.getOrCompute()
            val current = currentEntry.getOrCompute()
            
            removeButton.disabled.set(ctp.getOrNull(current.id) == null)
        }
    }

}
