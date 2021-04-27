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
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.SceneRoot
import io.github.chrislo27.paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.engine.Engine
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.world.World
import polyrhythmmania.world.render.GBA2Tileset
import polyrhythmmania.world.render.GBATileset
import polyrhythmmania.world.render.WorldRenderer


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
    
    val trackView: TrackView = TrackView()
    
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
        debugRenderUpdate(delta)
        
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

    private fun debugRenderUpdate(delta: Float) {
        // FIXME remove
        val trackView = this.trackView
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            trackView.beat += 7f * delta
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            trackView.beat -= 7f * delta
        }
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
}