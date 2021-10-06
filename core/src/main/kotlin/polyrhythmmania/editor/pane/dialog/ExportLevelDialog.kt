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
import paintbox.binding.VarChangedListener
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
import polyrhythmmania.container.manifest.ExportStatistics
import polyrhythmmania.container.manifest.SaveOptions
import polyrhythmmania.editor.Click
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.BlockEndState
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.ui.PRManiaSkins
import polyrhythmmania.util.DecimalFormats
import polyrhythmmania.util.TimeUtils
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.system.measureNanoTime


class ExportLevelDialog(editorPane: EditorPane) : EditorDialog(editorPane) {

    /*
    
    CHECKLIST is first, cannot migrate if the checklist is not complete.
      -> FILE_DIALOG_OPEN if the checklist is complete and user clicks the button
    FILE_DIALOG_OPEN
      -> Back to CHECKLIST if cancelled
      -> Go to SIMULATING
    SIMULATING will simulate the level
      -> If an exception occurs, go to DONE with an appropriate error msg
      -> If there are no issues, save
      -> If there are issues, go to SIMULATION_WARNINGS
    SIMULATION_WARNINGS shows the warnings in the simulation
      -> Yes/No confirmation to actually save or not.
    SAVE_ERROR will ask the user to either save to a different location or quit
      -> Back to SAVING or quit the dialog
    DONE
      -> Exit
    
     */
    enum class Substate {
        CHECKLIST,
        FILE_DIALOG_OPEN,
        SIMULATING,
        SIMULATION_WARNINGS,
        SAVE_ERROR,
        DONE,
    }
    
    private enum class ChecklistState(val color: Color) {
        NONE(PRManiaColors.NEGATIVE),
        COMPLETE(PRManiaColors.POSITIVE), 
        PARTIAL(Color.valueOf("#FF964C"));
    }
    
    private data class SimulationResult(val percentage: Int = 0, val firstMiss: Float? = null,
                                        val rodsExploded: Int = 0,
                                        val exportStatistics: ExportStatistics? = null) {
        
        fun anyWarnings(): Boolean {
            return firstMiss != null
        }
    }
    
    companion object {
        private val closableSubstates: Set<Substate> = EnumSet.complementOf(EnumSet.of(Substate.FILE_DIALOG_OPEN, Substate.SIMULATING))
    }

    private val substate: Var<Substate> = Var(Substate.CHECKLIST)
    private val checklistIncomplete: BooleanVar = BooleanVar(false)
    private val simulationResult: Var<SimulationResult> = Var(SimulationResult())
    private lateinit var levelFile: File
    
    private val checklistPane: Pane
    private val doneDescLabel: TextLabel
    private val saveErrorDescLabel: TextLabel
    private val simWarningsDescLabel: TextLabel

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
                        levelFile = file
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
        
        contentPane += TextLabel(binding = {
            Localization.getValue("editor.dialog.exportLevel.simulating.progress", simulationResult.use().percentage)
        }).apply {
            this.markup.set(editorPane.palette.markup)
            this.textColor.set(Color.WHITE.cpy())
            this.renderAlign.set(Align.center)
            this.visible.bind { substate.use() == Substate.SIMULATING }
        }
        
        doneDescLabel = TextLabel("").apply {
            this.markup.set(editorPane.palette.markup)
            this.textColor.set(Color.WHITE.cpy())
            this.renderAlign.set(Align.center)
            this.visible.bind { substate.use() == Substate.DONE }
        }
        contentPane += doneDescLabel
        saveErrorDescLabel = TextLabel("").apply {
            this.markup.set(editorPane.palette.markup)
            this.textColor.set(Color.WHITE.cpy())
            this.renderAlign.set(Align.center)
            this.visible.bind { substate.use() == Substate.SAVE_ERROR }
        }
        contentPane += saveErrorDescLabel
        simWarningsDescLabel = TextLabel("").apply {
            this.markup.set(editorPane.palette.markup)
            this.textColor.set(Color.WHITE.cpy())
            this.renderAlign.set(Align.center)
            this.visible.bind { substate.use() == Substate.SIMULATION_WARNINGS }
        }
        contentPane += simWarningsDescLabel
        bottomPane.addChild(Button(binding = { Localization.getVar("editor.dialog.exportLevel.saveError.button").use() },
                font = editorPane.palette.musicDialogFont).apply {
            this.applyDialogStyleBottom()
            this.bounds.width.set(500f)
            Anchor.TopCentre.configure(this)
            this.visible.bind { substate.use() == Substate.SAVE_ERROR }
            this.setOnAction {
                openFileDialog { file: File? ->
                    if (file != null) {
                        levelFile = file
                        save(file, simulationResult.getOrCompute())
                    } else {
                        substate.set(Substate.SAVE_ERROR)
                    }
                }
            }
        })
        bottomPane.addChild(Button(binding = { Localization.getVar("editor.dialog.exportLevel.simulationWarnings.button").use() },
                font = editorPane.palette.musicDialogFont).apply {
            this.applyDialogStyleBottom()
            this.bounds.width.set(500f)
            Anchor.TopCentre.configure(this)
            this.visible.bind { substate.use() == Substate.SIMULATION_WARNINGS }
            this.setOnAction {
                save(levelFile, simulationResult.getOrCompute())
            }
        })
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
        thread(start = true, isDaemon = true) {
            simulation(levelFile)
        }
    }
    
    private fun simulation(levelFile: File) {
        val editor = this.editor
        val container = editor.container
        val engine = container.engine
        val timing = engine.timingProvider
        val endBlockPosition = container.endBlockPosition.get()
        val endStateSec = engine.tempos.beatsToSeconds(endBlockPosition).coerceAtLeast(0f)
        engine.resetEndSignal()
        val percentageSimulated = AtomicInteger(0)
        val endSignalTriggered = AtomicBoolean(false)
        val endListener: VarChangedListener<Boolean> = VarChangedListener {
            if (it.getOrCompute()) {
                endSignalTriggered.set(true)
            }
        }
        engine.endSignalReceived.addListener(endListener)
        
        var currentSimResult = SimulationResult()
        
        try {
            engine.soundInterface.disableSounds = true
            editor.setPlaytestingEnabled(false)
            editor.compileEditorIntermediates()
            timing.seconds = 0f
            engine.seconds = 0f
            editor.resetWorld()
            engine.inputter.reset()
            
            var sec = 0f
            val step = 1f / 60f
            var lastUIUpdateMs = 0L
            var missedYet = false
            var rodsExploded = 0

            while (sec <= endStateSec && !endSignalTriggered.get()) {
                timing.seconds = sec
                engine.seconds = sec
                engine.removeActiveTextbox(unpauseSoundInterface = false, runTextboxOnComplete = true)
                
                if (engine.inputter.rodsExplodedPR > rodsExploded) {
                    rodsExploded = engine.inputter.rodsExplodedPR
                    currentSimResult = currentSimResult.copy(rodsExploded = rodsExploded)
                    if (!missedYet && !engine.inputter.noMiss) {
                        missedYet = true
                        currentSimResult = currentSimResult.copy(firstMiss = sec)
                    }
                }
                
                sec += step
                val percentageFloat = sec / (endStateSec.coerceAtLeast(0.01f))
                percentageSimulated.set(if (!percentageFloat.isFinite()) 0 else (percentageFloat * 100).roundToInt().coerceIn(0, 99))
                
                if ((System.currentTimeMillis() - lastUIUpdateMs) >= 50L) {
                    lastUIUpdateMs = System.currentTimeMillis()
                    currentSimResult = currentSimResult.copy(percentage = percentageSimulated.get())
                    Gdx.app.postRunnable { 
                        simulationResult.set(currentSimResult)
                    }
                }
            }
            
            percentageSimulated.set(100)
            val exportStatistics = ExportStatistics(endStateSec, engine.inputter.totalExpectedInputs,
                    engine.tempos.computeAverageTempo(endBlockPosition))
            val finalSimResult = currentSimResult.copy(percentage = 100, exportStatistics = exportStatistics)
            currentSimResult = finalSimResult
            Gdx.app.postRunnable {
                simulationResult.set(finalSimResult)
            }
            if (finalSimResult.anyWarnings()) {
                Gdx.app.postRunnable {
                    simWarningsDescLabel.text.set(Localization.getValue("editor.dialog.exportLevel.simulationWarnings",
                            finalSimResult.rodsExploded, DecimalFormats.format("0.0#", finalSimResult.firstMiss ?: Float.NaN)))
                    substate.set(Substate.SIMULATION_WARNINGS)
                }
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val exClassName = e.javaClass.name
            Gdx.app.postRunnable { 
                doneDescLabel.text.set(Localization.getValue("editor.dialog.exportLevel.simulationError", exClassName))
                substate.set(Substate.DONE)
            }
            return
        } finally {
            engine.soundInterface.disableSounds = false
            engine.endSignalReceived.removeListener(endListener)
            val originalSecs = editor.playbackStart.get()
            timing.seconds = originalSecs
            engine.seconds = originalSecs
            editor.resetWorld()
            editor.updatePaletteAndTexPackChangesState()
            engine.soundInterface.clearAllNonMusicAudio()
        }
        
        // Attempt to save
        save(levelFile, currentSimResult)
    }

    private fun save(newFile: File, simulationResult: SimulationResult) {
        try {
            editor.compileEditorIntermediates()
            
            editor.container.writeToFile(newFile, SaveOptions.editorExportAsLevel(simulationResult.exportStatistics!!))

            Gdx.app.postRunnable {
                doneDescLabel.text.set(Localization.getValue("editor.dialog.exportLevel.done.desc",
                        TimeUtils.convertMsToTimestamp((simulationResult.exportStatistics.durationSec) * 1000, noMs = true),
                        simulationResult.exportStatistics.inputCount,
                        DecimalFormats.format("0.0#", simulationResult.exportStatistics.averageBPM)))
                substate.set(Substate.DONE)
            }
        } catch (e: Exception) {
            Paintbox.LOGGER.warn("Error occurred while saving container as level:")
            e.printStackTrace()
            val exClassName = e.javaClass.name
            Gdx.app.postRunnable {
                substate.set(Substate.SAVE_ERROR)
                saveErrorDescLabel.text.set(Localization.getValue("editor.dialog.exportLevel.saveError", exClassName))
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