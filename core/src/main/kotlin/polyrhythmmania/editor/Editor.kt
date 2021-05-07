package polyrhythmmania.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.SceneRoot
import io.github.chrislo27.paintbox.util.gdxutils.disposeQuietly
import io.github.chrislo27.paintbox.util.gdxutils.isAltDown
import io.github.chrislo27.paintbox.util.gdxutils.isControlDown
import io.github.chrislo27.paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.editor.track.BlockType
import polyrhythmmania.editor.track.Track
import polyrhythmmania.editor.track.block.Block
import polyrhythmmania.editor.track.block.Instantiator
import polyrhythmmania.editor.undo.ActionHistory
import polyrhythmmania.editor.undo.impl.SelectionAction
import polyrhythmmania.engine.Engine
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.world.World
import polyrhythmmania.world.render.GBATileset
import polyrhythmmania.world.render.WorldRenderer
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.LinkedHashMap
import kotlin.math.E


class Editor(val main: PRManiaGame, val sceneRoot: SceneRoot = SceneRoot(1280, 720))
    : ActionHistory<Editor>(), InputProcessor by sceneRoot.inputSystem, Disposable {

    companion object {
        val TRACK_INPUT_A: String = "input_a"
        val TRACK_INPUT_DPAD: String = "input_dpad"
        val TRACK_VFX0: String = "vfx_0"
    }

    private val uiCamera: OrthographicCamera = OrthographicCamera()
    val frameBuffer: FrameBuffer


    val world: World = World()
    val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem()
    val timing: TimingProvider = SimpleTimingProvider {
        Gdx.app.postRunnable { throw it }
        true
    } //soundSystem
    val engine: Engine = Engine(timing, world, soundSystem)
    val renderer: WorldRenderer by lazy {
        WorldRenderer(world, GBATileset(AssetRegistry["tileset_gba"]))
    }

    val tracks: List<Track> = listOf(
            Track(TRACK_INPUT_A, EnumSet.of(BlockType.INPUT)),
            Track(TRACK_INPUT_DPAD, EnumSet.of(BlockType.INPUT)),
            Track(TRACK_VFX0, EnumSet.of(BlockType.VFX)),
    )
    val trackMap: Map<String, Track> = tracks.associateByTo(LinkedHashMap()) { track -> track.id }

    // Editor tooling states
    val trackView: TrackView = TrackView()
    val tool: ReadOnlyVar<Tool> = Var(Tool.SELECTION)
    val click: Var<Click> = Var(Click.None)
    val snapping: FloatVar = FloatVar(0.5f)
    val beatLines: BeatLines = BeatLines()

    // Editor scene states
    val blocks: List<Block> = CopyOnWriteArrayList()
    val selectedBlocks: Map<Block, Boolean> = WeakHashMap()

    val editorPane: EditorPane

    init {
        trackView.renderScale.set(0.5f)
        frameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, 1280, 720, true, true)
    }

    init { // This init block should be LAST
        editorPane = EditorPane(this)
        sceneRoot += editorPane
        resize(Gdx.graphics.width, Gdx.graphics.height)
    }

    fun render(delta: Float, batch: SpriteBatch) {
        val frameBuffer = this.frameBuffer
        frameBuffer.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        renderer.render(batch, engine)
        frameBuffer.end()

        batch.projectionMatrix = uiCamera.combined
        batch.begin()

        sceneRoot.renderAsRoot(batch)

        batch.end()
    }

    fun renderUpdate() {
        val ctrl = Gdx.input.isControlDown()
        val alt = Gdx.input.isAltDown()
        val shift = Gdx.input.isShiftDown()
        val delta = Gdx.graphics.deltaTime

        click.getOrCompute().renderUpdate()

        // FIXME 
        val trackView = this.trackView
        if (!ctrl && !alt && !shift) {
            if (Input.Keys.D in pressedButtons) {
                trackView.beat.set((trackView.beat.getOrCompute() + (7f * delta)).coerceAtLeast(0f))
            }
            if (Input.Keys.A in pressedButtons) {
                trackView.beat.set((trackView.beat.getOrCompute() - (7f * delta)).coerceAtLeast(0f))
            }
        }
    }

    fun attemptInstantiatorDrag(instantiator: Instantiator) {
        if (click.getOrCompute() != Click.None) return
        val currentTool = this.tool.getOrCompute()
        if (currentTool != Tool.SELECTION) return

        val newBlock: Block = instantiator.factory.invoke(instantiator, this)

        val newClick = Click.DragSelection.create(this, listOf(newBlock), Vector2(0f, 0f), newBlock, true)
        if (newClick != null) {
            click.set(newClick)
        }
    }

    fun changeTool(tool: Tool) {
        this.tool as Var
        this.tool.set(tool)
    }

    fun resize(w: Int, h: Int) {
        var width = Gdx.graphics.width.toFloat()
        var height = Gdx.graphics.height.toFloat()
        if (width < 1280f || height < 720f) {
            width = 1280f
            height = 720f
        }
        uiCamera.setToOrtho(false, width, height)
        uiCamera.update()
        sceneRoot.resize(uiCamera)
    }

    override fun dispose() {
        frameBuffer.disposeQuietly()
    }

    fun addBlock(block: Block) {
        this.blocks as MutableList
        if (block !in this.blocks) {
            this.blocks.add(block)
        }
    }

    fun addBlocks(blocks: List<Block>) {
        this.blocks as MutableList
        blocks.forEach { block ->
            if (block !in this.blocks) {
                this.blocks.add(block)
            }
        }
    }

    fun removeBlock(block: Block) {
        this.blocks as MutableList
        this.blocks.remove(block)
        (this.selectedBlocks as MutableMap).remove(block)
    }

    fun removeBlocks(blocks: List<Block>) {
        this.blocks as MutableList
        this.blocks.removeAll(blocks)
        this.selectedBlocks as MutableMap
        blocks.forEach { block ->
            this.selectedBlocks.remove(block)
        }
    }

    private val pressedButtons: MutableSet<Int> = mutableSetOf()

    override fun keyDown(keycode: Int): Boolean {
//        val ctrl = Gdx.input.isControlDown()
//        val alt = Gdx.input.isAltDown()
//        val shift = Gdx.input.isShiftDown()
//        if (!ctrl && !alt && !shift && keycode == Input.Keys.D) {
        when (keycode) {
            Input.Keys.D, Input.Keys.A -> {
                pressedButtons += keycode
                return true
            }
        }
//        }
        return sceneRoot.inputSystem.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        if (pressedButtons.remove(keycode)) return true
        return sceneRoot.inputSystem.keyUp(keycode)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val currentClick: Click = click.getOrCompute()
        var inputConsumed = false
        when (currentClick) {
            is Click.DragSelection -> {
                if (button == Input.Buttons.LEFT) {
                    currentClick.complete()
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
            else -> {
            }
        }
        return inputConsumed || sceneRoot.inputSystem.touchUp(screenX, screenY, pointer, button)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        var inputConsumed = false

        val currentClick = click.getOrCompute()
        if (currentClick is Click.CreateSelection || currentClick is Click.DragSelection) {
            editorPane.allTracksPane.editorTrackArea.onMouseMovedOrDragged(screenX.toFloat(), screenY.toFloat())
            inputConsumed = true
        }

        return inputConsumed || sceneRoot.inputSystem.touchDragged(screenX, screenY, pointer)
    }

    fun getDebugString(): String {
        return """Click: ${click.getOrCompute().javaClass.simpleName}

"""
    }

    data class BeatLines(var active: Boolean = false, var fromBeat: Int = 0, var toBeat: Int = 0)
}