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
import paintbox.util.TinyFDWrapper
import polyrhythmmania.Localization
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.container.Container
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.util.TempFileUtils
import java.io.File
import kotlin.concurrent.thread


class SaveDialog(editorPane: EditorPane) : EditorDialog(editorPane) {

    enum class Substate {
        FILE_DIALOG_OPEN,
        SAVING,
        DONE,
        SAVE_ERROR,
    }

    val substate: Var<Substate> = Var(Substate.FILE_DIALOG_OPEN)

    val descLabel: TextLabel

    @Volatile
    private var lastSaveLoc: File? = null
    @Volatile
    private var firstTime: Boolean = true

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.save.title").use() }
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
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("common.close")))
            this.disabled.bind {
                val ss = substate.use()
                !(ss == Substate.SAVE_ERROR || ss == Substate.DONE)
            }
        })
        descLabel = TextLabel("").apply {
            this.markup.set(editorPane.palette.markup)
            this.textColor.set(Color.WHITE.cpy())
            this.renderAlign.set(Align.center)
            this.textAlign.set(TextAlign.CENTRE)
        }
        contentPane.addChild(descLabel)
    }
    
    fun assignSaveLocation(file: File) {
        lastSaveLoc = file
    }

    fun prepareShow(forceSaveAs: Boolean): SaveDialog {
        descLabel.text.set("")

        val location: File? = lastSaveLoc
        if (forceSaveAs || location == null || firstTime) {
            descLabel.text.set(Localization.getValue("common.closeFileChooser"))
            // Open file chooser

            substate.set(Substate.FILE_DIALOG_OPEN)
            editorPane.main.restoreForExternalDialog { completionCallback ->
                thread(isDaemon = true) {
                    val title = Localization.getValue("fileChooser.save.title")
                    val filter = TinyFDWrapper.FileExtFilter(Localization.getValue("fileChooser.save.filter"), listOf("*.${Container.FILE_EXTENSION}")).copyWithExtensionsInDesc()
                    TinyFDWrapper.saveFile(title,
                            lastSaveLoc
                                    ?: (main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_SAVE)
                                    ?: main.getDefaultDirectory()).resolve("level.${Container.FILE_EXTENSION}"), filter) { file: File? ->
                        completionCallback()
                        if (file != null) {
                            val newInitialDirectory = if (!file.isDirectory) file.parentFile else file
                            val fileWithCorrectExt = if (!file.extension.equals(Container.FILE_EXTENSION, ignoreCase = true))
                                    (File(file.absolutePath + ".${Container.FILE_EXTENSION}"))
                            else file
                            
                            Gdx.app.postRunnable {
                                main.persistDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_SAVE, newInitialDirectory)
                            }
                            save(fileWithCorrectExt)
                        } else {
                            Gdx.app.postRunnable {
                                substate.set(Substate.DONE)
                                attemptCloseDialog()
                            }
                        }
                    }
                }
            }
        } else {
            thread(isDaemon = true) {
                save(location)
            }
        }

        return this
    }

    private fun save(newFile: File) {
        try {
            Gdx.app.postRunnable {
                descLabel.text.set(Localization.getValue("editor.dialog.save.saving"))
                substate.set(Substate.SAVING)
            }

            editor.container.writeToFile(newFile)
            lastSaveLoc = newFile
            firstTime = false

            Gdx.app.postRunnable {
                substate.set(Substate.DONE)
                attemptCloseDialog()
            }
        } catch (e: Exception) {
            Paintbox.LOGGER.warn("Error occurred while saving container:")
            e.printStackTrace()
            val exClassName = e.javaClass.name
            Gdx.app.postRunnable {
                substate.set(Substate.SAVE_ERROR)
                descLabel.text.set(Localization.getValue("editor.dialog.save.saveError", exClassName))
            }
        }
    }

    private fun attemptCloseDialog() {
        val substate = substate.getOrCompute()
        if (!(substate == Substate.SAVE_ERROR || substate == Substate.DONE)) return

        editorPane.closeDialog()
    }
}