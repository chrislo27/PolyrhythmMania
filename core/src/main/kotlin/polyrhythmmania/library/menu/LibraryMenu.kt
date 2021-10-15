package polyrhythmmania.library.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import net.lingala.zip4j.ZipFile
import paintbox.Paintbox
import paintbox.PaintboxGame
import paintbox.binding.Var
import paintbox.font.PaintboxFont
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.control.ToggleGroup
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.SkinFactory
import paintbox.util.TinyFDWrapper
import paintbox.util.Version
import paintbox.util.gdxutils.openFileExplorer
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.container.Container
import polyrhythmmania.container.LevelMetadata
import polyrhythmmania.container.manifest.ExportStatistics
import polyrhythmmania.container.manifest.LibraryRelevantData
import polyrhythmmania.library.LevelEntry
import polyrhythmmania.screen.mainmenu.menu.LoadSavedLevelMenu
import polyrhythmmania.screen.mainmenu.menu.MenuCollection
import polyrhythmmania.screen.mainmenu.menu.StandardMenu
import polyrhythmmania.ui.PRManiaSkins
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread


class LibraryMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    private var workerThread: Thread? = null
    
    private val toggleGroup: ToggleGroup = ToggleGroup()
    private val levelList: MutableList<LevelEntryData> = mutableListOf()
    
    private val vbox: VBox
    
    init {
        PRMania.DEFAULT_LEVELS_FOLDER
        
        this.setSize(WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.library.title").use() }
        this.showLogo.set(false)
        this.contentPane.bounds.height.set(520f)

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)

            this.vBar.unitIncrement.set(10f)
            this.vBar.blockIncrement.set(40f)
        }
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }


        contentPane.addChild(scrollPane)
        contentPane.addChild(hbox)


        val vbox = VBox().apply {
            this.spacing.set(0f)
            this.bounds.height.set(100f)
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
                    Gdx.net.openFileExplorer(main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_LIBRARY_VIEW) ?: run {
                        PRMania.DEFAULT_LEVELS_FOLDER.also { it.mkdirs() }
                    })
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.library.changeLibraryLocation").use() }).apply {
                this.bounds.width.set(160f)
                this.setOnAction {
                    // TODO
                    main.restoreForExternalDialog { completionCallback ->
                        thread(isDaemon = true) {
                            TinyFDWrapper.selectFolder("title", File("~")) { file: File? ->
                                completionCallback()
                                println("Selected: $file")
                            }
                        }
                    }
                }
            }
        }
    }
    
    fun prepareShow(): LibraryMenu {
        if (synchronized(this) { this.workerThread == null }) {
            startSearchThread()
        }
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
        return LibraryEntryButton(this, levelEntry, this.toggleGroup).apply {
            this.padding.set(Insets(4f, 4f, 12f, 12f))
            this.bounds.height.set(48f)
            this.textAlign.set(TextAlign.LEFT)
            this.renderAlign.set(Align.left)
            this.setOnHoverStart(blipSoundListener)
        }
    }
    
    private fun addLevels(list: List<LevelEntry>) {
        val levelEntryData = list.map { LevelEntryData(it, createLibraryEntryButton(it)) }
        
        levelList.addAll(levelEntryData)
        
        levelList.sortWith(LevelEntryData.comparator)
        updateLevelListVbox()
    }
    
    private fun removeLevel(levelEntryData: LevelEntryData) {
        levelList -= levelEntryData
        
        levelList.sortWith(LevelEntryData.comparator)
        updateLevelListVbox()
    }
    
    private fun updateLevelListVbox() {
        vbox.temporarilyDisableLayouts {
            vbox.children.forEach { vbox.removeChild(it) }
            levelList.forEach { vbox.addChild(it.button) }
        }
        vbox.sizeHeightToChildren(100f)
    }
    
    fun interruptSearchThread() {
        try {
            synchronized(this) {
                this.workerThread?.interrupt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun startSearchThread(): Thread {
        interruptSearchThread()
        
        val searchFolder = getLibraryFolder()
        val thread = thread(start = false, isDaemon = true, name = "Library search") {
            try {
                val potentialFiles = searchFolder.listFiles { file: File ->
                    val lowerName = file.name.lowercase(Locale.ROOT)
                    file.extension.lowercase(Locale.ROOT) == Container.LEVEL_FILE_EXTENSION 
                            && !lowerName.endsWith(".autosave.${Container.LEVEL_FILE_EXTENSION}")
                }?.toList() ?: emptyList()
                
                var lastUIPushTime = System.currentTimeMillis()
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
                
                for (file: File in potentialFiles) {
                    try {
                        val levelEntry: LevelEntry = loadLevelEntry(file)
                        entriesToAdd += levelEntry
                        
                        if (System.currentTimeMillis() - lastUIPushTime >= 100L) {
                            pushEntriesToUI()
                        }
                    } catch (e: Exception) {
                        Paintbox.LOGGER.warn("Exception when scanning level in library: ${file.absolutePath}")
                        e.printStackTrace()
                    }
                }
                
                pushEntriesToUI()
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
    
    private fun loadLevelEntry(file: File): LevelEntry {
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
            LevelEntry.Modern(libraryRelevantData.levelUUID, file, containerVersion, programVersion, levelMetadata, exportStatistics)
        } else {
            LevelEntry.Legacy(file, containerVersion, programVersion)
        }
    }
    
}