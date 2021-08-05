package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.control.Button
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import paintbox.util.TinyFDWrapper
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.container.Container
import polyrhythmmania.container.ExternalResource
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorScreen
import polyrhythmmania.editor.pane.EditorPane
import java.io.File
import kotlin.concurrent.thread


class LoadDialog(editorPane: EditorPane) : EditorDialog(editorPane) {

    enum class Substate {
        PICKING_OPTION,
        FILE_DIALOG_OPEN,
        LOADING,
        LOADED,
        LOAD_ERROR,
    }

    val substate: Var<Substate> = Var(Substate.PICKING_OPTION)

    val descLabel: TextLabel
    val confirmButton: Button

    @Volatile
    private var loaded: LoadData? = null

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.load.title").use() }
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
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("common.cancel")))
            this.disabled.bind {
                val ss = substate.use()
                !(ss == Substate.LOAD_ERROR || ss == Substate.LOADED || ss == Substate.PICKING_OPTION)
            }
        })
        descLabel = TextLabel("").apply {
            this.markup.set(editorPane.palette.markup)
            this.textColor.set(Color.WHITE.cpy())
            this.renderAlign.set(Align.center)
            this.textAlign.set(TextAlign.CENTRE)
        }
        contentPane.addChild(descLabel)

        val confirmHbox = HBox().apply {
            Anchor.BottomCentre.configure(this)
            this.align.set(HBox.Align.CENTRE)
            this.spacing.set(16f)
            this.bounds.width.set(700f)
            this.visible.bind { substate.use() == Substate.LOADED }
        }
        bottomPane.addChild(confirmHbox)
        confirmButton = Button(binding = { Localization.getVar("editor.dialog.load.confirm").use() }, font = editorPane.palette.loadDialogFont).apply {
            this.bounds.width.set(600f)
            this.applyDialogStyleBottom()
            this.setOnAction {
                val loadData = loaded
                if (loadData == null) {
                    attemptClose()
                } else {
                    val newScreen = loadData.newEditorScreen
                    val currentScreen = main.screen
                    if (currentScreen is EditorScreen) {
                        Gdx.app.postRunnable {
                            main.screen = null
                            currentScreen.dispose()
                            main.screen = newScreen
                        }
                    }
                }
            }
        }
        confirmHbox.temporarilyDisableLayouts {
            confirmHbox.addChild(confirmButton)
        }


        val pickHbox = HBox().apply {
            Anchor.BottomCentre.configure(this)
            this.align.set(HBox.Align.CENTRE)
            this.spacing.set(16f)
            this.bounds.width.set(900f)
            this.visible.bind { substate.use() == Substate.PICKING_OPTION }
        }
        bottomPane.addChild(pickHbox)
        pickHbox.temporarilyDisableLayouts {
            pickHbox += Button(binding = { Localization.getVar("editor.dialog.load.button.savedLevel").use() },
                    font = editorPane.palette.loadDialogFont).apply {
                this.bounds.width.set(500f)
                this.applyDialogStyleBottom()
                this.setOnAction {
                    showFileDialog(main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_LOAD)
                            ?: main.getDefaultDirectory(), false)
                }
            }
            pickHbox += Button(binding = { Localization.getVar("editor.dialog.load.button.recovery").use() },
                    font = editorPane.palette.loadDialogFont).apply {
                this.bounds.width.set(300f)
                this.applyDialogStyleBottom()
                this.setOnAction {
                    showFileDialog(PRMania.RECOVERY_FOLDER, true)
                }
            }
        }
    }
    
    private fun showFileDialog(dir: File, isRecovery: Boolean) {
        descLabel.text.set(Localization.getValue("common.closeFileChooser"))
        substate.set(Substate.FILE_DIALOG_OPEN)
        editorPane.main.restoreForExternalDialog { completionCallback ->
            thread(isDaemon = true) {
                val title = Localization.getValue("fileChooser.load.title")
                val filter = TinyFDWrapper.FileExtFilter(Localization.getValue("fileChooser.load.filter"), listOf("*.${Container.FILE_EXTENSION}")).copyWithExtensionsInDesc()
                TinyFDWrapper.openFile(title, dir, filter) { file: File? ->
                    completionCallback()
                    if (file != null) {
                        Gdx.app.postRunnable {
                            load(file, isRecovery)
                        }
                    } else {
                        Gdx.app.postRunnable {
                            prepareShow(null)
                        }
                    }
                }
            }
        }
    }

    fun prepareShow(dropPath: String?): LoadDialog {
        if (dropPath == null) {
            descLabel.text.set(Localization.getValue("editor.dialog.load.desc"))
            substate.set(Substate.PICKING_OPTION)
        } else {
            val file = File(dropPath)
            Gdx.app.postRunnable {
                load(file, false)
            }
        }
 
        return this
    }

    /**
     * Must be called on the GL thread.
     */
    private fun load(newFile: File, isRecovery: Boolean) {
        descLabel.text.set(Localization.getValue("editor.dialog.load.loading"))
        substate.set(Substate.LOADING)

        val newEditorScreen = EditorScreen(main, debugMode = ((main.screen as? EditorScreen?)?.debugMode ?: false))
        val newEditor: Editor = newEditorScreen.editor
        val newContainer: Container = newEditor.container

        thread(isDaemon = true) {
            try {
                val loadMetadata = newContainer.readFromFile(newFile)
                
                if (loadMetadata.isFutureVersion) {
                    Gdx.app.postRunnable {
                        substate.set(Substate.LOAD_ERROR)
                        descLabel.text.set(Localization.getValue("editor.dialog.load.error.futureVersion", loadMetadata.programVersion.toString(), loadMetadata.containerVersion))
                    }
                } else {
                    Gdx.app.postRunnable {
                        // Translate some aspects to the Editor
                        newEditor.startingTempo.set(newContainer.engine.tempos.tempoAtSeconds(0f))
                        newEditor.tempoChanges.set(newContainer.engine.tempos.getAllTempoChanges().filter { it.beat > 0f })
                        newEditor.musicVolumes.set(newContainer.engine.musicData.volumeMap.getAllMusicVolumes())
                        newEditor.timeSignatures.set(newContainer.engine.timeSignatures.map.values.toList())

                        val musicRes: ExternalResource? = newContainer.compressedMusic
                        if (musicRes != null) {
                            val containerMusicData = newContainer.engine.musicData
                            val beadsMusic = containerMusicData.beadsMusic!!
                            newEditor.musicData.setMusic(beadsMusic, musicRes)
                            newEditor.musicData.waveform?.generateSummaries()

                            newEditor.musicData.firstBeatSec.set(containerMusicData.firstBeatSec)
                            newEditor.musicData.rate.set(containerMusicData.rate)
                            newEditor.musicFirstBeat.set(containerMusicData.musicSyncPointBeat)
                            val loopParams = containerMusicData.loopParams
                            newEditor.musicData.loopParams.set(loopParams)

                            newEditor.updateForNewMusicData()
                        }

                        if (!isRecovery) {
                            newEditor.editorPane.saveDialog.assignSaveLocation(newFile)
                        }

                        substate.set(Substate.LOADED)
                        descLabel.text.set(Localization.getValue("editor.dialog.load.loaded",
                            Localization.getValue("editor.dialog.load.loadedInformation",
                                loadMetadata.programVersion, "${loadMetadata.containerVersion}"))
                        )
                        loadMetadata.loadOnGLThread()
                        loaded = LoadData(newEditorScreen, loadMetadata)

                        if (!isRecovery) {
                            val newInitialDirectory = if (!newFile.isDirectory) newFile.parentFile else newFile
                            main.persistDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_LOAD, newInitialDirectory)
                        }
                    }
                }
            } catch (e: Exception) {
                Paintbox.LOGGER.warn("Error occurred while loading container:")
                e.printStackTrace()
                val exClassName = e.javaClass.name
                Gdx.app.postRunnable {
                    substate.set(Substate.LOAD_ERROR)
                    descLabel.text.set(Localization.getValue("editor.dialog.load.loadError", exClassName))
                }
            }
        }
    }

    override fun onCloseDialog() {
        val loadData = this.loaded
        if (loadData != null) {
            this.loaded = null
            loadData.newEditorScreen.dispose()
        }
    }

    override fun canCloseDialog(): Boolean {
        val substate = substate.getOrCompute()
        if (!(substate == Substate.LOAD_ERROR || substate == Substate.LOADED || substate == Substate.PICKING_OPTION))
            return false
        
        return true
    }

    data class LoadData(val newEditorScreen: EditorScreen, val loadMetadata: Container.LoadMetadata)
}
