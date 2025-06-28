package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.PaintboxGame
import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.binding.VarChangedListener
import paintbox.filechooser.FileExtFilter
import paintbox.filechooser.TinyFDWrapper
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageIcon
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.DecimalFormats
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaColors
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.achievements.Achievements
import polyrhythmmania.container.Container
import polyrhythmmania.container.manifest.ExportStatistics
import polyrhythmmania.container.manifest.SaveOptions
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.editor.block.BlockEndState
import polyrhythmmania.editor.block.FlashingLightsWarnable
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.engine.StatisticsMode
import polyrhythmmania.engine.input.InputResult
import polyrhythmmania.engine.input.InputResultLike
import polyrhythmmania.engine.input.InputScore
import polyrhythmmania.engine.input.InputType
import polyrhythmmania.ui.PRManiaSkins
import polyrhythmmania.util.TimeUtils
import polyrhythmmania.world.EntityRodPR
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.roundToInt


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
    
    private data class SimulationResult(val percentage: Int = 0, val firstMissBeat: Float? = null,
                                        val inputsMissed: Int = 0, val totalInputs: Int = 0,
                                        val exportStatistics: ExportStatistics? = null) {
        
        fun anyWarnings(): Boolean {
            return inputsMissed > 0
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
                
                fun createChecklistItem(text: String, completed: ChecklistState, required: Boolean): HBox {
                    if (completed == ChecklistState.NONE && required) {
                        checklistIncomplete.set(true)
                    }
                    return HBox().apply {
                        val iconSize = 64f
                        this.bounds.height.set(iconSize)
                        this.margin.set(Insets(6f, 6f, 8f, 8f))
                        val check = ImageIcon(TextureRegion(when (completed) {
                            ChecklistState.NONE -> PaintboxGame.gameInstance.staticAssets.paintboxSpritesheet.checkboxX
                            ChecklistState.COMPLETE -> PaintboxGame.gameInstance.staticAssets.paintboxSpritesheet.checkboxCheck
                            ChecklistState.PARTIAL -> PaintboxGame.gameInstance.staticAssets.paintboxSpritesheet.checkboxLine
                        })).apply {
                            this.margin.set(Insets(4f, 4f, 8f, 8f))
                            this.bounds.width.set(iconSize)
                            this.tint.set(completed.color)
                        }
                        val label = TextLabel(binding = { (if (required) "[color=prmania_negative]* []" else "") + Localization.getVar(text).use() }).apply {
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
                        if (container.blocks.any { it is BlockEndState }) ChecklistState.COMPLETE else ChecklistState.NONE, true)
                this += createChecklistItem("editor.dialog.exportLevel.checklist.item.levelMetadata",
                        if (container.levelMetadata.areRequiredFieldsNonempty()) (
                                if (container.levelMetadata.anyFieldsBlank()) ChecklistState.PARTIAL else ChecklistState.COMPLETE
                                ) else ChecklistState.NONE, true).apply {
                    this += Button(binding = { Localization.getVar("editor.dialog.exportLevel.checklist.item.levelMetadata.button").use() },
                            font = editorPane.palette.musicDialogFont).apply {
                        this.applyDialogStyleContent()
                        this.bounds.width.set(350f)
                        this.margin.set(Insets(4f))
                        this.setOnAction { 
                            editor.attemptOpenLevelMetadataDialog {
                                editor.attemptExport()
                            }
                        }
                    }
                }
                this += createChecklistItem("editor.dialog.exportLevel.checklist.item.levelBanner",
                        if (container.bannerTexture.getOrCompute() != null) ChecklistState.COMPLETE else ChecklistState.NONE, false).apply {
                    this += Button(binding = { Localization.getVar("editor.dialog.exportLevel.checklist.item.levelBanner.button").use() },
                            font = editorPane.palette.musicDialogFont).apply {
                        this.applyDialogStyleContent()
                        this.bounds.width.set(350f)
                        this.margin.set(Insets(4f))
                        this.setOnAction { 
                            editor.attemptOpenBannerDialog {
                                editor.attemptExport()
                            }
                        }
                    }
                }
                this += createChecklistItem("editor.dialog.exportLevel.checklist.item.flashingLightsWarning",
                        getCheckboxStateForFlashingLights(), false).apply {
                    this += Button(binding = { Localization.getVar("editor.dialog.exportLevel.checklist.item.levelMetadata.button").use() },
                            font = editorPane.palette.musicDialogFont).apply {
                        this.applyDialogStyleContent()
                        this.bounds.width.set(350f)
                        this.margin.set(Insets(4f))
                        this.setOnAction {
                            editor.attemptOpenLevelMetadataDialog {
                                editor.attemptExport()
                            }
                        }
                    }
                }
            }
            this.sizeHeightToChildren(100f)
        })
        contentPane.addChild(checklistPane)
        bottomPane += HBox().apply { 
            Anchor.TopCentre.configure(this)
            this.spacing.set(8f)
            this.align.set(HBox.Align.CENTRE)
            this.bindWidthToParent(multiplier = 0.9f)
            
            this += Button(binding = { Localization.getVar("editor.dialog.exportLevel.checklist.simulateButton").use() },
                    font = editorPane.palette.musicDialogFont).apply {
                this.applyDialogStyleBottom()
                this.bounds.width.set(500f)
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
            }
            if (EditorSpecialFlags.STORY_MODE in editor.flags) {
                this += Button("Simulate without export (STORY MODE)", font = editorPane.palette.musicDialogFont).apply {
                    this.applyDialogStyleBottom()
                    this.bounds.width.set(350f)
                    this.visible.bind { substate.use() == Substate.CHECKLIST }
                    if (!editor.container.blocks.any { it is BlockEndState }) {
                        this.disabled.set(true)
                        this.tooltipElement.set(editorPane.createDefaultTooltip("Missing End State block!"))
                    }
                    this.setOnAction {
                        startSimulationNoSave()
                    }
                }
            }
        }

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
    
    private fun getCheckboxStateForFlashingLights(): ChecklistState {
        val container = editor.container
        if (container.levelMetadata.flashingLightsWarning) {
            return ChecklistState.COMPLETE
        }
        
        val blocks = editor.container.blocks
        return if (blocks.any { it is FlashingLightsWarnable }) {
            ChecklistState.NONE
        } else ChecklistState.PARTIAL
    }

    private fun openFileDialog(callback: (File?) -> Unit) {
        substate.set(Substate.FILE_DIALOG_OPEN)
        editorPane.main.restoreForExternalDialog { completionCallback ->
            thread(isDaemon = true) {
                val title = Localization.getValue("fileChooser.exportLevel.title")
                val filter = FileExtFilter(Localization.getValue("fileChooser.exportLevel.filter"),
                        listOf("*.${Container.LEVEL_FILE_EXTENSION}")).copyWithExtensionsInDesc()
                val savedFileName: String? = editorPane.saveDialog.lastSaveLoc?.nameWithoutExtension
                val defaultFile = (main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_EDITOR_EXPORT)
                        ?: main.getDefaultDirectory()).resolve("${savedFileName ?: "level"}.${Container.LEVEL_FILE_EXTENSION}")
                TinyFDWrapper.saveFile(title, defaultFile, filter) { file: File? ->
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
    
    private fun startSimulationNoSave() {
        substate.set(Substate.SIMULATING)
        thread(start = true, isDaemon = true) {
            simulation(null)
        }
    }
    
    private fun simulation(levelFile: File?) {
        val editor = this.editor
        val container = editor.container
        val engine = container.engine
        val timing = engine.timingProvider
        val previousPlaybackSpeed = engine.playbackSpeed
        val endBlockPosition = container.endBlockPosition.get()
        val endStateSec = engine.tempos.beatsToSeconds(endBlockPosition).coerceAtLeast(0f)
        engine.resetEndSignal()
        engine.statisticsMode = StatisticsMode.DISABLED
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
            val inputter = engine.inputter
            engine.soundInterface.disableSounds = true
            editor.setPlaytestingEnabled(false)
            timing.seconds = 0f
            engine.seconds = 0f
            engine.playbackSpeed = 1f
            editor.compileEditorIntermediates()
            inputter.resetState()
            
            var sec = 0f
            val step = 1f / 60f
            var lastUIUpdateMs = 0L

            while (sec <= endStateSec && !endSignalTriggered.get()) {
                timing.seconds = sec
                engine.seconds = sec
                engine.removeActiveTextbox(unpauseSoundInterface = false, runTextboxOnComplete = true)
                
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
            
            container.world.entities.filterIsInstance<EntityRodPR>().forEach { rod ->
                inputter.submitInputsFromRod(rod)
            }
            percentageSimulated.set(100)
            
            val allTempoChanges = engine.tempos.getAllTempoChanges()
            val exportStatistics = ExportStatistics(endStateSec, inputter.totalExpectedInputs,
                    engine.tempos.computeAverageTempo(endBlockPosition),
                    allTempoChanges.minOf { it.newTempo }, allTempoChanges.maxOf { it.newTempo }, inputter.skillStarBeat.isFinite())
            
            data class InputResultTuple(override val perfectBeat: Float, override val expectedIndex: Int,
                                        override val inputType: InputType) : InputResultLike
            
            val expectedInputs: List<EntityRodPR.ExpectedInput.Expected> = inputter.expectedInputsPr
            val hitInputs: List<InputResult> = inputter.inputResults.filter { it.inputScore != InputScore.MISS }
            val hitInputsSet: Set<InputResultTuple> = hitInputs.map { 
                InputResultTuple(it.perfectBeat, it.expectedIndex, it.inputType) 
            }.toSet()
            val unhitInputs: List<InputResultTuple> = expectedInputs.map {
                InputResultTuple(it.perfectBeat, it.expectedIndex, it.inputType)
            }.filter { ei ->
                // We are directly comparing floats. Usually dangerous, but the auto inputter will generate the 
                // same perfectBeat for a given index and EntityRodPR
                ei !in hitInputsSet
            }.sortedBy { it.perfectBeat }
            
            val finalSimResult = currentSimResult.copy(percentage = 100, exportStatistics = exportStatistics, 
                    totalInputs = inputter.totalExpectedInputs, inputsMissed = unhitInputs.size,
                    firstMissBeat = unhitInputs.firstOrNull()?.perfectBeat)
            currentSimResult = finalSimResult
            Gdx.app.postRunnable {
                simulationResult.set(finalSimResult)
            }
            if (finalSimResult.anyWarnings()) {
                Gdx.app.postRunnable {
                    simWarningsDescLabel.text.set(Localization.getValue("editor.dialog.exportLevel.simulationWarnings",
                            finalSimResult.inputsMissed, finalSimResult.totalInputs,
                            DecimalFormats.format("0.0#", finalSimResult.firstMissBeat ?: Float.NaN)))
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
            engine.statisticsMode = StatisticsMode.IN_EDITOR
            engine.soundInterface.disableSounds = false
            engine.endSignalReceived.removeListener(endListener)
            val originalSecs = editor.playbackStart.get()
            timing.seconds = originalSecs
            engine.seconds = originalSecs
            editor.resetWorldEntitiesAndEngineModules()
            editor.updatePaletteAndTexPackChangesState()
            container.resetInputFeedbackEntities()
            engine.soundInterface.clearAllNonMusicAudio()
            engine.playbackSpeed = previousPlaybackSpeed
        }

        // Attempt to save
        save(levelFile, currentSimResult)
    }

    private fun save(newFile: File?, simulationResult: SimulationResult) {
        try {
            editor.compileEditorIntermediates()

            val exportStatistics: ExportStatistics = simulationResult.exportStatistics!!
            if (newFile != null) { // Null file is used for export only (debug)
                editor.container.writeToFile(newFile, SaveOptions.editorExportAsLevel(exportStatistics))
            }

            Gdx.app.postRunnable {
                doneDescLabel.text.set(Localization.getValue("editor.dialog.exportLevel.done.desc",
                        TimeUtils.convertMsToTimestamp((exportStatistics.durationSec) * 1000, noMs = true),
                        exportStatistics.inputCount,
                        DecimalFormats.format("0.0#", exportStatistics.minBPM),
                        DecimalFormats.format("0.0#", exportStatistics.averageBPM),
                        DecimalFormats.format("0.0#", exportStatistics.maxBPM),
                        DecimalFormats.format("0.0", exportStatistics.averageInputsPerMinute),
                ))
                substate.set(Substate.DONE)
                
                Achievements.attemptAwardThresholdAchievement(Achievements.editorFirstGoodExport, simulationResult.totalInputs)
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