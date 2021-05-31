package polyrhythmmania.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.binding.invert
import io.github.chrislo27.paintbox.font.Markup
import io.github.chrislo27.paintbox.font.TextRun
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.SceneRoot
import io.github.chrislo27.paintbox.ui.UIElement
import io.github.chrislo27.paintbox.ui.contextmenu.ContextMenu
import io.github.chrislo27.paintbox.util.gdxutils.disposeQuietly
import io.github.chrislo27.paintbox.util.gdxutils.isAltDown
import io.github.chrislo27.paintbox.util.gdxutils.isControlDown
import io.github.chrislo27.paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.Container
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockEndState
import polyrhythmmania.editor.block.Instantiator
import polyrhythmmania.editor.music.EditorMusicData
import polyrhythmmania.editor.pane.dialog.MusicDialog
import polyrhythmmania.editor.undo.ActionGroup
import polyrhythmmania.editor.undo.ActionHistory
import polyrhythmmania.editor.undo.impl.*
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.engine.tempo.TempoMap
import polyrhythmmania.soundsystem.*
import polyrhythmmania.world.TemporaryEntity
import polyrhythmmania.world.World
import polyrhythmmania.world.render.WorldRenderer
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.math.floor


class Editor(val main: PRManiaGame, val sceneRoot: SceneRoot = SceneRoot(1280, 720))
    : ActionHistory<Editor>(), InputProcessor by sceneRoot.inputSystem, Disposable, Lwjgl3WindowListener {

    companion object {
        const val TRACK_INPUT_0: String = "input_0"
        const val TRACK_INPUT_1: String = "input_1"
        const val TRACK_INPUT_2: String = "input_2"
        const val TRACK_VFX_0: String = "vfx_0"
    }

    private val uiCamera: OrthographicCamera = OrthographicCamera()
    val previewFrameBuffer: FrameBuffer
    val waveformWindow: WaveformWindow
    

    val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem()
    val timing: TimingProvider = SimpleTimingProvider {
        Gdx.app.postRunnable { throw it }
        true
    } //soundSystem
    val container: Container = Container(this.soundSystem, this.timing)
    
    val world: World get() = container.world
    val engine: Engine get() = container.engine
    val renderer: WorldRenderer get() = container.renderer


    // Default markup used for blocks, bold is inverted
    val blockMarkup: Markup = Markup(mapOf(
            "bold" to main.mainFontBordered,
            "italic" to main.mainFontItalicBordered,
            "bolditalic" to main.mainFontBoldItalicBordered,
            "rodin" to main.fontRodinFixedBordered,
            "prmania_icons" to main.fontIcons,
    ), TextRun(main.mainFontBoldBordered, ""), Markup.FontStyles("bold", "italic", "bolditalic"))

    val tracks: List<Track> = listOf(
            Track(TRACK_INPUT_0, EnumSet.of(BlockType.INPUT)),
            Track(TRACK_INPUT_1, EnumSet.of(BlockType.INPUT)),
            Track(TRACK_INPUT_2, EnumSet.of(BlockType.INPUT)),
//            Track(TRACK_VFX_0, EnumSet.of(BlockType.VFX)),
    )
    val trackMap: Map<String, Track> = tracks.associateByTo(LinkedHashMap()) { track -> track.id }

    // Editor tooling states
    val playState: ReadOnlyVar<PlayState> = Var(PlayState.STOPPED)
    val trackView: TrackView = TrackView()
    val tool: ReadOnlyVar<Tool> = Var(Tool.SELECTION)
    val click: Var<Click> = Var(Click.None)
    val snapping: FloatVar = FloatVar(0.25f)
    val beatLines: BeatLines = BeatLines()
    var cameraPan: CameraPan? = null

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
    val metronomeEnabled: Var<Boolean> = Var(false)
    private var lastMetronomeBeat: Int = -1

    val engineBeat: FloatVar = FloatVar(engine.beat)

    /**
     * Call Var<Boolean>.invert() to force the status to be updated. Used when an undo or redo takes place.
     */
    private val forceUpdateStatus: Var<Boolean> = Var(false)
    val editorPane: EditorPane

    init {
        previewFrameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, 1280, 720, true, true)
        waveformWindow = WaveformWindow(this)
        soundSystem.setPaused(true)
        soundSystem.startRealtime()
    }

    init {
        engine.autoInputs = true
        engine.endSignalReceived.addListener {
            if (it.getOrCompute() && playState.getOrCompute() == PlayState.PLAYING) {
                changePlayState(PlayState.STOPPED)
            }
        }
        tool.addListener {
            beatLines.active = false
        }
    }

    init { // This init block should be LAST
        editorPane = EditorPane(this)
        sceneRoot += editorPane
        resize()
        bindStatusBar(editorPane.statusBarMsg)
    }

    fun render(delta: Float, batch: SpriteBatch) {
        val frameBuffer = this.previewFrameBuffer
        frameBuffer.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        renderer.render(batch, engine)
        frameBuffer.end()

        batch.projectionMatrix = uiCamera.combined
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
        val alt = Gdx.input.isAltDown()
        val shift = Gdx.input.isShiftDown()
        val delta = Gdx.graphics.deltaTime

        val currentPlayState = playState.getOrCompute()
        if (currentPlayState == PlayState.PLAYING) {
            timing.seconds += delta

            val currentEngineBeat = engine.beat
            engineBeat.set(currentEngineBeat)
            val floorBeat = floor(engine.beat).toInt()
            if (floorBeat > lastMetronomeBeat) {
                lastMetronomeBeat = floorBeat
                if (metronomeEnabled.getOrCompute()) {
                    engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_cowbell"))
                }
            }
        }

        click.getOrCompute().renderUpdate()

        // FIXME 
        val trackView = this.trackView
        if (!ctrl && !alt) {
            val panSpeed = 7f * delta * (if (shift) 10f else 1f)
            if (Input.Keys.D in pressedButtons || Input.Keys.RIGHT in pressedButtons) {
                trackView.beat.set((trackView.beat.getOrCompute() + panSpeed).coerceAtLeast(0f))
            }
            if (Input.Keys.A in pressedButtons || Input.Keys.LEFT in pressedButtons) {
                trackView.beat.set((trackView.beat.getOrCompute() - panSpeed).coerceAtLeast(0f))
            }
        }
    }

    /**
     * Call to change the "intermediate state" editor objects (like Blocks, editor tempo changes, etc)
     * into the [engine]. This mutates the [engine] state.
     */
    fun compileEditorIntermediates() {
        resetWorld()
        compileEditorTempos()
        compileEditorMusicInfo()
        compileEditorBlocks()
    }

    fun compileEditorBlocks() {
        engine.removeEvents(engine.events.toList())
        val events = mutableListOf<Event>()
        blocks.toList().forEach { block ->
            events.addAll(block.compileIntoEvents())
        }
        events.sortBy { it.beat }
        engine.addEvents(events)
    }

    fun compileEditorTempos() {
        // Set starting tempo
        val tempos = engine.tempos
        tempos.removeTempoChangesBulk(tempos.getAllTempoChanges())
        tempos.addTempoChange(TempoChange(0f, this.startingTempo.getOrCompute()))
        tempos.addTempoChangesBulk(this.tempoChanges.getOrCompute().toList())
    }

    fun compileEditorMusicInfo() {
        val engineMusicData = engine.musicData
        engineMusicData.beadsMusic = this.musicData.beadsMusic
        engineMusicData.loopParams = this.musicData.loopParams.getOrCompute()
        engineMusicData.firstBeatSec = this.musicData.firstBeatSec.getOrCompute()
        engineMusicData.musicFirstBeat = this.musicFirstBeat.getOrCompute()
        val player = engine.soundInterface.getCurrentMusicPlayer(engineMusicData.beadsMusic)
        if (player != null) {
            player.useLoopParams(engineMusicData.loopParams)
        }

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
    }

    fun attemptInstantiatorDrag(instantiator: Instantiator<Block>) {
        if (click.getOrCompute() != Click.None || playState.getOrCompute() != PlayState.STOPPED) return
        val currentTool = this.tool.getOrCompute()
        if (currentTool != Tool.SELECTION) return

        val newBlock: Block = instantiator.factory.invoke(instantiator, engine)

        val newClick = Click.DragSelection.create(this, listOf(newBlock), Vector2(0f, 0f), newBlock, true)
        if (newClick != null) {
            click.set(newClick)
        }
    }

    fun attemptMarkerMove(markerType: MarkerType, mouseBeat: Float) {
        if (click.getOrCompute() != Click.None || playState.getOrCompute() != PlayState.STOPPED) return
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
        if (canUndo() && click.getOrCompute() == Click.None && playState.getOrCompute() == PlayState.STOPPED) {
            this.undo()
            forceUpdateStatus.invert()
        }
    }

    fun attemptRedo() {
        if (canRedo() && click.getOrCompute() == Click.None && playState.getOrCompute() == PlayState.STOPPED) {
            this.redo()
            forceUpdateStatus.invert()
        }
    }

    fun attemptExitToTitle() {
        if (click.getOrCompute() == Click.None && playState.getOrCompute() == PlayState.STOPPED) {
            editorPane.openDialog(editorPane.exitConfirmDialog)
        }
    }
    
    fun attemptSave(forceSaveAs: Boolean) {
        if (click.getOrCompute() == Click.None && playState.getOrCompute() == PlayState.STOPPED) {
            editorPane.openDialog(editorPane.saveDialog.prepareShow(forceSaveAs))
        }
    }
    
    fun attemptLoad() {
        if (click.getOrCompute() == Click.None && playState.getOrCompute() == PlayState.STOPPED) {
            editorPane.openDialog(editorPane.loadDialog.prepareShow())
        }
    }

    fun changeTool(tool: Tool) {
        if (click.getOrCompute() != Click.None || playState.getOrCompute() != PlayState.STOPPED) return
        this.tool as Var
        this.tool.set(tool)
    }

    fun changePlayState(newState: PlayState) {
        this.playState as Var
        val lastState = this.playState.getOrCompute()
        if (lastState == newState) return
        if (this.click.getOrCompute() != Click.None) return
        if (lastState == PlayState.STOPPED && newState == PlayState.PAUSED) return

        if (lastState == PlayState.STOPPED && newState == PlayState.PLAYING) {
            compileEditorIntermediates()
            engine.resetEndSignal()
            val newSeconds = engine.tempos.beatsToSeconds(this.playbackStart.getOrCompute())
            timing.seconds = newSeconds
            engine.musicData.update()
            val engineBeatFloor = floor(engine.beat)
            lastMetronomeBeat = if (engineBeatFloor == engine.beat) (engineBeatFloor.toInt() - 1) else engineBeatFloor.toInt()
            val player = engine.soundInterface.getCurrentMusicPlayer(engine.musicData.beadsMusic)
            if (player != null) {
                engine.musicData.setPlayerPositionToCurrentSec()
                player.pause(false)
            }
        } else if (newState == PlayState.STOPPED) {
            resetWorld()
            timing.seconds = engine.tempos.beatsToSeconds(this.playbackStart.getOrCompute())
        }

        if (newState == PlayState.PLAYING) {
            soundSystem.setPaused(false)
        } else {
            soundSystem.setPaused(true)
        }

        this.playState.set(newState)
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
        root.showRootContextMenu(contextMenu)
    }

    fun resize() {
        var width = Gdx.graphics.width.toFloat()
        var height = Gdx.graphics.height.toFloat()
        // UI scale
        val uiScale = 1f // Note: scales don't work with inputs currently
        width /= uiScale
        height /= uiScale
        if (width < 1280f || height < 720f) {
            width = 1280f
            height = 720f
        }

        uiCamera.setToOrtho(false, width, height)
        uiCamera.update()
        sceneRoot.resize(uiCamera)
    }

    override fun dispose() {
        previewFrameBuffer.disposeQuietly()
        waveformWindow.disposeQuietly()

        soundSystem.setPaused(true)
        container.disposeQuietly()
    }

    fun addBlock(block: Block) {
        val blocks = this.blocks as MutableList
        if (block !in blocks) {
            blocks.add(block)
        }
    }

    fun addBlocks(blocksToAdd: List<Block>) {
        val blocks = this.blocks as MutableList
        blocksToAdd.forEach { block ->
            if (block !in blocks) {
                blocks.add(block)
            }
        }
    }

    fun removeBlock(block: Block) {
        val blocks = this.blocks as MutableList
        blocks.remove(block)
        (this.selectedBlocks as MutableMap).remove(block)
        if (block.ownedContextMenu != null) {
            if (sceneRoot.isContextMenuActive())
                sceneRoot.hideRootContextMenu()
            block.ownedContextMenu = null
        }
    }

    fun removeBlocks(blocksToAdd: List<Block>) {
        val blocks = this.blocks as MutableList
        blocks.removeAll(blocksToAdd)
        this.selectedBlocks as MutableMap
        blocks.forEach { block ->
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
            when (currentClick) {
                is Click.CreateSelection -> Localization.getVar("editor.status.creatingSelection").use()
                is Click.DragSelection -> {
                    var res = Localization.getVar("editor.status.draggingSelection").use()
                    if (currentClick.wouldBeDeleted.use() && !currentClick.isNew) {
                        res += " " + Localization.getVar("editor.status.draggingSelection.willBeDeleted").use()
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
                    }
                }
            }
        }
    }

    private val pressedButtons: MutableSet<Int> = mutableSetOf()

    override fun keyDown(keycode: Int): Boolean {
        var inputConsumed = false
        val ctrl = Gdx.input.isControlDown()
        val alt = Gdx.input.isAltDown()
        val shift = Gdx.input.isShiftDown()
        val currentClick = click.getOrCompute()
        val state = playState.getOrCompute()
        
        if (sceneRoot.isContextMenuActive()) return false
        if (sceneRoot.isDialogActive()) return false

        when (keycode) {
            Input.Keys.D, Input.Keys.A, Input.Keys.LEFT, Input.Keys.RIGHT -> {
                pressedButtons += keycode
                inputConsumed = true
            }
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
                if (currentClick == Click.None && state == PlayState.STOPPED) {
                    cameraPan = CameraPan(0.25f, trackView.beat.getOrCompute(), 0f)
                    inputConsumed = true
                }
            }
            Input.Keys.END -> { // END: Jump to first BlockEndState OR last block
                if (currentClick == Click.None && state == PlayState.STOPPED) {
                    val blocks = this.blocks.sortedBy { it.beat }
                    if (blocks.isNotEmpty()) {
                        val firstEndBlock: BlockEndState? = blocks.firstOrNull { it is BlockEndState } as? BlockEndState?
                        val targetBlock: Block = firstEndBlock ?: blocks.last()
                        cameraPan = CameraPan(0.25f, trackView.beat.getOrCompute(), (targetBlock.beat).coerceAtLeast(0f))
                    }
                    inputConsumed = true
                }
            }
            Input.Keys.SPACE -> { // SPACE: Play state
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
            Input.Keys.T -> {
                if (!shift && !alt && !ctrl && currentClick == Click.None) {
                    val tapalongPane = editorPane.toolbar.tapalongPane
                    if (tapalongPane.apparentVisibility.getOrCompute()) {
                        tapalongPane.tap()
                    }
                }
            }
            Input.Keys.Z -> { // CTRL+Z: Undo // CTRL+SHIFT+Z: Redo
                if (currentClick == Click.None && state == PlayState.STOPPED) {
                    if (ctrl && !alt) {
                        if (shift) {
                            attemptRedo()
                        } else {
                            attemptUndo()
                        }
                    }
                }
            }
            Input.Keys.Y -> { // CTRL+Y: Redo
                if (currentClick == Click.None && state == PlayState.STOPPED) {
                    if (ctrl && !alt && !shift) {
                        attemptRedo()
                    }
                }
            }
            Input.Keys.S -> { // CTRL+S: Save // CTRL+ALT+S: Save As
                if (currentClick == Click.None && state == PlayState.STOPPED) {
                    if (ctrl && !shift) {
                        attemptSave(alt)
                    }
                }
            }
            Input.Keys.O -> { // CTRL+O: Open
                if (currentClick == Click.None && state == PlayState.STOPPED) {
                    if (ctrl && !shift && !alt) {
                        attemptLoad()
                    }
                }
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
        }

        return inputConsumed || sceneRoot.inputSystem.keyDown(keycode)
    }

    override fun keyTyped(character: Char): Boolean {
        if (sceneRoot.isContextMenuActive()) return false
        if (sceneRoot.isDialogActive()) return false

        var inputConsumed: Boolean = sceneRoot.inputSystem.keyTyped(character)
        if (!inputConsumed) {

        }

        return inputConsumed
    }

    override fun keyUp(keycode: Int): Boolean {
        if (pressedButtons.remove(keycode)) return true
        return sceneRoot.inputSystem.keyUp(keycode)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val currentClick: Click = click.getOrCompute()
        var inputConsumed = false
        if (this.playState.getOrCompute() == PlayState.STOPPED) {
            when (currentClick) {
                is Click.DragSelection -> {
                    if (button == Input.Buttons.LEFT) {
                        if (currentClick.wouldBeDeleted.getOrCompute() && !currentClick.isNew) {
                            val prevSelection = this.selectedBlocks.keys.toList()
                            currentClick.abortAction()
                            this.mutate(DeletionAction(prevSelection))
                        } else if (!currentClick.isPlacementInvalid.getOrCompute()) {
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
                                    if (peek != null && peek is MoveMarkerAction && peek.marker == currentClick.type) {
                                        peek.next = currentClick.point.getOrCompute()
                                    } else {
                                        this.addActionWithoutMutating(MoveMarkerAction(currentClick.type, currentClick.originalPosition, currentClick.point.getOrCompute()))
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

        val currentClick = click.getOrCompute()
        if (currentClick is Click.CreateSelection || currentClick is Click.DragSelection || currentClick is Click.MoveMarker) {
            editorPane.allTracksPane.editorTrackArea.onMouseMovedOrDragged(screenX.toFloat(), screenY.toFloat())
            inputConsumed = true
        }

        return inputConsumed || sceneRoot.inputSystem.touchDragged(screenX, screenY, pointer)
    }

    fun getDebugString(): String {
        return """Click: ${click.getOrCompute().javaClass.simpleName}
engine.events: ${engine.events.size}
"""
        //path: ${sceneRoot.dialogLayer.lastHoveredElementPath.map { "${it::class.java.simpleName} [${it.bounds.x.getOrCompute()}, ${it.bounds.y.getOrCompute()}, ${it.bounds.width.getOrCompute()}, ${it.bounds.height.getOrCompute()}]" }}
    }
    
    // Lwjgl3WindowListener functions:
    
    override fun filesDropped(files: Array<out String>?) {
        if (files == null || files.isEmpty()) return
        
        val currentDialog: UIElement? = sceneRoot.getCurrentRootDialog()
        when (currentDialog) {
            is MusicDialog -> {
                // TODO pass along to MusicDialog once files are refactored
            }
            else -> {}
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
