package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.Paintbox
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.packing.PackedSheet
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.ImageNode
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.control.ButtonSkin
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.layout.HBox
import io.github.chrislo27.paintbox.util.TinyFDWrapper
import polyrhythmmania.Localization
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
        FILE_DIALOG_OPEN,
        LOADING,
        LOADED,
        LOAD_ERROR,
    }

    val substate: Var<Substate> = Var(Substate.FILE_DIALOG_OPEN)

    val descLabel: TextLabel
    val confirmButton: Button

    @Volatile
    private var loaded: LoadData? = null

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.load.title").use() }
        bottomPane.addChild(Button("").apply {
            Anchor.BottomRight.configure(this)
            this.bounds.width.bind { bounds.height.use() }
            this.applyDialogStyleBottom()
            this.setOnAction {
                attemptCloseDialog()
            }
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_linear")["x"])).apply {
                this.tint.bind { editorPane.palette.toolbarIconToolNeutralTint.use() }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("common.cancel")))
            this.disabled.bind {
                val ss = substate.use()
                !(ss == Substate.LOAD_ERROR || ss == Substate.LOADED)
            }
        })
        descLabel = TextLabel("").apply {
            this.markup.set(editorPane.palette.markup)
            this.textColor.set(Color.WHITE.cpy())
            this.renderAlign.set(Align.center)
            this.textAlign.set(TextAlign.CENTRE)
        }
        contentPane.addChild(descLabel)

        val hbox = HBox().apply {
            Anchor.BottomCentre.configure(this)
            this.align.set(HBox.Align.CENTRE)
            this.spacing.set(16f)
            this.bounds.width.set(700f)
        }
        bottomPane.addChild(hbox)

        confirmButton = Button(binding = { Localization.getVar("editor.dialog.load.confirm").use() }, font = editorPane.palette.loadDialogFont).apply {
            this.bounds.width.set(600f)
            this.applyDialogStyleBottom()
            this.visible.bind { substate.use() == Substate.LOADED }
            this.setOnAction {
                val loadData = loaded
                if (loadData == null) {
                    attemptCloseDialog()
                } else {
                    val newScreen = loadData.newEditorScreen
                    val currentScreen = main.screen
                    if (currentScreen is EditorScreen) {
                        Gdx.app.postRunnable {
                            currentScreen.dispose()
                        }
                    }
                    main.screen = newScreen
                }
            }
        }
        hbox.temporarilyDisableLayouts {
            hbox.addChild(confirmButton)
        }
    }

    fun prepareShow(): LoadDialog {
        descLabel.text.set(Localization.getValue("common.closeFileChooser"))
        // Open file chooser

        substate.set(Substate.FILE_DIALOG_OPEN)
        editorPane.main.restoreForExternalDialog { completionCallback ->
            thread(isDaemon = true) {
                val title = Localization.getValue("fileChooser.load.title")
                val filter = TinyFDWrapper.FileExtFilter(Localization.getValue("fileChooser.load.filter"), listOf("*.${Container.FILE_EXTENSION}")).copyWithExtensionsInDesc()
                TinyFDWrapper.openFile(title,
                        main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_LOAD)
                                ?: main.getDefaultDirectory(), filter) { file: File? ->
                    completionCallback()
                    if (file != null) {
                        val newInitialDirectory = if (!file.isDirectory) file.parentFile else file

                        Gdx.app.postRunnable {
                            main.persistDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_LOAD, newInitialDirectory)
                            load(file)
                        }
                    } else {
                        Gdx.app.postRunnable {
                            substate.set(Substate.LOADED)
                            attemptCloseDialog()
                        }
                    }
                }
            }
        }

        return this
    }

    /**
     * Must be called on the GL thread.
     */
    private fun load(newFile: File) {
        descLabel.text.set(Localization.getValue("editor.dialog.load.loading"))
        substate.set(Substate.LOADING)

        val newEditorScreen = EditorScreen(main, debugMode = ((main.screen as? EditorScreen?)?.debugMode ?: false))
        val newEditor: Editor = newEditorScreen.editor
        val newContainer: Container = newEditor.container

        thread(isDaemon = true) {
            try {
                val loadMetadata = newContainer.readFromFile(newFile)

                Gdx.app.postRunnable {
                    // Translate some aspects to the Editor
                    newEditor.startingTempo.set(newContainer.engine.tempos.tempoAtSeconds(0f))
                    newEditor.tempoChanges.set(newContainer.engine.tempos.getAllTempoChanges().filter { it.beat > 0f })
                    newEditor.musicVolumes.set(newContainer.engine.musicData.volumeMap.getAllMusicVolumes())
                    
                    val musicRes: ExternalResource? = newContainer.compressedMusic
                    if (musicRes != null) {
                        val containerMusicData = newContainer.engine.musicData
                        val beadsMusic = containerMusicData.beadsMusic!!
                        newEditor.musicData.setMusic(beadsMusic, musicRes)
                        newEditor.musicData.waveform?.generateSummaries()
                        
                        newEditor.musicData.firstBeatSec.set(containerMusicData.firstBeatSec)
                        newEditor.musicFirstBeat.set(containerMusicData.musicFirstBeat)
                        val loopParams = containerMusicData.loopParams
                        newEditor.musicData.loopParams.set(loopParams)
                        
                        newEditor.updateForNewMusicData()
                    }

                    substate.set(Substate.LOADED)
                    descLabel.text.set(Localization.getValue("editor.dialog.load.loaded", loadMetadata.programVersion, "${loadMetadata.containerVersion}"))
                    loaded = LoadData(newEditorScreen, loadMetadata)
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

    private fun attemptCloseDialog() {
        val substate = substate.getOrCompute()
        if (!(substate == Substate.LOAD_ERROR || substate == Substate.LOADED)) return

        val loadData = this.loaded
        if (loadData != null) {
            this.loaded = null
            loadData.newEditorScreen.dispose()
        }
        
        editorPane.closeDialog()
    }

    data class LoadData(val newEditorScreen: EditorScreen, val loadMetadata: Container.LoadMetadata)
}
