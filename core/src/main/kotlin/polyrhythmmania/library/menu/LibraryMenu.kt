package polyrhythmmania.library.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import net.lingala.zip4j.ZipFile
import paintbox.Paintbox
import paintbox.binding.BooleanVar
import paintbox.binding.ReadOnlyBooleanVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.ImageRenderingMode
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.control.TextLabel
import paintbox.ui.control.ToggleGroup
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.TinyFDWrapper
import paintbox.util.Version
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.openFileExplorer
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.container.Container
import polyrhythmmania.container.LevelMetadata
import polyrhythmmania.container.manifest.ExportStatistics
import polyrhythmmania.container.manifest.LibraryRelevantData
import polyrhythmmania.library.LevelEntry
import polyrhythmmania.screen.mainmenu.menu.MenuCollection
import polyrhythmmania.screen.mainmenu.menu.StandardMenu
import polyrhythmmania.ui.PRManiaSkins
import polyrhythmmania.util.TempFileUtils
import java.io.File
import java.util.*
import kotlin.concurrent.thread


class LibraryMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    private var workerThread: Thread? = null

    private val currentBanner: Var<Texture?> = Var(null) // Note: A Texture will not be disposed unless it is swapped out.
    private val toggleGroup: ToggleGroup = ToggleGroup()
    private val selectedLevelEntry: ReadOnlyVar<LevelEntry?> = Var {
        val at = toggleGroup.activeToggle.use()
        if (at != null && at.selectedState.useB()) {
            (at as? LibraryEntryButton)?.levelEntry
        } else null
    }
    private val levelList: Var<List<LevelEntryData>> = Var(emptyList())
    private val activeLevelList: Var<List<LevelEntryData>> = Var(emptyList())
    
    private val vbox: VBox
    private val contentPaneLeft: RectElement
    private val contentPaneRight: RectElement
    
    init {
        PRMania.DEFAULT_LEVELS_FOLDER // Invoke to mkdirs
        
        this.setSize(percentage = 0.975f)
        this.titleText.bind { Localization.getVar("mainMenu.library.title").use() }
        this.showLogo.set(false)
        this.contentPane.bounds.height.set(520f)
        this.contentPane.padding.set(Insets.ZERO)
        this.contentPane.color.set(Color(0f, 0f, 0f, 0f))
        
        contentPaneLeft = RectElement(grey).apply {
            Anchor.TopLeft.configure(this)
            contentPane += this
            this.bindWidthToParent(multiplier = 0.5f, adjust = 64f)
            this.padding.set(Insets(16f))
        }
        contentPaneRight = RectElement(grey).apply {
            Anchor.BottomRight.configure(this)
            contentPane += this
            this.bounds.height.bind { 
                // NOTE: the top part overflows its parent container, but the only element there is the level banner.
                (parent.use()?.bounds?.height?.useF() ?: 0f) + titleLabel.bounds.height.useF()
            }
            this.bindWidthToParent(multiplier = 0.5f, adjust = -100f)
            this.padding.set(Insets(0f))
            this.border.set(Insets(16f))
            this.borderStyle.set(SolidBorder(grey).also { border ->
                border.roundedCorners.set(true)
            })
        }

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)

            this.vBar.unitIncrement.set(48f)
            this.vBar.blockIncrement.set(48f * 4)
        }
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }


        contentPaneLeft.addChild(scrollPane)
        contentPaneLeft.addChild(hbox)

        // No levels label
        scrollPane.addChild(TextLabel(binding = {
            val ll = levelList.use()
            val active = activeLevelList.use()
            if (ll.isEmpty()) {
                Localization.getVar("mainMenu.library.noLevelsInFolder").use()
            } else if (active.isEmpty()) {
                Localization.getVar("mainMenu.library.noLevelsFiltered").use()
            } else ""
        }, font = main.fontMainMenuMain).apply {
            scrollPane.addChild(this) // Intentional, not part of content
            Anchor.Centre.configure(this, offsetX = -(scrollPane.barSize.get()))
            this.bindWidthToParent(adjust = -(64f + scrollPane.barSize.get()))
            this.bindHeightToParent(adjust = -(64f))
            this.doLineWrapping.set(true)
            this.renderAlign.set(Align.center)
            this.visible.bind {
                activeLevelList.use().isEmpty()
            }
        })
        
        val anyLevelSelected: ReadOnlyBooleanVar = BooleanVar {
            selectedLevelEntry.use() != null
        }
        // Select from the left! label
        contentPaneRight.addChild(TextLabel(binding = { Localization.getVar("mainMenu.library.selectFromLeft").use() }, font = main.fontMainMenuMain).apply {
            this.doLineWrapping.set(true)
            this.renderAlign.set(Align.center)
            this.visible.bind {
                !anyLevelSelected.useB()
            }
        })
        
        // Level details pane
        val levelDetailsPane = Pane().apply {
            val levelEntry: ReadOnlyVar<LevelEntry.Modern?> = Var.bind { selectedLevelEntry.use() as? LevelEntry.Modern }
            this.visible.bind {
                levelEntry.use() != null
            }
            val bannerRatio = 3.2f // 512 x 160
            val spacing = 4f
            this += ImageNode(binding = {
                val tex: Texture = currentBanner.use() ?: AssetRegistry["library_default_banner"]
                TextureRegion(tex)
            }, renderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO).apply {
                Anchor.TopLeft.configure(this)
                this.bindHeightToSelfWidth(multiplier = 1f / bannerRatio)
            }
            this += VBox().apply {
                Anchor.BottomLeft.configure(this)
                val thisBounds = this.bounds
                thisBounds.height.bind {
                    @Suppress("SimpleRedundantLet")
                    (parent.use()?.let { p -> p.contentZone.height.useF() } ?: 0f) - (thisBounds.width.useF() * (1f / bannerRatio) + spacing * 2)
                }
                this.spacing.set(spacing)
                this.temporarilyDisableLayouts {
                    this += TextLabel(binding = {
                        val level = levelEntry.use()
                        if (level != null) {
                            val metadata = level.levelMetadata
                            val exportStats = level.exportStatistics
                            "Creator: ${metadata.levelCreator}\n${metadata.songName}${if (metadata.songArtist.isNotBlank()) " by ${metadata.songArtist}" else ""}\n${metadata.albumName} (${metadata.albumYear})\n${metadata.genre}\nDifficulty: ${metadata.difficulty} / 10"
                        } else ""
                    }, font = main.fontMainMenuThin).apply { 
                        this.renderAlign.set(Align.topLeft)
                        this.doClipping.set(true)
                        this.doXCompression.set(false)
                        this.skinID.set(PRManiaSkins.SCROLLING_TEXTLABEL)
                    }
                }
            }
        }
        contentPaneRight += levelDetailsPane
        // Level details pane (legacy)
        val levelDetailsPaneLegacy = Pane().apply {
            val levelEntry: ReadOnlyVar<LevelEntry.Legacy?> = Var.bind { selectedLevelEntry.use() as? LevelEntry.Legacy }
            this.visible.bind {
                levelEntry.use() != null
            }
            val bannerRatio = 3.2f // 512 x 160
            val spacing = 4f
            this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("library_default_banner")),
                    renderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO).apply {
                Anchor.TopLeft.configure(this)
                this.bindHeightToSelfWidth(multiplier = 1f / bannerRatio)
            }
            this += VBox().apply {
                Anchor.BottomLeft.configure(this)
                val thisBounds = this.bounds
                thisBounds.height.bind {
                    @Suppress("SimpleRedundantLet")
                    (parent.use()?.let { p -> p.contentZone.height.useF() } ?: 0f) - (thisBounds.width.useF() * (1f / bannerRatio) + spacing * 2)
                }
                this.spacing.set(spacing)
                this.temporarilyDisableLayouts {
                    this += TextLabel(binding = {
                        val level = levelEntry.use()
                        if (level != null) {
                            "[Legacy Level]\n${level.getTitle()}\nGame Version: ${level.programVersion}"
                        } else ""
                    }, font = main.fontMainMenuThin).apply { 
                        this.renderAlign.set(Align.topLeft)
                    }
                }
            }
        }
        contentPaneRight += levelDetailsPaneLegacy

        val vbox = VBox().apply {
            this.spacing.set(0f)
            this.bounds.height.set(100f)
            this.margin.set(Insets(0f, 0f, 0f, 2f))
        }
        this.vbox = vbox

        scrollPane.setContent(vbox)

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
            hbox += createSmallButton(binding = {""}).apply {
                this.bindWidthToSelfHeight()
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["refresh"]))
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.library.refresh")))
                this.setOnAction {
                    startSearchThread()
                }
            }
            hbox += createSmallButton(binding = {""}).apply {
                this.bindWidthToSelfHeight()
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["filter"]))
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.library.sortAndFilter")))
                this.setOnAction {
                    // TODO
                }
            }
            hbox += createSmallButton(binding = {""}).apply {
                this.bindWidthToSelfHeight()
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_open"]))
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.library.openLibraryLocation.tooltip")))
                this.setOnAction {
                    Gdx.net.openFileExplorer(getLibraryFolder())
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.library.changeLibraryLocation").use() }).apply {
                this.bounds.width.set(160f)
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.library.changeLibraryLocation.tooltip")))
                this.setOnAction {
                    val oldFolder = getLibraryFolder()
                    main.restoreForExternalDialog { completionCallback ->
                        thread(isDaemon = true) {
                            TinyFDWrapper.selectFolder(Localization.getValue("fileChooser.libraryFolderChange.title"), oldFolder) { file: File? ->
                                completionCallback()
                                if (file != null && file.isDirectory && file != oldFolder) {
                                    main.persistDirectory(PreferenceKeys.FILE_CHOOSER_LIBRARY_VIEW, file)
                                    startSearchThread()
                                }
                            }
                        }
                    }
                }
            }
            
        }
        
        selectedLevelEntry.addListener {
            val level = it.getOrCompute()
            if (level is LevelEntry.Modern && level.file.exists()) {
                Gdx.app.postRunnable {
                    unloadBanner()
                    try {
                        val zipFile = ZipFile(level.file)
                        val bannerHeader = zipFile.getFileHeader("banner.png")
                        if (bannerHeader != null) {
                            val tempFile = TempFileUtils.createTempFile("banner", ".png")
                            zipFile.getInputStream(bannerHeader).use { zipInputStream ->
                                val out = tempFile.outputStream()
                                zipInputStream.copyTo(out)
                            }

                            val tex = Texture(FileHandle(tempFile))
                            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                            tempFile.delete()

                            if (!Container.isBannerTextureWithinSize(tex)) {
                                Paintbox.LOGGER.warn("Ignoring banner texture because it is not the right size (${tex.width}x${tex.height})")
                                tex.disposeQuietly()
                            } else {
                                unloadBanner()
                                this.currentBanner.set(tex)
                            }
                        }
                    } catch (e: Exception) {
                        Paintbox.LOGGER.warn("Failed to load level banner for ${level.file.absolutePath}")
                        e.printStackTrace()
                    }
                }
            } else {
                Gdx.app.postRunnable {
                    unloadBanner()
                }
            }
        }
    }

    fun prepareShow(): LibraryMenu {
        toggleGroup.activeToggle.getOrCompute()?.selectedState?.set(false)
        startSearchThread()
        return this
    }
    
    private fun getLibraryFolder(): File {
        val prefName = PreferenceKeys.FILE_CHOOSER_LIBRARY_VIEW
        return main.attemptRememberDirectory(prefName)?.takeIf { it.isDirectory } ?: run {
            PRMania.DEFAULT_LEVELS_FOLDER.also { defFolder ->
                defFolder.mkdirs()
                main.persistDirectory(prefName, defFolder)
            }
        }
    }
    
    private fun createLibraryEntryButton(levelEntry: LevelEntry): LibraryEntryButton {
        return LibraryEntryButton(this, levelEntry).apply {
            this@LibraryMenu.toggleGroup.addToggle(this)
            this.padding.set(Insets(4f, 4f, 12f, 12f))
            this.bounds.height.set(48f)
            this.textAlign.set(TextAlign.LEFT)
            this.renderAlign.set(Align.left)
            this.setOnHoverStart(blipSoundListener)
        }
    }
    
    private fun addLevels(list: List<LevelEntry>) {
        val levelEntryData = list.map { LevelEntryData(it, createLibraryEntryButton(it)) }
        
        val newLeveLList = levelList.getOrCompute() + levelEntryData
        levelList.set(newLeveLList)
        
        filterAndSortLevelList()
        updateLevelListVbox()
    }
    
    private fun removeLevels(list: List<LevelEntryData>) {
        levelList.set(levelList.getOrCompute() - list)
        activeLevelList.set(activeLevelList.getOrCompute() - list)
        list.forEach { toggleGroup.removeToggle(it.button) }
        
        filterAndSortLevelList()
        updateLevelListVbox()
    }
    
    private fun filterAndSortLevelList() {
        // TODO correct filtering
        val filtered = levelList.getOrCompute().toMutableList()
        
        // TODO correct sorting
        filtered.sortWith(LevelEntryData.comparator)
        activeLevelList.set(filtered)
    }
    
    private fun updateLevelListVbox() {
        val buttonBorder = Insets(1f, 0f, 0f, 0f)
        vbox.temporarilyDisableLayouts {
            vbox.children.forEach { vbox.removeChild(it) }
            activeLevelList.getOrCompute().forEachIndexed { index, it ->
                val button = it.button
                button.border.set(if (index == 0) Insets.ZERO else buttonBorder)
                vbox.addChild(button)
            }
        }
        vbox.sizeHeightToChildren(100f)
    }
    
    fun interruptSearchThread() {
        try {
            synchronized(this) {
                this.workerThread?.interrupt()
                this.workerThread = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun startSearchThread(): Thread {
        interruptSearchThread()
        
        removeLevels(levelList.getOrCompute().toList())
        
        val searchFolder = getLibraryFolder()
        val thread = thread(start = false, isDaemon = true, name = "Library Search") {
            Paintbox.LOGGER.info("Starting Library search in ${searchFolder.absolutePath}")
            val startNano = System.nanoTime()
            try {
                val potentialFiles = searchFolder.listFiles { file: File ->
                    val lowerName = file.name.lowercase(Locale.ROOT)
                    file.extension.lowercase(Locale.ROOT) == Container.LEVEL_FILE_EXTENSION 
                            && !lowerName.endsWith(".autosave.${Container.LEVEL_FILE_EXTENSION}")
                }?.toList() ?: emptyList()
                Paintbox.LOGGER.info("[Library Search] Possible files found: ${potentialFiles.size}")
                
                var lastUIPushTime = 0L
                val entriesToAdd = mutableListOf<LevelEntry>()
                
                fun pushEntriesToUI() {
                    if (entriesToAdd.isNotEmpty()) {
                        val copy = entriesToAdd.toList()
                        entriesToAdd.clear()
                        Gdx.app.postRunnable { 
                            addLevels(copy)
                        }
                        lastUIPushTime = System.currentTimeMillis()
                    }
                }
                
                var levelsAdded = 0
                for (file: File in potentialFiles) {
                    try {
                        val levelEntry: LevelEntry = loadLevelEntry(file) ?: continue
                        entriesToAdd += levelEntry
                        levelsAdded++
                        
                        if (System.currentTimeMillis() - lastUIPushTime >= 100L) {
                            pushEntriesToUI()
                        }
                    } catch (e: Exception) {
                        Paintbox.LOGGER.warn("Exception when scanning level in library: ${file.absolutePath}")
                        e.printStackTrace()
                    }
                }
                
                pushEntriesToUI()
                Paintbox.LOGGER.info("[Library Search] Levels read: $levelsAdded (took ${(System.nanoTime() - startNano) / 1_000_000f} ms)")
            } catch (ignored: InterruptedException) {
            } catch (e: Exception) {
                Paintbox.LOGGER.error("Exception when searching for files in library directory ${searchFolder.absolutePath}")
                e.printStackTrace()
            }
        }
        synchronized(this) {
            this.workerThread = thread
        }
        thread.start()
        return thread
    }

    private fun unloadBanner() {
        val ct = this.currentBanner.getOrCompute()
        if (ct != null) {
            this.currentBanner.set(null)
            ct.disposeQuietly()
        }
    }
    
    private fun loadLevelEntry(file: File): LevelEntry? {
        val zipFile = ZipFile(file)
        val json: JsonObject
        zipFile.getInputStream(zipFile.getFileHeader("manifest.json")).use { zipInputStream ->
            val reader = zipInputStream.reader()
            json = Json.parse(reader).asObject()
        }

        val libraryRelevantDataLoad = LibraryRelevantData.fromManifestJson(json, file.lastModified())
        val libraryRelevantData: LibraryRelevantData = libraryRelevantDataLoad.first
        
        val containerVersion: Int = libraryRelevantData.containerVersion
        val programVersion: Version = libraryRelevantData.programVersion
        val uuid: UUID? = libraryRelevantData.levelUUID
        val levelMetadata: LevelMetadata? = libraryRelevantData.levelMetadata
        val exportStatistics: ExportStatistics? = libraryRelevantData.exportStatistics
        
        return if (uuid != null && levelMetadata != null && exportStatistics != null) {
            if (libraryRelevantData.isAutosave || libraryRelevantData.isProject) return null
            LevelEntry.Modern(libraryRelevantData.levelUUID, file, containerVersion, programVersion, levelMetadata, exportStatistics)
        } else {
            LevelEntry.Legacy(file, containerVersion, programVersion)
        }
    }
    
}