package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.PaintboxGame
import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageIcon
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.TinyFDWrapper
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaColors
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.container.Container
import polyrhythmmania.container.manifest.SaveOptions
import polyrhythmmania.editor.Click
import polyrhythmmania.editor.block.BlockEndState
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.ui.PRManiaSkins
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.max


class ExportLevelDialog(editorPane: EditorPane) : EditorDialog(editorPane) {

    /*
    
    CHECKLIST is first, cannot migrate if the checklist is not complete.
      -> FILE_DIALOG_OPEN if the checklist is complete and user clicks the button
    FILE_DIALOG_OPEN
      -> Back to CHECKLIST if cancelled
      -> Go to SIMULATING
    SIMULATING will simulate the level
      -> Once done, go to SAVING and save the level
    SAVING
      -> Go to SAVE_ERROR if an error occurs
      -> Go to DONE if successful
    SAVE_ERROR will show the simulation result, and will ask the user to either save to a different location or quit
      -> Back to SAVING or quit the dialog
    DONE is done and a confirmation of the simulation will appear
      -> Exit dialog
    
     */
    enum class Substate {
        CHECKLIST,
        FILE_DIALOG_OPEN,
        SIMULATING,
        SAVING,
        SAVE_ERROR,
        DONE,
    }
    
    private enum class ChecklistState(val color: Color) {
        NONE(PRManiaColors.NEGATIVE),
        COMPLETE(PRManiaColors.POSITIVE), 
        PARTIAL(Color.valueOf("#FF964C"));
    }
    
    companion object {
        private val closableSubstates: Set<Substate> = setOf(Substate.CHECKLIST, Substate.SAVE_ERROR, Substate.DONE)
    }

    private val substate: Var<Substate> = Var(Substate.CHECKLIST)
    private val checklistIncomplete: BooleanVar = BooleanVar(false)
    
    private val checklistPane: Pane
    
    private val descLabel: TextLabel

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.exportLevel.title").use() }
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
                !canCloseDialog(ss)
            }
        })
        
        checklistPane = ScrollPane().apply { 
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)
            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(0f, 0f, 0f, 0f))
            this.vBar.unitIncrement.set(64f)
            this.vBar.blockIncrement.set(100f)
            this.vBar.skinID.set(PRManiaSkins.SCROLLBAR_SKIN)
            
            this.visible.bind { substate.use() == Substate.CHECKLIST }
        }
        checklistPane.setContent(VBox().apply {
            this.temporarilyDisableLayouts {
                this += TextLabel(binding = { Localization.getVar("editor.dialog.exportLevel.checklist.desc").use() }).apply {
                    this.bounds.height.set(100f)
                    this.markup.set(editorPane.palette.markupInstantiatorDesc)
                    this.textColor.set(Color.WHITE.cpy())
                    this.renderAlign.set(Align.topLeft)
                    this.margin.set(Insets(8f, 0f, 0f, 0f))
                    this.doLineWrapping.set(true)
                }
                this += RectElement(Color.WHITE).apply { 
                    this.bounds.height.set(9f)
                    this.margin.set(Insets(4f, 4f, 1f, 1f))
                }
                this += TextLabel(binding = { Localization.getVar("editor.dialog.exportLevel.checklist.title").use() },
                        font = main.fontEditorDialogTitle).apply {
                    this.bounds.height.set(42f)
                    this.textColor.set(Color.WHITE.cpy())
                    this.renderAlign.set(Align.topLeft)
                    this.margin.set(Insets(12f, 8f, 0f, 0f))
                    this.setScaleXY(0.5f)
                }
                
                fun createChecklistItem(text: String, completed: ChecklistState): HBox {
                    if (completed == ChecklistState.NONE) {
                        checklistIncomplete.set(true)
                    }
                    return HBox().apply {
                        val iconSize = 64f
                        this.bounds.height.set(iconSize)
                        this.margin.set(Insets(8f))
                        val check = ImageIcon(TextureRegion(when (completed) {
                            ChecklistState.NONE -> PaintboxGame.paintboxSpritesheet.checkboxX
                            ChecklistState.COMPLETE -> PaintboxGame.paintboxSpritesheet.checkboxCheck
                            ChecklistState.PARTIAL -> PaintboxGame.paintboxSpritesheet.checkboxLine
                        })).apply {
                            this.margin.set(Insets(4f, 4f, 8f, 8f))
                            this.bounds.width.set(iconSize)
                            this.tint.set(completed.color)
                        }
                        val label = TextLabel(binding = { Localization.getVar(text).use() }).apply {
                            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("$text.tooltip")))
                            this.bounds.x.set(iconSize)
                            this.markup.set(editorPane.palette.markup)
                            this.textColor.set(Color.WHITE.cpy())
                            this.renderAlign.set(Align.left)
                            this.margin.set(Insets(1f, 1f, 16f, 32f))
                            this.resizeBoundsToContent(affectWidth = true, affectHeight = false)
                        }
                        this += check
                        this += label
                    }
                }

                val container = editor.container
                this += createChecklistItem("editor.dialog.exportLevel.checklist.item.endStateBlock",
                        if (container.blocks.any { it is BlockEndState }) ChecklistState.COMPLETE else ChecklistState.NONE)
                this += createChecklistItem("editor.dialog.exportLevel.checklist.item.levelMetadata",
                        if (container.levelMetadata.areRequiredFieldsNonempty()) (
                                if (container.levelMetadata.anyFieldsBlank()) ChecklistState.PARTIAL else ChecklistState.COMPLETE
                                ) else ChecklistState.NONE).apply {
                    this += Button(binding = { Localization.getVar("editor.dialog.exportLevel.checklist.item.levelMetadata.button").use() },
                            font = editorPane.palette.musicDialogFont).apply {
                        this.applyDialogStyleContent()
                        this.bounds.width.set(350f)
                        this.margin.set(Insets(4f))
                        this.setOnAction { 
                            editor.attemptOpenLevelMetadataDialog()
                        }
                    }
                }
            }
            this.sizeHeightToChildren(100f)
        })
        contentPane.addChild(checklistPane)
        bottomPane.addChild(Button(binding = { Localization.getVar("editor.dialog.exportLevel.checklist.simulateButton").use() },
                font = editorPane.palette.musicDialogFont).apply {
            this.applyDialogStyleBottom()
            this.bounds.width.set(500f)
            Anchor.TopCentre.configure(this)
            if (checklistIncomplete.get()) {
                this.disabled.set(true)
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.exportLevel.checklist.simulateButton.disabledTooltip")))
            }
            this.visible.bind { substate.use() == Substate.CHECKLIST }
            this.setOnAction { 
                openFileDialog { file: File? ->
                    if (file != null) {
                        startSimulation(file)
                    } else {
                        substate.set(Substate.CHECKLIST)
                    }
                }
            }
        })

        contentPane += TextLabel(binding = { Localization.getVar("common.closeFileChooser").use() }).apply {
            this.markup.set(editorPane.palette.markup)
            this.textColor.set(Color.WHITE.cpy())
            this.renderAlign.set(Align.center)
            this.visible.bind { substate.use() == Substate.FILE_DIALOG_OPEN }
        }
        
        descLabel = TextLabel("").apply {
            this.markup.set(editorPane.palette.markup)
            this.textColor.set(Color.WHITE.cpy())
            this.renderAlign.set(Align.center)
            this.textAlign.set(TextAlign.CENTRE)
        }
    }

    private fun openFileDialog(callback: (File?) -> Unit) {
        substate.set(Substate.FILE_DIALOG_OPEN)
        editorPane.main.restoreForExternalDialog { completionCallback ->
            thread(isDaemon = true) {
                val title = Localization.getValue("fileChooser.exportLevel.title")
                val filter = TinyFDWrapper.FileExtFilter(Localization.getValue("fileChooser.exportLevel.filter"), listOf("*.${Container.LEVEL_FILE_EXTENSION}")).copyWithExtensionsInDesc()
                TinyFDWrapper.saveFile(title, (main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_EXPORT)
                                        ?: main.getDefaultDirectory()).resolve("level.${Container.LEVEL_FILE_EXTENSION}"), filter) { file: File? ->
                    completionCallback()
                    if (file != null) {
                        val newInitialDirectory = if (!file.isDirectory) file.parentFile else file
                        val ext = file.extension
                        val fileWithCorrectExt = if (!ext.equals(Container.LEVEL_FILE_EXTENSION, ignoreCase = true)) {
                            (File(file.absolutePath + ".${Container.LEVEL_FILE_EXTENSION}"))
                        } else {
                            file
                        }

                        Gdx.app.postRunnable {
                            main.persistDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_EXPORT, newInitialDirectory)
                            callback(fileWithCorrectExt)
                        }
                    } else {
                        Gdx.app.postRunnable {
                            callback(null)
                        }
                    }
                }
            }
        }
    }
    
    private fun startSimulation(levelFile: File) {
        substate.set(Substate.SIMULATING)
        
    }

    private fun save(newFile: File) {
        try {
            Gdx.app.postRunnable {
                descLabel.text.set(Localization.getValue("editor.dialog.save.saving"))
                substate.set(Substate.SAVING)
            }
            
            editor.compileEditorIntermediates()

            editor.container.writeToFile(newFile, SaveOptions.EDITOR_EXPORT_AS_LEVEL)

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
        return canCloseDialog(this.substate.getOrCompute())
    }
    
    private fun canCloseDialog(substate: Substate): Boolean {
        return substate in closableSubstates
    }
}