package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.Paintbox
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.layout.HBox
import io.github.chrislo27.paintbox.ui.layout.VBox
import io.github.chrislo27.paintbox.util.TinyFDWrapper
import io.github.chrislo27.paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.Localization
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.container.Container
import polyrhythmmania.editor.EditorScreen
import polyrhythmmania.editor.pane.dialog.LoadDialog
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.soundsystem.SoundSystem
import java.io.File
import kotlin.concurrent.thread

class LoadSavedLevelMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    enum class Substate {
        FILE_DIALOG_OPEN,
        LOADING,
        LOADED,
        LOAD_ERROR,
    }

    val substate: Var<Substate> = Var(Substate.FILE_DIALOG_OPEN)

    val descLabel: TextLabel

    @Volatile
    private var loaded: LoadData? = null

    init {
        this.setSize(WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.play.playSavedLevel").use() }
        this.contentPane.bounds.height.set(250f)

        val content = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
            this.bindHeightToParent(-40f)
        }
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }

        descLabel = TextLabel(text = Localization.getValue("common.closeFileChooser")).apply {
            this.markup.set(this@LoadSavedLevelMenu.markup)
            this.padding.set(Insets(4f))
            this.textColor.set(UppermostMenu.ButtonSkin.TEXT_COLOR)
            this.renderAlign.set(Align.center)
            this.textAlign.set(TextAlign.CENTRE)
            this.doLineWrapping.set(true)
        }
        content.addChild(descLabel)

        contentPane.addChild(content)
        contentPane.addChild(hbox)

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.cancel").use() }).apply {
                this.bounds.width.set(100f)
                this.visible.bind {
                    when (substate.use()) {
                        Substate.LOADED, Substate.LOAD_ERROR -> true
                        else -> false
                    }
                }
                this.setOnAction {
                    loaded?.newContainer?.disposeQuietly()
                    loaded = null
                    removeSelfFromMenuCol()
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.play.playAction").use() }).apply {
                this.bounds.width.set(125f)
                this.visible.bind {
                    when (substate.use()) {
                        Substate.LOADED -> true
                        else -> false
                    }
                }
                this.setOnAction {
                    // TODO play!
                }
            }
        }
    }

    init { // This init block should be LAST
        Gdx.app.postRunnable {
            main.restoreForExternalDialog { completionCallback ->
                thread(isDaemon = true) {
                    val title = Localization.getValue("fileChooser.load.title")
                    val filter = TinyFDWrapper.FileExtFilter(Localization.getValue("fileChooser.load.filter"), listOf("*.${Container.FILE_EXTENSION}")).copyWithExtensionsInDesc()
                    TinyFDWrapper.openFile(title,
                            main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_PLAY_SAVED_LEVEL)
                                    ?: main.getDefaultDirectory(), filter) { file: File? ->
                        completionCallback()
                        if (file != null) {
                            loadFile(file)
                        } else { // Cancelled out
                            Gdx.app.postRunnable {
                                removeSelfFromMenuCol()
                            }
                        }
                    }
                }
            }
        }
    }
    
    fun removeSelfFromMenuCol() {
        menuCol.popLastMenu()
        menuCol.removeMenu(this)
    }

    /**
     * To be called in the SAME thread that opened the file chooser.
     */
    private fun loadFile(newFile: File) {
        Gdx.app.postRunnable {
            descLabel.text.set(Localization.getValue("editor.dialog.load.loading"))
            substate.set(Substate.LOADING)
        }

        val newSoundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem().apply {
            this.audioContext.out.gain = main.settings.gameplayVolume.getOrCompute() / 100f
        }
        val newContainer: Container = Container(newSoundSystem, SimpleTimingProvider {
            Gdx.app.postRunnable { throw it }
            true
        })

        try {
            val loadMetadata = newContainer.readFromFile(newFile)

            Gdx.app.postRunnable {
                substate.set(Substate.LOADED)
                descLabel.text.set(Localization.getValue("editor.dialog.load.loadedInformation", loadMetadata.programVersion, "${loadMetadata.containerVersion}"))
                loaded = LoadData(newContainer, loadMetadata)

                val newInitialDirectory = if (!newFile.isDirectory) newFile.parentFile else newFile
                main.persistDirectory(PreferenceKeys.FILE_CHOOSER_PLAY_SAVED_LEVEL, newInitialDirectory)
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

    data class LoadData(val newContainer: Container, val loadMetadata: Container.LoadMetadata)
}