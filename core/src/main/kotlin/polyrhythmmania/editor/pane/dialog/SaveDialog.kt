package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.binding.Var
import paintbox.filechooser.FileExtFilter
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.control.Button
import paintbox.ui.control.TextLabel
import paintbox.filechooser.TinyFDWrapper
import polyrhythmmania.Localization
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.container.Container
import polyrhythmmania.container.manifest.SaveOptions
import polyrhythmmania.editor.pane.EditorPane
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
    var lastSaveLoc: File? = null
        private set
    @Volatile
    private var firstTime: Boolean = true

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.save.title").use() }
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
    
    fun getCurrentSaveLocation(): File? {
        return lastSaveLoc
    }

    fun prepareShow(forceSaveAs: Boolean): SaveDialog {
        descLabel.text.set("")

        val location: File? = lastSaveLoc
        if (forceSaveAs || location == null || firstTime) {
            descLabel.text.set(Localization.getValue("common.closeFileChooser"))
            // Open file chooser
            
            fun getFileWithCorrectExt(file: File): File {
                val ext = file.extension
                val fileWithCorrectExt = if (ext.equals(Container.LEVEL_FILE_EXTENSION, ignoreCase = true)) {
                    // Strip out .prmania file ext and replace with .prmproj for older files
                    (File(file.absolutePath.substringBeforeLast(".${Container.LEVEL_FILE_EXTENSION}") + ".${Container.PROJECT_FILE_EXTENSION}"))
                } else if (!ext.equals(Container.PROJECT_FILE_EXTENSION, ignoreCase = true)) {
                    (File(file.absolutePath + ".${Container.PROJECT_FILE_EXTENSION}"))
                } else {
                    file
                }
                return fileWithCorrectExt
            }

            substate.set(Substate.FILE_DIALOG_OPEN)
            editorPane.main.restoreForExternalDialog { completionCallback ->
                thread(isDaemon = true) {
                    val title = Localization.getValue("fileChooser.save.project.title")
                    val filter = FileExtFilter(Localization.getValue("fileChooser.save.project.filter"), listOf("*.${Container.PROJECT_FILE_EXTENSION}")).copyWithExtensionsInDesc()
                    val correctFileLoc = if (location == null) null else getFileWithCorrectExt(location)
                    TinyFDWrapper.saveFile(title,
                            correctFileLoc
                                    ?: (main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_SAVE)
                                    ?: main.getDefaultDirectory()).resolve("level_project.${Container.PROJECT_FILE_EXTENSION}"), filter) { file: File? ->
                        completionCallback()
                        if (file != null) {
                            val newInitialDirectory = if (!file.isDirectory) file.parentFile else file
                            val fileWithCorrectExt = getFileWithCorrectExt(file)

                            Gdx.app.postRunnable {
                                main.persistDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_SAVE, newInitialDirectory)
                            }
                            save(fileWithCorrectExt)
                        } else {
                            Gdx.app.postRunnable {
                                substate.set(Substate.DONE)
                                attemptClose()
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
            
            editor.compileEditorIntermediates()

            editor.container.writeToFile(newFile, SaveOptions.EDITOR_SAVE_AS_PROJECT)
            lastSaveLoc = newFile
            firstTime = false

            Gdx.app.postRunnable {
                substate.set(Substate.DONE)
                attemptClose()
                editorPane.menubar.triggerAutosaveIndicator(Localization.getValue("editor.button.save.saveMessage"))
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

    override fun canCloseDialog(): Boolean {
        val substate = substate.getOrCompute()
        if (!(substate == Substate.SAVE_ERROR || substate == Substate.DONE)) return false
        return true
    }
}