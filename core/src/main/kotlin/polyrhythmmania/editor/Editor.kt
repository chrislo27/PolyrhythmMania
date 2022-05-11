package polyrhythmmania.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import paintbox.Paintbox
import paintbox.binding.*
import paintbox.font.Markup
import paintbox.font.TextRun
import paintbox.ui.SceneRoot
import paintbox.ui.UIElement
import paintbox.ui.contextmenu.ContextMenu
import paintbox.util.MathHelper
import paintbox.util.Vector2Stack
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.isAltDown
import paintbox.util.gdxutils.isControlDown
import paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.PRManiaGame
import polyrhythmmania.Settings
import polyrhythmmania.container.Container
import polyrhythmmania.container.GlobalContainerSettings
import polyrhythmmania.container.manifest.SaveOptions
import polyrhythmmania.editor.block.*
import polyrhythmmania.editor.help.HelpDialog
import polyrhythmmania.editor.music.EditorMusicData
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.editor.pane.dialog.*
import polyrhythmmania.editor.undo.ActionGroup
import polyrhythmmania.editor.undo.ActionHistory
import polyrhythmmania.editor.undo.impl.*
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.input.InputKeymapKeyboard
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.engine.tempo.TempoMap
import polyrhythmmania.engine.timesignature.TimeSignature
import polyrhythmmania.soundsystem.*
import paintbox.util.DecimalFormats
import polyrhythmmania.engine.StatisticsMode
import polyrhythmmania.engine.input.InputType
import polyrhythmmania.world.EventDeployRod
import polyrhythmmania.world.World
import polyrhythmmania.world.entity.TemporaryEntity
import polyrhythmmania.world.render.ForceTexturePack
import polyrhythmmania.world.render.ForceTilesetPalette
import polyrhythmmania.world.render.WorldRenderer
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.ceil


class Editor(val main: PRManiaGame, val flags: EnumSet<EditorSpecialFlags>)
    : ActionHistory<Editor>(), InputProcessor, Disposable, Lwjgl3WindowListener {

    companion object {
        val DEFAULT_TRACKS: List<Track> = listOf(
                Track(TrackID.INPUT_0, EnumSet.of(BlockType.INPUT)),
                Track(TrackID.INPUT_1, EnumSet.of(BlockType.INPUT)),
                Track(TrackID.INPUT_2, EnumSet.of(BlockType.INPUT)),
                Track(TrackID.FX_0, EnumSet.of(BlockType.FX)),
                Track(TrackID.FX_1, EnumSet.of(BlockType.FX)),
                Track(TrackID.FX_2, EnumSet.of(BlockType.FX)),
        )
        
        val MOVE_WINDOW_LEFT_KEYCODES: Set<Int> = setOf(Input.Keys.LEFT, Input.Keys.A)
        val MOVE_WINDOW_RIGHT_KEYCODES: Set<Int> = setOf(Input.Keys.RIGHT, Input.Keys.D)
        val MOVE_WINDOW_KEYCODES: Set<Int> = (MOVE_WINDOW_LEFT_KEYCODES + MOVE_WINDOW_RIGHT_KEYCODES)

        val AUTOSAVE_INTERVALS: List<Int> = listOf(0, 1, 3, 5, 10, 15, 20, 30)
    }

    private val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    val previewFrameBuffer: FrameBuffer
    val previewTextureRegion: TextureRegion
    val waveformWindow: WaveformWindow
    val settings: Settings get() = main.settings

    val sceneRoot: SceneRoot = SceneRoot(uiCamera)

    val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem().apply {
        this.audioContext.out.gain = main.settings.gameplayVolume.getOrCompute() / 100f
    }
    val timing: TimingProvider = SimpleTimingProvider {
        Gdx.app.postRunnable { throw it }
        true
    } //soundSystem
    val container: Container = Container(this.soundSystem, this.timing,
            GlobalContainerSettings(forceTexturePack = ForceTexturePack.NO_FORCE, forceTilesetPalette = ForceTilesetPalette.NO_FORCE))

    val world: World get() = container.world
    val engine: Engine get() = container.engine
    val renderer: WorldRenderer get() = container.renderer

    val inputKeymapKeyboard: InputKeymapKeyboard = settings.inputKeymapKeyboard.getOrCompute()

    // Default markup used for blocks, bold is inverted
    val blockMarkup: Markup = Markup(mapOf(
            Markup.FONT_NAME_BOLD to main.mainFontBordered,
            Markup.FONT_NAME_ITALIC to main.mainFontItalicBordered,
            Markup.FONT_NAME_BOLDITALIC to main.mainFontBoldItalicBordered,
            "rodin" to main.fontRodinFixedBordered,
            "prmania_icons" to main.fontIcons,
    ), TextRun(main.mainFontBoldBordered, ""), Markup.FontStyles.ALL_USING_BOLD_ITALIC)

    val tracks: List<Track> = DEFAULT_TRACKS
    val trackMap: Map<TrackID, Track> = tracks.associateByTo(LinkedHashMap()) { track -> track.id }

    // Editor tooling states
    val playState: ReadOnlyVar<PlayState> = Var(PlayState.STOPPED)
    val trackView: TrackView = TrackView()
    val tool: ReadOnlyVar<Tool> = Var(Tool.SELECTION)
    val click: Var<Click> = Var(Click.None)
    val allowedToEdit: ReadOnlyBooleanVar = BooleanVar { playState.use() == PlayState.STOPPED && click.use() == Click.None }
    val snapping: FloatVar = FloatVar(0.25f)
    val beatLines: BeatLines = BeatLines()
    var cameraPan: CameraPan? = null
    val cameraOffset: CameraOffset = CameraOffset()
    private val pressedButtons: MutableSet<Int> = mutableSetOf()
    private var suggestPanCameraDir: Int = 0 // Used when dragging items

    // Read-only editor settings hooking into Settings
    val panWhenDraggingAtEdge: ReadOnlyVar<Boolean> get() = settings.editorCameraPanOnDragEdge
    val panningDuringPlaybackSetting: ReadOnlyVar<CameraPanningSetting> get() = settings.editorPanningDuringPlayback
    val autosaveInterval: ReadOnlyVar<Int> get() = settings.editorAutosaveInterval

    // Editor objects and state
    val markerMap: Map<MarkerType, Marker> = MarkerType.VALUES.asReversed().associateWith { Marker(it) }
    val playbackStart: FloatVar = markerMap.getValue(MarkerType.PLAYBACK_START).beat
    val blocks: List<Block> get() = container.blocks
    val selectedBlocks: Map<Block, Boolean> = WeakHashMap()
    val startingTempo: FloatVar = FloatVar(TempoMap.DEFAULT_STARTING_GLOBAL_TEMPO)
    val tempoChanges: Var<List<TempoChange>> = Var(listOf())
    val musicFirstBeat: FloatVar = markerMap.getValue(MarkerType.MUSIC_FIRST_BEAT).beat
    val musicVolumes: Var<List<MusicVolume>> = Var(listOf())
    val musicData: EditorMusicData by lazy { EditorMusicData(this) }
    val metronomeEnabled: BooleanVar = BooleanVar(false)
    val timeSignatures: Var<List<TimeSignature>> = Var(listOf())
    private var lastPlacedMetronomeBeat: Int = -1
    private var timeUntilAutosave: Float = autosaveInterval.getOrCompute() * 60f
    val lastAutosaveTimeMs: LongVar = LongVar(0L)
    val playbackSpeed: FloatVar = FloatVar(1f)

    val engineBeat: FloatVar = FloatVar(engine.beat)

    /**
     * Call Var<Boolean>.invert() to force the status to be updated. Used when an undo or redo takes place.
     */
    private val forceUpdateStatus: BooleanVar = BooleanVar(false)
    val editorPane: EditorPane
    
    private val autosaveIntervalListener: VarChangedListener<Int> = VarChangedListener {
        timeUntilAutosave = it.getOrCompute() * 60f
    }

    init {
        previewFrameBuffer = FrameBuffer(Pixmap.Format.RGB888, 1280, 720, true, true)
        previewTextureRegion = TextureRegion(previewFrameBuffer.colorBufferTexture).also { tr ->
            tr.flip(false, true)
        }
        waveformWindow = WaveformWindow(this)
        soundSystem.setPaused(true)
        soundSystem.startRealtime()
    }

    init {
        engine.statisticsMode = StatisticsMode.IN_EDITOR
        engine.inputCalibration = main.settings.inputCalibration.getOrCompute()
        engine.autoInputs = true
        engine.endSignalReceived.addListener { endSignal ->
            if (endSignal.getOrCompute() && playState.getOrCompute() == PlayState.PLAYING) {
                changePlayState(PlayState.STOPPED)
            }
        }
        engine.soundInterface.disableSounds = true
        tool.addListener {
            beatLines.active = false
        }
        autosaveInterval.addListener(autosaveIntervalListener)
        playbackSpeed.addListener { 
            engine.playbackSpeed = it.getOrCompute()
        }
        metronomeEnabled.addListener { 
            if (!it.getOrCompute()) {
                engine.removeEvents(engine.events.filter { e -> e is EventEditorMetronome })
            }
            lastPlacedMetronomeBeat = -1
        }
    }

    init { // This init block should be LAST
        editorPane = EditorPane(this)
        sceneRoot += editorPane
        resize()
        bindStatusBar(editorPane.statusBarMsg)
        playbackStart.addListener { ps ->
            val newBeat = ps.getOrCompute()
            val newSeconds = engine.tempos.beatsToSeconds(newBeat)
            // Remove all other events first
            engine.removeEvents(engine.events.toList())
            timing.seconds = newSeconds
            engine.seconds = newSeconds
            engineBeat.set(newBeat)
            updatePaletteAndTexPackChangesState(newBeat)
        }
    }

    fun render(delta: Float, batch: SpriteBatch) {
        val frameBuffer = this.previewFrameBuffer
        frameBuffer.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        renderer.render(batch)
        frameBuffer.end()

        val camera = uiCamera
        batch.projectionMatrix = camera.combined
        batch.begin()

        val cameraPan = this.cameraPan
        if (cameraPan != null) {
            cameraPan.update(delta, trackView)
            if (cameraPan.isDone) {
                this.cameraPan = null
            }
        }

        sceneRoot.renderAsRoot(batch)

        batch.end()
    }

    fun renderUpdate() {
        val ctrl = Gdx.input.isControlDown()
//        val alt = Gdx.input.isAltDown()
        val shift = Gdx.input.isShiftDown()
        val delta = Gdx.graphics.deltaTime

        val autosaveIntervalMin = autosaveInterval.getOrCompute()
        if (autosaveIntervalMin > 0) {
            if (timeUntilAutosave > 0f)
                timeUntilAutosave -= delta
        }

        val currentPlayState = playState.getOrCompute()
        if (currentPlayState == PlayState.PLAYING) {
            timing.seconds += delta

            val currentEngineBeat = engine.beat
            engineBeat.set(currentEngineBeat)
            
            populateMetronomeTicks(engine.beat + 8)

            val currentPanSetting = panningDuringPlaybackSetting.getOrCompute()
            when (currentPanSetting) {
                CameraPanningSetting.PAN -> {
                    if (this.cameraPan == null && !pressedButtons.any { it in MOVE_WINDOW_KEYCODES }) {
                        val beatWidth = editorPane.allTracksPane.editorTrackArea.beatWidth.get()
                        val currentBeatX = trackView.beat.get()
                        if (currentEngineBeat !in (currentBeatX)..(currentBeatX + beatWidth)) {
                            this.cameraPan = CameraPan(0.125f, currentBeatX, currentEngineBeat, Interpolation.smoother)
                        }
                    }
                }
                CameraPanningSetting.FOLLOW -> {
                    this.cameraPan = null
                    val aheadAmt = 4f

                    val cameraOffset = this.cameraOffset
                    var anyManual = false
                    if (pressedButtons.any { it in MOVE_WINDOW_LEFT_KEYCODES }) {
                        anyManual = !anyManual

                        val target = editorPane.allTracksPane.editorTrackArea.beatWidth.get() - aheadAmt - 3f
                        cameraOffset.changeTarget(target)
                    }
                    if (pressedButtons.any { it in MOVE_WINDOW_RIGHT_KEYCODES }) {
                        anyManual = !anyManual
                        val target = -(aheadAmt - 1f)
                        cameraOffset.changeTarget(target)
                    }

                    if (!anyManual) {
                        val target = 0f
                        cameraOffset.changeTarget(target)
                    }

                    cameraOffset.update(delta)

                    val seconds = engine.seconds
                    val linearBeat = engine.tempos.secondsToBeats(seconds, disregardSwing = true) - (aheadAmt + cameraOffset.current)
                    trackView.beat.set(MathHelper.snapToNearest(linearBeat, 1f / trackView.pxPerBeat.get()).coerceAtLeast(0f))
                }
            }
        } else {
            if (currentPlayState == PlayState.STOPPED) {
                if (autosaveIntervalMin > 0) {
                    if (timeUntilAutosave <= 0f && allowedToEdit.get() && click.getOrCompute() == Click.None) {
                        timeUntilAutosave = autosaveIntervalMin.coerceAtLeast(1) * 60f
                        thread(start = true, isDaemon = true, priority = Thread.MIN_PRIORITY, name = "Editor Autosave") {
                            val currentSaveLoc = editorPane.saveDialog.getCurrentSaveLocation()
                            val isRecovery = currentSaveLoc == null
                            val file: File = if (currentSaveLoc != null) {
                                val suffix = ".autosave"
                                val max = 64 // Doesn't include the container extension
                                val newfilename = currentSaveLoc.nameWithoutExtension.take(max - suffix.length) + suffix + "." + Container.PROJECT_FILE_EXTENSION
                                currentSaveLoc.resolveSibling(newfilename)
                            } else {
                                getRecoveryFile(true)
                            }
                            try {
                                container.writeToFile(file, SaveOptions.EDITOR_AUTOSAVE)
                                Paintbox.LOGGER.debug("Autosave completed (interval: $autosaveIntervalMin min, filename: ${file.name})")
                                Gdx.app.postRunnable {
                                    lastAutosaveTimeMs.set(System.currentTimeMillis())
                                    editorPane.menubar.triggerAutosaveIndicator(Localization.getValue("editor.button.save.autosave.success${if (isRecovery) ".recovery" else ""}"))
                                }
                            } catch (e: Exception) {
                                Paintbox.LOGGER.warn("Autosave failed! filename: ${file.name}")
                                e.printStackTrace()
                                Gdx.app.postRunnable {
                                    editorPane.menubar.triggerAutosaveIndicator(Localization.getValue("editor.button.save.autosave.failure"))
                                }
                            }
                        }
                    }
                }
            }
        }

        click.getOrCompute().renderUpdate()

        if (!ctrl) {
            val moveFast = shift
            if (MOVE_WINDOW_RIGHT_KEYCODES.any { it in pressedButtons } || suggestPanCameraDir > 0) {
                panCamera(+1, delta, moveFast)
            }
            if (MOVE_WINDOW_LEFT_KEYCODES.any { it in pressedButtons } || suggestPanCameraDir < 0) {
                panCamera(-1, delta, moveFast)
            }
        }
    }
    
    fun getRecoveryFile(overwrite: Boolean, midfix: String = ""): File {
        val now = LocalDate.now()
        val date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT))
        fun file(num: Int): File {
            return PRMania.RECOVERY_FOLDER.resolve("recovery_${date}${if (midfix.isNotEmpty()) "_${midfix}" else ""}${if (num > 0) "_$num" else ""}.${Container.PROJECT_FILE_EXTENSION}")
        }
        var num = 0
        var lastFile = file(0)
        while (!overwrite && lastFile.exists()) {
            lastFile = file(++num)
        }
        return lastFile
    }

    fun panCamera(dir: Int, delta: Float, fast: Boolean = Gdx.input.isShiftDown()) {
        if (dir == 0) return

        val trackView = this.trackView
        val panSpeed = 7f * delta * (if (fast) 10f else 1f)
        trackView.beat.set((trackView.beat.get() + panSpeed * dir / trackView.renderScale.get()).coerceAtLeast(0f))
    }

    /**
     * Call to change the "intermediate state" editor objects (like Blocks, editor tempo changes, etc)
     * into the [engine]. This mutates the [engine] state.
     */
    fun compileEditorIntermediates() {
        resetWorld()
        compileEditorTempos()
        compileEditorTimeSignatures()
        compileEditorMusicInfo()
        compileEditorMusicVolumes()
        compileEditorBlocks()
    }

    fun compileEditorBlocks() {
        engine.removeEvents(engine.events.toList())
        val events = mutableListOf<Event>()
        blocks.sortedWith(Block.getComparator()).forEach { block ->
            events.addAll(block.compileIntoEvents())
        }
        engine.addEvents(events)
    }

    fun compileEditorTempos() {
        // Set starting tempo
        val tempos = engine.tempos
        tempos.removeTempoChangesBulk(tempos.getAllTempoChanges())
        tempos.addTempoChange(TempoChange(0f, this.startingTempo.get()))
        tempos.addTempoChangesBulk(this.tempoChanges.getOrCompute().toList())
    }

    fun compileEditorTimeSignatures() {
        val ts = engine.timeSignatures
        ts.clear()
        this.timeSignatures.getOrCompute().forEach { t ->
            ts.add(t)
        }
    }

    fun compileEditorMusicInfo() {
        val engineMusicData = engine.musicData
        engineMusicData.beadsMusic = this.musicData.beadsMusic
        engineMusicData.loopParams = this.musicData.loopParams.getOrCompute()
        engineMusicData.firstBeatSec = this.musicData.firstBeatSec.get()
        engineMusicData.rate = this.musicData.rate.get()
        engineMusicData.musicSyncPointBeat = this.musicFirstBeat.get()
        val player = engine.soundInterface.getCurrentMusicPlayer(engineMusicData.beadsMusic)
        player?.useLoopParams(engineMusicData.loopParams)
    }
    
    fun compileEditorMusicVolumes() {
        val engineMusicData = engine.musicData
        val volumeMap = engineMusicData.volumeMap
        volumeMap.removeMusicVolumesBulk(volumeMap.getAllMusicVolumes())
        volumeMap.addMusicVolumesBulk(this.musicVolumes.getOrCompute().toList())
    }

    fun resetWorld() {
        world.entities.toList().forEach { ent ->
            if (ent is TemporaryEntity) {
                world.removeEntity(ent)
            }
        }
        world.rows.forEach { row ->
            row.rowBlocks.forEach { entity ->
                entity.despawn(1f)
            }
            row.updateInputIndicators()
        }
        engine.inputter.reset()
        engine.removeActiveTextbox(unpauseSoundInterface = false, runTextboxOnComplete = false)
    }

    /**
     * Should be called on GL thread only. Renders the music waveform for the music dialog and sets up other values.
     */
    fun updateForNewMusicData() {
        val beadsMusic: BeadsMusic = musicData.beadsMusic ?: return
        val window = editorPane.musicDialog.window
        window.reset()
        window.musicDurationSec.set((beadsMusic.musicSample.lengthMs / 1000).toFloat())
        window.limitWindow()
        this.compileEditorMusicInfo()
        this.waveformWindow.generateOverall()
        this.waveformWindow.invalidateBlockCache()
    }

    fun attemptInstantiatorDrag(instantiator: Instantiator<Block>) {
        if (!allowedToEdit.get()) return
        val currentTool = this.tool.getOrCompute()
        if (currentTool != Tool.SELECTION) {
            changeTool(Tool.SELECTION)
        }

        val newBlock: Block = instantiator.factory.invoke(instantiator, engine)

        val newClick = Click.DragSelection.create(this, listOf(newBlock), Vector2(0f, 0f), newBlock, true)
        if (newClick != null) {
            click.set(newClick)
        }
    }

    fun attemptMarkerMove(markerType: MarkerType, mouseBeat: Float) {
        if (!allowedToEdit.get()) return
        val marker = this.markerMap.getValue(markerType)
        click.set(Click.MoveMarker(this, marker.beat, markerType).apply {
            this.onMouseMoved(mouseBeat, 0, 0f)
        })
    }

    fun attemptPlaybackStartMove(mouseBeat: Float) {
        attemptMarkerMove(MarkerType.PLAYBACK_START, mouseBeat)
    }

    fun attemptMusicDelayMove(mouseBeat: Float) {
        attemptMarkerMove(MarkerType.MUSIC_FIRST_BEAT, mouseBeat)
    }

    fun attemptUndo() {
        if (canUndo() && allowedToEdit.get()) {
            this.undo()
            forceUpdateStatus.invert()
        }
    }

    fun attemptRedo() {
        if (canRedo() && allowedToEdit.get()) {
            this.redo()
            forceUpdateStatus.invert()
        }
    }

    fun attemptOpenSettingsDialog() {
        if (allowedToEdit.get()) {
            editorPane.openDialog(editorPane.editorSettingsDialog)
        }
    }

    fun attemptOpenHelpDialog() {
        if (allowedToEdit.get()) {
            editorPane.openDialog(editorPane.helpDialog)
        }
    }

    fun attemptExitToTitle() {
        changePlayState(PlayState.STOPPED)
        if (allowedToEdit.get()) {
            editorPane.openDialog(editorPane.exitConfirmDialog)
        }
    }
    
    fun attemptOpenPaletteEditDialog() {
        if (allowedToEdit.get()) {
            editorPane.openDialog(editorPane.paletteEditDialog.prepareShow())
        }
    }
    
    fun attemptOpenTexturePackEditDialog() {
        if (allowedToEdit.get()) {
            editorPane.openDialog(editorPane.texturePackEditDialog.prepareShow())
        }
    }
    
    fun attemptOpenLevelMetadataDialog(afterDialogClosed: () -> Unit = {}) {
        if (allowedToEdit.get()) {
            editorPane.openDialog(LevelMetadataDialog(editorPane, afterDialogClosed))
        }
    }
    
    fun attemptOpenBannerDialog(afterDialogClosed: () -> Unit) {
        if (allowedToEdit.get()) {
            editorPane.openDialog(BannerDialog(editorPane, afterDialogClosed))
        }
    }

    fun attemptStartPlaytest() {
        editorPane.openDialog(editorPane.playtestDialog)
        setPlaytestingEnabled(true)
        if (playState.getOrCompute() == PlayState.STOPPED && settings.editorPlaytestStartsPlay.getOrCompute()) {
            changePlayState(PlayState.PLAYING)
        }
    }

    fun setPlaytestingEnabled(enabled: Boolean) {
        engine.autoInputs = !enabled
        engine.inputter.areInputsLocked = !enabled
    }

    fun attemptNewLevel() {
        if (allowedToEdit.get()) {
            editorPane.openDialog(editorPane.newDialog)
        }
    }

    fun attemptSave(forceSaveAs: Boolean) {
        if (allowedToEdit.get()) {
            editorPane.openDialog(editorPane.saveDialog.prepareShow(forceSaveAs))
        }
    }

    fun attemptExport() {
        if (allowedToEdit.get()) {
            editorPane.openDialog(ExportLevelDialog(editorPane))
        }
    }

    fun attemptLoad(dropPath: String?) {
        if (allowedToEdit.get()) {
            editorPane.openDialog(editorPane.loadDialog.prepareShow(dropPath))
        }
    }

    fun changeTool(tool: Tool) {
        if (!allowedToEdit.get()) return
        this.tool as Var
        this.tool.set(tool)
    }

    fun changePlayState(newState: PlayState) {
        this.playState as Var
        val lastState = this.playState.getOrCompute()
        if (lastState == newState) return
        if (this.click.getOrCompute() != Click.None) return
        if (lastState == PlayState.STOPPED && newState == PlayState.PAUSED) return


        if (newState == PlayState.PLAYING) {
            soundSystem.setPaused(false)
            engine.soundInterface.disableSounds = false
        } else {
            soundSystem.setPaused(true)
            engine.soundInterface.disableSounds = true
        }

        if (lastState == PlayState.STOPPED && newState == PlayState.PLAYING) {
            compileEditorIntermediates()
            resetWorld()
            engine.resetEndSignal()
            cameraOffset.changeTarget(0f)
            cameraOffset.reset()
            renderer.onWorldReset()

            val playbackStartBeats = this.playbackStart.get()
            val newSeconds = engine.tempos.beatsToSeconds(playbackStartBeats)
            val wereSoundsDisabled = engine.soundInterface.disableSounds
            engine.soundInterface.disableSounds = true
            
            engine.removeEvents(engine.events.filter { it is EventDeployRod && it.beat + 4f + 5f < playbackStartBeats })
            engine.removeEvents(engine.events.filter { it is EventEditorMetronome })
            
            // Simulate some time before the playback start depending on deploy rod events
            if (settings.editorHigherAccuracyPreview.getOrCompute()) {
                // For each EventDeployRod, check if the window of time (4 to 4+5 beats) overlaps the playbackStartBeats
                val rodEvents = engine.events.filter { e ->
                    e is EventDeployRod && playbackStartBeats in (e.beat + 4f)..(e.beat + 4f + 5f)
                }.sorted()
                if (rodEvents.isNotEmpty()) {
                    val simulateFromBeat = rodEvents.first().beat + 4f
                    
                    if (simulateFromBeat < playbackStartBeats) {
                        val simFromSec = engine.tempos.beatsToSeconds(simulateFromBeat) - (1 / 60f)
                        var s = simFromSec
                        while (s < newSeconds) {
                            timing.seconds = s
                            engine.seconds = s
                            engine.removeActiveTextbox(unpauseSoundInterface = false, runTextboxOnComplete = true)
                            val last = s
                            s = (s + 1 / 60f).coerceAtMost(newSeconds)
                            if (s <= last) break
                        }
                    }
                }
            }
            
            timing.seconds = newSeconds
            engine.seconds = newSeconds
            engine.musicData.update()
            engine.soundInterface.disableSounds = wereSoundsDisabled
            
            lastPlacedMetronomeBeat = -1
            populateMetronomeTicks(engine.beat + 8)
        } else if (newState == PlayState.STOPPED) {
            resetWorld()
            val newSeconds = engine.tempos.beatsToSeconds(this.playbackStart.get())
            timing.seconds = newSeconds
            engine.seconds = newSeconds
            updatePaletteAndTexPackChangesState()
            engine.soundInterface.clearAllNonMusicAudio()
        }

        if (newState == PlayState.PLAYING) {
            val player = engine.soundInterface.getCurrentMusicPlayer(engine.musicData.beadsMusic)
            if (player != null) {
                engine.musicData.setMusicPlayerPositionToCurrentSec()
                player.pause(false)
            }
        }

        this.playState.set(newState)
    }
    
    private fun populateMetronomeTicks(upUntilBeat: Float) {
        val engineBeatCeil = ceil(engine.beat).toInt()
        
        if (metronomeEnabled.get()) {
            var beat: Int = engineBeatCeil
            while (beat < upUntilBeat) {
                if (beat > lastPlacedMetronomeBeat) {
                    engine.addEvent(EventEditorMetronome(engine, (beat).toFloat()))
                }
                beat++
            }
            lastPlacedMetronomeBeat = beat - 1
        } else {
            lastPlacedMetronomeBeat = engineBeatCeil
        }
    }
    
    fun updatePaletteAndTexPackChangesState(currentBeat: Float = engine.beat) {
        world.tilesetPalette.applyTo(renderer.tileset)
        container.setTexturePackFromSource()
        
        val events = blocks.filter { it is BlockPaletteChange || it is BlockTexPackChange }
                .flatMap { it.compileIntoEvents() }.sortedBy { it.beat }
        events.forEach { evt ->
            engine.updateEvent(evt, currentBeat)
        }
    }

    fun attemptOpenGenericContextMenu(contextMenu: ContextMenu) {
        val root = sceneRoot
        root.hideRootContextMenu()
        blocks.forEach { it.ownedContextMenu = null }
        root.showRootContextMenu(contextMenu, suggestOffsetY = -64f)
        editorPane.enqueueAnimation(contextMenu.opacity, 0f, 1f).apply {
            onStart = { contextMenu.visible.set(true) }
        }
    }

    fun attemptOpenBlockContextMenu(block: Block, contextMenu: ContextMenu) {
        val root = sceneRoot
        root.hideRootContextMenu()
        blocks.forEach { it.ownedContextMenu = null }
        block.ownedContextMenu = contextMenu
        val existing = contextMenu.onRemovedFromScene
        contextMenu.onRemovedFromScene = { r ->
            existing(r)
            block.ownedContextMenu = null
        }
        root.showRootContextMenu(contextMenu, suggestOffsetY = -128f)
        editorPane.enqueueAnimation(contextMenu.opacity, 0f, 1f).apply {
            onStart = { contextMenu.visible.set(true) }
        }
    }

    fun resize() {
        var width = Gdx.graphics.width.toFloat()
        var height = Gdx.graphics.height.toFloat()
        // UI scale
        // 1 = native (1x larger), 2 = 1.5x larger, 3 = 2x larger, 4 = 2.5x larger
        val uiScale = ((settings.editorUIScale.getOrCompute().coerceAtLeast(1) + 1) / 2f).coerceAtLeast(1f) //(width / 1280f).coerceAtLeast(0f)
        width /= uiScale
        height /= uiScale
        if (width < 1280f || height < 720f) {
            width = 1280f
            height = 720f
        }

        uiCamera.setToOrtho(false, width, height)
        uiCamera.update()
        sceneRoot.resize()
    }

    override fun dispose() {
        previewFrameBuffer.disposeQuietly()
        waveformWindow.disposeQuietly()
        editorPane.dispose()

        soundSystem.setPaused(true)
        container.disposeQuietly()
        autosaveInterval.removeListener(autosaveIntervalListener)
    }

    fun addBlock(block: Block) {
        container.addBlock(block)
    }

    fun addBlocks(blocksToAdd: List<Block>) {
        container.addBlocks(blocksToAdd)
    }

    fun removeBlock(block: Block) {
        container.removeBlock(block)
        (this.selectedBlocks as MutableMap).remove(block)
        if (block.ownedContextMenu != null) {
            if (sceneRoot.isContextMenuActive())
                sceneRoot.hideRootContextMenu()
            block.ownedContextMenu = null
        }
    }

    fun removeBlocks(blocksToAdd: List<Block>) {
        container.removeBlocks(blocksToAdd)
        this.selectedBlocks as MutableMap
        blocksToAdd.forEach { block ->
            this.selectedBlocks.remove(block)
            if (block.ownedContextMenu != null) {
                if (sceneRoot.isContextMenuActive())
                    sceneRoot.hideRootContextMenu()
                block.ownedContextMenu = null
            }
        }
    }

    private fun bindStatusBar(msg: Var<String>) {
        msg.bind {
            this@Editor.forceUpdateStatus.use()
            val tool = this@Editor.tool.use()
            val currentClick = this@Editor.click.use()
            editorPane.allTracksPane.editorTrackArea.suggestIncompatibleTooltip(false)
            when (currentClick) {
                is Click.CreateSelection -> Localization.getVar("editor.status.creatingSelection").use()
                is Click.DragSelection -> {
                    if (currentClick.incompatibleTracks.use()) {
                        editorPane.allTracksPane.editorTrackArea.suggestIncompatibleTooltip(true)
                    }
                    
                    var res = Localization.getVar("editor.status.draggingSelection").use()
                    if (currentClick.wouldBeDeleted.use() && !currentClick.isNew) {
                        res += " " + Localization.getVar("editor.status.draggingSelection.willBeDeleted").use()
                    } else if (currentClick.placementInvalidDuplicates) {
                        res += " " + Localization.getVar("editor.status.draggingSelection.noDuplicates").use()
                    } else if (currentClick.collidesWithOtherBlocks.use()) {
                        res += " " + Localization.getVar("editor.status.draggingSelection.collides").use()
                    } else if (currentClick.isPlacementInvalid.use()) {
                        res += " " + Localization.getVar("editor.status.draggingSelection.invalidPlacement").use()
                    }
                    res
                }
                is Click.MoveMarker -> {
                    when (currentClick.type) {
                        MarkerType.PLAYBACK_START ->
                            Localization.getVar("editor.status.movingPlaybackStart").use()
                        MarkerType.MUSIC_FIRST_BEAT ->
                            Localization.getVar("editor.status.movingMusicFirstBeat").use()
                    }
                }
                is Click.MoveTempoChange -> {
                    val valid = currentClick.isCurrentlyValid.use()
                    var res = Localization.getVar("editor.status.tempoChangeTool.dragging").use()
                    if (!valid) {
                        res += " " + Localization.getVar("editor.status.tempoChangeTool.dragging.invalidPlacement").use()
                    }
                    res
                }
                is Click.DragMusicVolume -> {
                    val valid = currentClick.isCurrentlyValid.use()
                    var res = Localization.getVar("editor.status.musicVolumeTool.dragging").use()
                    if (!valid) {
                        res += " " + Localization.getVar("editor.status.musicVolumeTool.dragging.invalidPlacement").use()
                    }
                    res
                }
                Click.None -> {
                    when (tool) {
                        Tool.SELECTION -> {
                            var res = Localization.getVar("editor.status.selectionTool").use()
                            if (selectedBlocks.isNotEmpty()) {
                                // Size doesn't have to be a var b/c the status gets updated during a new selection
                                res += " " + Localization.getVar("editor.status.selectionTool.selectedCount", Var(listOf(selectedBlocks.keys.size)))
                            }
                            res
                        }
                        Tool.TEMPO_CHANGE -> Localization.getVar("editor.status.tempoChangeTool").use()
                        Tool.MUSIC_VOLUME -> Localization.getVar("editor.status.musicVolumeTool").use()
                        Tool.TIME_SIGNATURE -> Localization.getVar("editor.status.timeSignatureTool").use()
                    }
                }
            }
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        var inputConsumed = false
        val ctrl = Gdx.input.isControlDown()
        val alt = Gdx.input.isAltDown()
        val shift = Gdx.input.isShiftDown()
        val currentClick = click.getOrCompute()
        val state = playState.getOrCompute()
        
        if (settings.editorArrowKeysLikeScroll.getOrCompute()) {
            if (keycode == Input.Keys.UP) {
                Gdx.input.inputProcessor.scrolled(0f, -1f)
                inputConsumed = true
            } else if (keycode == Input.Keys.DOWN) {
                Gdx.input.inputProcessor.scrolled(0f, 1f)
                inputConsumed = true
            }
        }

        val contextMenuActive = sceneRoot.isContextMenuActive()
        val dialogActive = sceneRoot.isDialogActive()
        if (!inputConsumed && !contextMenuActive && (!dialogActive || sceneRoot.getCurrentRootDialog() == editorPane.playtestDialog)) {
            if (keycode == Input.Keys.SPACE) { // SPACE: Play state (or in Playtest dialog)
                if (!alt && !ctrl && currentClick == Click.None) {
                    if (state == PlayState.STOPPED) {
                        if (!shift) {
                            changePlayState(PlayState.PLAYING)
                            inputConsumed = true
                        }
                    } else {
                        if (state == PlayState.PLAYING) {
                            changePlayState(if (shift) PlayState.PAUSED else PlayState.STOPPED)
                        } else { // PAUSED
                            changePlayState(PlayState.PLAYING)
                        }
                        inputConsumed = true
                    }
                }
            }
        }
        if (!inputConsumed && !contextMenuActive && !dialogActive) {
            if (keycode in MOVE_WINDOW_KEYCODES) {
                pressedButtons += keycode
                inputConsumed = true
            } else {
                if (keycode == Input.Keys.HOME) {
                    val width = editorPane.allTracksPane.editorTrackArea.beatWidth.get()
                    if (!ctrl && !alt && !shift) {
                        // HOME: Jump to beat 0
                        cameraPan = CameraPan(0.25f, trackView.beat.get(), 0f)
                        inputConsumed = true
                    } else if (!ctrl && !alt && shift) {
                        // Shift+HOME: Jump to playback start
                        cameraPan = CameraPan(0.25f, trackView.beat.get(), (playbackStart.get() - width / 2f).coerceAtLeast(0f))
                        inputConsumed = true
                    } else if (ctrl && !alt && shift) {
                        // Ctrl+Shift+HOME: Jump to playback start
                        cameraPan = CameraPan(0.25f, trackView.beat.get(), (musicFirstBeat.get() - width / 2f).coerceAtLeast(0f))
                        inputConsumed = true
                    }
                } else if (keycode == Input.Keys.END) {
                    if (!ctrl && !alt && !shift) {
                        // END: Jump to stopping position
                        if (this.blocks.isNotEmpty()) {
                            cameraPan = CameraPan(0.25f, trackView.beat.get(), (container.stopPosition.get()).coerceAtLeast(0f))
                        }
                        inputConsumed = true
                    }
                } else if (!shift && !alt && !ctrl) {
                    when (keycode) {
                        Input.Keys.DEL, Input.Keys.FORWARD_DEL -> { // BACKSPACE or DELETE: Delete selection
                            if (currentClick == Click.None && state == PlayState.STOPPED) {
                                val selected = selectedBlocks.keys.toList()
                                if (!ctrl && !alt && !shift && selected.isNotEmpty()) {
                                    this.mutate(ActionGroup(SelectionAction(selected.toSet(), emptySet()), DeletionAction(selected)))
                                    forceUpdateStatus.invert()
                                }
                                inputConsumed = true
                            }
                        }
                        Input.Keys.HOME -> { // HOME: Jump to beat 0
                            cameraPan = CameraPan(0.25f, trackView.beat.get(), 0f)
                            inputConsumed = true
                        }
                        Input.Keys.END -> { // END: Jump to stopping position
                            if (this.blocks.isNotEmpty()) {
                                cameraPan = CameraPan(0.25f, trackView.beat.get(), (container.stopPosition.get()).coerceAtLeast(0f))
                            }
                            inputConsumed = true
                        }
                        in Input.Keys.NUM_0..Input.Keys.NUM_9 -> { // 0..9: Tools
                            if (!ctrl && !alt && !shift && currentClick == Click.None) {
                                val number = (if (keycode == Input.Keys.NUM_0) 10 else keycode - Input.Keys.NUM_0) - 1
                                if (number in 0 until Tool.VALUES.size) {
                                    changeTool(Tool.VALUES.getOrNull(number) ?: Tool.SELECTION)
                                    inputConsumed = true
                                }
                            }
                        }
                        Input.Keys.T -> {
                            if (!shift && !alt && !ctrl && currentClick == Click.None) {
                                val tapalongPane = editorPane.toolbar.tapalongPane
                                if (tapalongPane.apparentVisibility.get()) {
                                    tapalongPane.tap()
                                }
                            }
                        }
                        Input.Keys.R -> {
                            if (!shift && !alt && !ctrl && currentClick == Click.None) {
                                val tapalongPane = editorPane.toolbar.tapalongPane
                                if (tapalongPane.apparentVisibility.get()) {
                                    tapalongPane.reset()
                                }
                            }
                        }
                    }
                }
            }
            if (!inputConsumed && allowedToEdit.get()) {
                when (keycode) {
                    Input.Keys.F1 -> { // F1: Open help menu
                        if (!ctrl && !alt && !shift) {
                            attemptOpenHelpDialog()
                            inputConsumed = true
                        }
                    }
                    Input.Keys.Z -> { // CTRL+Z: Undo // CTRL+SHIFT+Z: Redo
                        if (ctrl && !alt) {
                            if (shift) {
                                attemptRedo()
                            } else {
                                attemptUndo()
                            }
                            inputConsumed = true
                        }
                    }
                    Input.Keys.Y -> { // CTRL+Y: Redo
                        if (ctrl && !alt && !shift) {
                            attemptRedo()
                            inputConsumed = true
                        }
                    }
                    Input.Keys.S -> { // CTRL+S: Save // CTRL+ALT+S: Save As
                        if (ctrl && !shift) {
                            attemptSave(alt)
                            inputConsumed = true
                        }
                    }
                    Input.Keys.E -> { // CTRL+SHIFT+E: Export
                        if (ctrl && shift && !alt) {
                            attemptExport()
                            inputConsumed = true
                        }
                    }
                    Input.Keys.O -> { // CTRL+O: Open
                        if (ctrl && !shift && !alt) {
                            attemptLoad(null)
                            inputConsumed = true
                        }
                    }
                    Input.Keys.N -> { // CTRL+N: New
                        if (ctrl && !shift && !alt) {
                            attemptNewLevel()
                            inputConsumed = true
                        }
                    }
                }
            }
        } else if (!contextMenuActive && dialogActive) {
            val currentRootDialog = sceneRoot.getCurrentRootDialog()
            // ESC: Close dialog // F1: Close help menu
            if (currentRootDialog is EditorDialog && currentRootDialog.canCloseWithEscKey()
                    && ((currentRootDialog is HelpDialog && keycode == Input.Keys.F1) || keycode == Input.Keys.ESCAPE)) {
                if (!ctrl && !alt && !shift) {
                    currentRootDialog.attemptClose()
                    inputConsumed = true
                }
            } else if (!engine.autoInputs && !engine.inputter.areInputsLocked) {
                val keyboardKeybinds = inputKeymapKeyboard
                when (keycode) {
                    keyboardKeybinds.buttonDpadUp -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(false, InputType.DPAD_UP)
                        }
                        inputConsumed = true
                    }
                    keyboardKeybinds.buttonDpadDown -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(false, InputType.DPAD_DOWN)
                        }
                        inputConsumed = true
                    }
                    keyboardKeybinds.buttonDpadLeft -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(false, InputType.DPAD_LEFT)
                        }
                        inputConsumed = true
                    }
                    keyboardKeybinds.buttonDpadRight -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(false, InputType.DPAD_RIGHT)
                        }
                        inputConsumed = true
                    }
                    keyboardKeybinds.buttonA -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(false, InputType.A)
                        }
                        inputConsumed = true
                    }
                }
            }
        } else if (contextMenuActive && !dialogActive) {
            val currentRootCtxMenu = sceneRoot.getCurrentRootContextMenu()
            if (currentRootCtxMenu != null && keycode == Input.Keys.ESCAPE) {
                if (!ctrl && !alt && !shift) {
                    sceneRoot.hideRootContextMenu()
                    inputConsumed = true
                }
            }
        }

        inputConsumed = sceneRoot.inputSystem.keyDown(keycode) || inputConsumed
        return inputConsumed
    }

    override fun keyTyped(character: Char): Boolean {
        var inputConsumed: Boolean = sceneRoot.inputSystem.keyTyped(character)
        if (!inputConsumed) {
            // Future impl here if needed. Scene root takes priority
        }

        return inputConsumed
    }

    override fun keyUp(keycode: Int): Boolean {
        var inputConsumed = pressedButtons.remove(keycode)
        
        if (!engine.autoInputs && !engine.inputter.areInputsLocked) {
            val keyboardKeybinds = inputKeymapKeyboard
            when (keycode) {
                keyboardKeybinds.buttonDpadUp -> {
                    engine.postRunnable {
                        engine.inputter.onButtonPressed(true, InputType.DPAD_UP)
                    }
                    inputConsumed = true
                }
                keyboardKeybinds.buttonDpadDown -> {
                    engine.postRunnable {
                        engine.inputter.onButtonPressed(true, InputType.DPAD_DOWN)
                    }
                    inputConsumed = true
                }
                keyboardKeybinds.buttonDpadLeft -> {
                    engine.postRunnable {
                        engine.inputter.onButtonPressed(true, InputType.DPAD_LEFT)
                    }
                    inputConsumed = true
                }
                keyboardKeybinds.buttonDpadRight -> {
                    engine.postRunnable {
                        engine.inputter.onButtonPressed(true, InputType.DPAD_RIGHT)
                    }
                    inputConsumed = true
                }
                keyboardKeybinds.buttonA -> {
                    engine.postRunnable {
                        engine.inputter.onButtonPressed(true, InputType.A)
                    }
                    inputConsumed = true
                }
            }
        }
    
        if (sceneRoot.inputSystem.keyUp(keycode)) {
            inputConsumed = true
        }
        
        return inputConsumed
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val currentClick: Click = click.getOrCompute()
        var inputConsumed = false
        this.suggestPanCameraDir = 0
        if (this.playState.getOrCompute() == PlayState.STOPPED) {
            when (currentClick) {
                is Click.DragSelection -> {
                    if (button == Input.Buttons.LEFT) {
                        if (currentClick.wouldBeDeleted.get() && !currentClick.isNew) {
                            val prevSelection = this.selectedBlocks.keys.toList()
                            currentClick.abortAction()
                            this.mutate(DeletionAction(prevSelection))
                        } else if (!currentClick.isPlacementInvalid.get()) {
                            val prevSelection = this.selectedBlocks.keys.toList()
                            currentClick.complete()
                            if (currentClick.isNew) {
                                this.mutate(ActionGroup(PlaceAction(currentClick.blocks.toList()), SelectionAction(prevSelection.toSet(), currentClick.blocks.toSet())))
                            } else {
                                this.addActionWithoutMutating(MoveAction(currentClick.blocks.associateWith { block ->
                                    MoveAction.Pos(currentClick.originalRegions.getValue(block), Click.DragSelection.BlockRegion(block.beat, block.trackIndex))
                                }))
                            }
                        } else {
                            currentClick.abortAction()
                        }

                        click.set(Click.None)
                        inputConsumed = true
                    } else if (button == Input.Buttons.RIGHT) {
                        // Cancel the drag
                        currentClick.abortAction()
                        click.set(Click.None)
                        inputConsumed = true
                    }
                }
                is Click.CreateSelection -> {
                    if (button == Input.Buttons.RIGHT) {
                        // Cancel the drag
                        currentClick.abortAction()
                        click.set(Click.None)
                        inputConsumed = true
                    } else if (button == Input.Buttons.LEFT) {
                        val previousSelection = this.selectedBlocks.keys.toSet()
                        val isCtrlDown = Gdx.input.isControlDown()
                        val isShiftDown = Gdx.input.isShiftDown()
                        val isAltDown = Gdx.input.isAltDown()
                        val xorSelectMode = isShiftDown && !isCtrlDown && !isAltDown

                        val newSelection: MutableSet<Block>
                        if (xorSelectMode) {
                            newSelection = previousSelection.toMutableSet()
                            blocks.forEach { block ->
                                if (currentClick.isBlockInSelection(block)) {
                                    if (block in previousSelection) {
                                        newSelection.remove(block)
                                    } else {
                                        newSelection.add(block)
                                    }
                                }
                            }
                        } else {
                            newSelection = mutableSetOf()
                            blocks.forEach { block ->
                                if (currentClick.isBlockInSelection(block)) {
                                    newSelection.add(block)
                                }
                            }
                        }

                        val selectionAction = SelectionAction(previousSelection, newSelection)
                        this.mutate(selectionAction)

                        click.set(Click.None)
                        inputConsumed = true
                    }
                }
                is Click.MoveMarker -> {
                    when (currentClick.type) {
                        MarkerType.PLAYBACK_START, MarkerType.MUSIC_FIRST_BEAT -> {
                            if (button == Input.Buttons.RIGHT) {
                                val didChange = currentClick.complete()
                                if (didChange) {
                                    val peek = peekAtUndoStack()
                                    if (!settings.editorDetailedMarkerUndo.getOrCompute() && peek != null && peek is MoveMarkerAction && peek.marker == currentClick.type) {
                                        peek.next = currentClick.point.get()
                                    } else {
                                        this.addActionWithoutMutating(MoveMarkerAction(currentClick.type, currentClick.originalPosition, currentClick.point.get()))
                                    }
                                }
                            }
                        }
                    }
                    click.set(Click.None)
                    inputConsumed = true
                }
                is Click.MoveTempoChange -> {
                    if (button == Input.Buttons.RIGHT) {
                        currentClick.abortAction()
                    } else if (button == Input.Buttons.LEFT) {
                        val result = currentClick.complete()
                        if (result != null) {
                            this.mutate(MoveTempoChangeAction(currentClick.tempoChange, result))
                        }
                    }
                    click.set(Click.None)
                    inputConsumed = true
                }
                is Click.DragMusicVolume -> {
                    if (button == Input.Buttons.RIGHT) {
                        currentClick.abortAction()
                    } else if (button == Input.Buttons.LEFT) {
                        val result = currentClick.complete()
                        if (result != null) {
                            this.mutate(ChangeMusicVolumeAction(currentClick.musicVol, result))
                        }
                    }
                    click.set(Click.None)
                    inputConsumed = true
                }
                Click.None -> { // Not an else so that when new Click types are added, a compile error is generated
                }
            }
        }

        return inputConsumed || sceneRoot.inputSystem.touchUp(screenX, screenY, pointer, button)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        var inputConsumed = false

        val allTracksPane = editorPane.allTracksPane
        val currentClick = click.getOrCompute()
        val vec = sceneRoot.screenToUI(Vector2Stack.getAndPush().set(screenX.toFloat(), screenY.toFloat()))
        if (currentClick is Click.CreateSelection || currentClick is Click.DragSelection || currentClick is Click.MoveMarker) {
            allTracksPane.editorTrackArea.onMouseMovedOrDragged(vec.x, vec.y)
            inputConsumed = true
        }

        this.suggestPanCameraDir = 0
        if (currentClick is Click.PansCameraOnDrag && panWhenDraggingAtEdge.getOrCompute()) {
            val thisPos = allTracksPane.getPosRelativeToRoot(Vector2Stack.getAndPush())
            thisPos.x = vec.x - thisPos.x
            thisPos.y = vec.y - thisPos.y

            val bufferZone = 20f
            val thisWidth = allTracksPane.bounds.width.get()
            if (thisPos.x - bufferZone < allTracksPane.sidebarWidth.get()) {
                this.suggestPanCameraDir = -1
            }
            if (thisPos.x + bufferZone > thisWidth) {
                this.suggestPanCameraDir = +1
            }

            Vector2Stack.pop()
        }

        Vector2Stack.pop()
        
        inputConsumed = sceneRoot.inputSystem.touchDragged(screenX, screenY, pointer) || inputConsumed

        return inputConsumed
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return sceneRoot.inputSystem.touchDown(screenX, screenY, pointer, button)
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return sceneRoot.inputSystem.mouseMoved(screenX, screenY)
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return sceneRoot.inputSystem.scrolled(amountX, amountY)
    }

    fun getDebugString(): String {
        val click = this.click.getOrCompute()
        val clickDebugString = click.getDebugString()
        return """timing: ${timing.seconds}

Click: ${click.javaClass.simpleName}${if (clickDebugString.isNotEmpty()) "\n$clickDebugString" else ""}
engine.events: ${engine.events.size}
autosave: ${DecimalFormats.format("0.0", timeUntilAutosave)}
trackView: ${trackView.beat.get()} | ${trackView.renderScale.get()} @ (${trackView.pxPerBeat.get()} px/beat)
path: ${sceneRoot.mainLayer.lastHoveredElementPath.map { "${it::class.java.simpleName}" }}
cameraOffsetCurrent: ${cameraOffset.current}
cameraOffsetTarget: ${cameraOffset.target}
suggest: $suggestPanCameraDir
musicSec: ${engine.musicData.getCorrectMusicPlayerPositionAt(engine.seconds) / 1000}
lastBlockPos: ${container.lastBlockPosition.get()}
endBlockPos: ${container.endBlockPosition.get()}
"""
        //path: ${sceneRoot.dialogLayer.lastHoveredElementPath.map { "${it::class.java.simpleName} [${it.bounds.x.getOrCompute()}, ${it.bounds.y.getOrCompute()}, ${it.bounds.width.getOrCompute()}, ${it.bounds.height.getOrCompute()}]" }}
    }

    // Lwjgl3WindowListener functions:

    override fun filesDropped(files: Array<out String>?) {
        if (files == null || files.isEmpty()) return

        val firstPath = files.first()
        when (val currentDialog: UIElement? = sceneRoot.getCurrentRootDialog()) {
            is MusicDialog -> {
                if (MusicDialog.SUPPORTED_MUSIC_EXTENSIONS.any { firstPath.endsWith(it.substring(1)) }) {
                    currentDialog.attemptSelectMusic(firstPath)
                }
            }
            else -> {
                if (firstPath.endsWith(".${Container.PROJECT_FILE_EXTENSION}") || firstPath.endsWith(".${Container.LEVEL_FILE_EXTENSION}")) {
                    attemptLoad(firstPath)
                }
            }
        }
    }

    override fun created(window: Lwjgl3Window?) {
    }

    override fun iconified(isIconified: Boolean) {
    }

    override fun maximized(isMaximized: Boolean) {
    }

    override fun focusLost() {
    }

    override fun focusGained() {
    }

    override fun closeRequested(): Boolean {
        return true
    }

    override fun refreshRequested() {
    }

    data class BeatLines(var active: Boolean = false, var fromBeat: Int = 0, var toBeat: Int = 0)
}
