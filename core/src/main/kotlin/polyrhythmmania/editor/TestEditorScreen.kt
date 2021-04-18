package polyrhythmmania.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.SceneRoot
import io.github.chrislo27.paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.engine.Engine
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.world.World
import polyrhythmmania.world.render.GBA2Tileset
import polyrhythmmania.world.render.WorldRenderer


class TestEditorScreen(main: PRManiaGame) : PRManiaScreen(main) {

    val world: World = World()
    val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem()
    val timing: TimingProvider = SimpleTimingProvider { 
        Gdx.app.postRunnable { throw it }
        true
    } //soundSystem
    val engine: Engine = Engine(timing, world, soundSystem)
    val renderer: WorldRenderer by lazy {
        WorldRenderer(world, GBA2Tileset(AssetRegistry["tileset_gba"]))
    }
    val frameBuffer: FrameBuffer


    private val uiCamera: OrthographicCamera = OrthographicCamera()
    private var root: SceneRoot = SceneRoot(Gdx.graphics.width, Gdx.graphics.height)
        private set(value) {
            main.inputMultiplexer.removeProcessor(field.inputSystem)
            field = value
            main.inputMultiplexer.addProcessor(value.inputSystem)
        }

    init {
        frameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, 1280, 720, true, true)
        updateUISizing()
        populate()
    }
    
    private fun populate() {
        root = SceneRoot(Gdx.graphics.width, Gdx.graphics.height)
        root += EditorPane()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        
        super.render(delta)

        val batch = main.batch
        val frameBuffer = this.frameBuffer
        
        frameBuffer.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        renderer.render(batch, engine)
        frameBuffer.end()
        
//        batch.begin()
//        batch.draw(frameBuffer.colorBufferTexture, 0f, 0f, frameBuffer.width.toFloat(), frameBuffer.height.toFloat(), 0, 0, frameBuffer.width, frameBuffer.height, false, true)
//        batch.end()

        batch.projectionMatrix = uiCamera.combined
        batch.begin()

        root.renderAsRoot(batch)

        batch.end()
    }

    override fun renderUpdate() {
        super.renderUpdate()

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            populate()
        }
    }

    override fun show() {
        super.show()
        main.inputMultiplexer.removeProcessor(root.inputSystem)
        main.inputMultiplexer.addProcessor(root.inputSystem)
    }

    override fun hide() {
        super.hide()
        main.inputMultiplexer.removeProcessor(root.inputSystem)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        updateUISizing()
    }
    
    fun updateUISizing() {
        var width = Gdx.graphics.width.toFloat()
        var height = Gdx.graphics.height.toFloat()
        if (width < 1280f || height < 720f) {
            width = 1280f
            height = 720f
        }
        uiCamera.setToOrtho(false, width, height)
        uiCamera.update()
        root.resize(uiCamera)
    }

    override fun dispose() {
        frameBuffer.disposeQuietly()
    }
}