package polyrhythmmania.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
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


class Editor(val main: PRManiaGame, val sceneRoot: SceneRoot = SceneRoot(1280, 720))
    : InputProcessor by sceneRoot.inputSystem, Disposable {

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

    val tracks: Map<String, Track> = listOf(
            Track("input_a", EnumSet.of(BlockType.INPUT)),
            Track("input_dpad", EnumSet.of(BlockType.INPUT)),
    ).associateByTo(LinkedHashMap()) { track -> track.id }

    // Editor tooling states
    val trackView: TrackView = TrackView()
    val tool: ReadOnlyVar<Tool> = Var(Tool.SELECTION)
    val click: ReadOnlyVar<Click> = Var(Click.None)
    val snapping: FloatVar = FloatVar(0.5f)
    val beatLines: BeatLines = BeatLines()

    // Editor scene states
    val blocks: MutableList<Block> = CopyOnWriteArrayList()

    init {
        trackView.renderScale = 0.5f
        frameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, 1280, 720, true, true)
        populateUIScene()
    }

    private fun populateUIScene() {
        sceneRoot += EditorPane(this)

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
                trackView.beat += 7f * delta
            }
            if (Input.Keys.A in pressedButtons) {
                trackView.beat -= 7f * delta
            }
        }
    }

    fun attemptInstantiatorDrag(instantiator: Instantiator) {
        if (click.getOrCompute() != Click.None) return
        val currentTool = this.tool.getOrCompute()
        if (currentTool != Tool.SELECTION) return

        val newBlock: Block = instantiator.factory.invoke(instantiator, this)

        val newClick = Click.DragSelection(this, listOf(newBlock))
        (click as Var).set(newClick)
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
        if ((pressedButtons as MutableSet).remove(keycode)) return true
        return sceneRoot.inputSystem.keyUp(keycode)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val currentClick: Click = click.getOrCompute()
        var inputConsumed = false
        when (currentClick) {
            is Click.DragSelection -> {
                if (button == Input.Buttons.LEFT) {
                    currentClick.complete()
                    (click as Var).set(Click.None)
                    inputConsumed = true
                } else if (button == Input.Buttons.RIGHT) {
                    // Cancel the drag
                    currentClick.abortAction()
                    (click as Var).set(Click.None)
                    inputConsumed = true
                }
            }
            is Click.CreateSelection -> {
                if (button == Input.Buttons.RIGHT) {
                    // Cancel the drag
                    currentClick.abortAction()
                    (click as Var).set(Click.None)
                    inputConsumed = true
                }
            }
            else -> {
            }
        }
        return inputConsumed || sceneRoot.inputSystem.touchUp(screenX, screenY, pointer, button)
    }

    fun getDebugString(): String {
        return """Click: ${click.getOrCompute().javaClass.simpleName}

"""
    }

    data class BeatLines(var active: Boolean = false, var fromBeat: Int = 0, var toBeat: Int = 0)
}