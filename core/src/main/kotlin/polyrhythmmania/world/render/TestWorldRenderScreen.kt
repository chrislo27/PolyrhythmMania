package polyrhythmmania.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.sample.GdxAudioReader
import polyrhythmmania.util.DecimalFormats
import polyrhythmmania.world.*
import kotlin.system.measureNanoTime


class TestWorldRenderScreen(main: PRManiaGame) : PRManiaScreen(main) {

    companion object {
        private val music: BeadsMusic = GdxAudioReader.newMusic(Gdx.files.internal("debugetc/Polyrhythm.ogg"))
    }

    val world: World = World()
    val renderer: WorldRenderer by lazy {
        WorldRenderer(world, GBATileset(AssetRegistry["tileset_gba"]))
    }
    val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem()
    val timing: TimingProvider = soundSystem
    val engine: Engine = Engine(timing, world)

    private val player = music.createPlayer(soundSystem.audioContext)

    private var nanoTime = System.nanoTime()

    init {
        soundSystem.audioContext.out.addInput(player)
        soundSystem.startRealtime()

        engine.tempos.addTempoChange(TempoChange(0f, 129f))
        engine.tempos.addTempoChange(TempoChange(88f, 148.5f))

        addEvents()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val batch = main.batch

        renderer.render(batch, engine)

        super.render(delta)
    }

    override fun renderUpdate() {
        super.renderUpdate()

        if (timing is SimpleTimingProvider) {
            timing.seconds += Gdx.graphics.deltaTime
        }

//        val realtimeMsDelta = (System.nanoTime() - nanoTime) / 1000000.0
//        nanoTime = System.nanoTime()
//        val deltaMs = Gdx.graphics.deltaTime.toDouble() * 1000.0
//        println("$deltaMs \t $realtimeMsDelta \t ${deltaMs - realtimeMsDelta}")

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            Gdx.app.postRunnable {
                val timeToStop = measureNanoTime {
                    this.soundSystem.stopRealtime()
                }
//                println(timeToStop / 1000000.0)
                this.dispose()
                main.screen = TestWorldRenderScreen(main)
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            soundSystem.setPaused(!soundSystem.isPaused)
        }

        val camera = renderer.camera
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            camera.position.y += Gdx.graphics.deltaTime * +4f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            camera.position.y += Gdx.graphics.deltaTime * -4f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            camera.position.x += Gdx.graphics.deltaTime * +4f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.position.x += Gdx.graphics.deltaTime * -4f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.E)) {
            camera.zoom += Gdx.graphics.deltaTime * -1f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            camera.zoom += Gdx.graphics.deltaTime * +1f
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            camera.setToOrtho(false, 5 * (16f / 9f), 5f)
            camera.zoom = 1f
            camera.position.set(camera.zoom * camera.viewportWidth / 2.0f, camera.zoom * camera.viewportHeight / 2.0f, 0f)
        }
    }

    override fun getDebugString(): String {
        return """${DecimalFormats.format("0.000", player.position / 1000f)}
---
${engine.getDebugString()}
---
${renderer.getDebugString()}
"""

    }

    override fun dispose() {
        soundSystem.disposeQuietly()
    }

    private fun addEvents() {
        val events = mutableListOf<Event>()
        events += EventRowBlockSpawn(engine, world.rowA, 0, EntityRowBlock.Type.PISTON_A, 8f)
        events += EventRowBlockSpawn(engine, world.rowA, 4, EntityRowBlock.Type.PISTON_A, 10f)
        (8 until 16).forEach { i ->
            events += EventRowBlockSpawn(engine, world.rowA, i,  EntityRowBlock.Type.PLATFORM, 12f)
        }
        
        events += EventDeployRod(engine, world.rowA, 12f - 4)
        events += EventRowBlockExtend(engine, world.rowA, 0, 12f)
        events += EventRowBlockExtend(engine, world.rowA, 4, 14f)
        events += EventRowBlockRetract(engine, world.rowA, -1, 15.5f)

        events += EventDeployRod(engine, world.rowA, 16f - 4)
        events += EventRowBlockExtend(engine, world.rowA, 0, 16f)
//        events += EventRowBlockExtend(engine, world.rowA, 4, 18f)
        events += EventRowBlockRetract(engine, world.rowA, -1, 22f)

        events += EventRowBlockDespawn(engine, world.rowA, -1, 23f)



        events += EventDeployRod(engine, world.rowA, 28f - 4)
        events += EventRowBlockSpawn(engine, world.rowA, 0, EntityRowBlock.Type.PISTON_A, 24f)
        events += EventRowBlockSpawn(engine, world.rowA, 2, EntityRowBlock.Type.PISTON_A, 25f)
        events += EventRowBlockSpawn(engine, world.rowA, 4, EntityRowBlock.Type.PISTON_A, 26f)
        events += EventRowBlockSpawn(engine, world.rowA, 6, EntityRowBlock.Type.PISTON_A, 27f)
        (8 until 16).forEach { i ->
            events += EventRowBlockSpawn(engine, world.rowA, i, EntityRowBlock.Type.PLATFORM, 28f)
        }
        events += EventRowBlockExtend(engine, world.rowA, 0, 28f)
        events += EventRowBlockExtend(engine, world.rowA, 2, 29f)
        events += EventRowBlockExtend(engine, world.rowA, 4, 30f)
        events += EventRowBlockExtend(engine, world.rowA, 6, 31f)
        events += EventRowBlockRetract(engine, world.rowA, -1, 31.5f)

        events += EventDeployRod(engine, world.rowA, 32f - 4)
        events += EventRowBlockExtend(engine, world.rowA, 0, 32f)
        events += EventRowBlockExtend(engine, world.rowA, 2, 33f)
        events += EventRowBlockExtend(engine, world.rowA, 4, 34f)
        events += EventRowBlockExtend(engine, world.rowA, 6, 35f)
        events += EventRowBlockRetract(engine, world.rowA, -1, 38f)

        events += EventRowBlockDespawn(engine, world.rowA, -1, 39f)

        

        events += EventDeployRod(engine, world.rowA, 44f - 4)
        events += EventDeployRod(engine, world.rowDpad, 44f - 4)
        events += EventRowBlockSpawn(engine, world.rowA, 0, EntityRowBlock.Type.PISTON_A, 40f)
        events += EventRowBlockSpawn(engine, world.rowA, 2, EntityRowBlock.Type.PISTON_A, 41f)
        events += EventRowBlockSpawn(engine, world.rowDpad, 0, EntityRowBlock.Type.PLATFORM, 41f)
        events += EventRowBlockSpawn(engine, world.rowDpad, 1, EntityRowBlock.Type.PLATFORM, 41f)
        events += EventRowBlockSpawn(engine, world.rowDpad, 2, EntityRowBlock.Type.PISTON_DPAD, 41f)
        events += EventRowBlockSpawn(engine, world.rowA, 4, EntityRowBlock.Type.PISTON_A, 42f)
        events += EventRowBlockSpawn(engine, world.rowA, 6, EntityRowBlock.Type.PISTON_A, 43f)
        events += EventRowBlockSpawn(engine, world.rowDpad, 6, EntityRowBlock.Type.PISTON_DPAD, 43f)
        (8 until 16).forEach { i ->
            events += EventRowBlockSpawn(engine, world.rowA, i, EntityRowBlock.Type.PLATFORM, 44f)
            if (i >= 10)
                events += EventRowBlockSpawn(engine, world.rowDpad, i, EntityRowBlock.Type.PLATFORM, 44f)
        }
        events += EventRowBlockExtend(engine, world.rowA, 0, 44f)
        events += EventRowBlockExtend(engine, world.rowA, 2, 45f)
        events += EventRowBlockExtend(engine, world.rowDpad, 2, 45f)
        events += EventRowBlockExtend(engine, world.rowA, 4, 46f)
        events += EventRowBlockExtend(engine, world.rowA, 6, 47f)
        events += EventRowBlockExtend(engine, world.rowDpad, 6, 47f)
        events += EventRowBlockRetract(engine, world.rowA, -1, 47.5f)
        events += EventRowBlockRetract(engine, world.rowDpad, -1, 47.5f)

        events += EventDeployRod(engine, world.rowA, 48f - 4)
        events += EventDeployRod(engine, world.rowDpad, 48f - 4)
        events += EventRowBlockExtend(engine, world.rowA, 0, 48f)
        events += EventRowBlockExtend(engine, world.rowA, 2, 49f)
        events += EventRowBlockExtend(engine, world.rowDpad, 2, 49f)
        events += EventRowBlockExtend(engine, world.rowA, 4, 50f)
        events += EventRowBlockExtend(engine, world.rowA, 6, 51f)
        events += EventRowBlockExtend(engine, world.rowDpad, 6, 51f)
        events += EventRowBlockRetract(engine, world.rowA, -1, 54f)
        events += EventRowBlockRetract(engine, world.rowDpad, -1, 54f)

        events += EventRowBlockDespawn(engine, world.rowA, -1, 55f)
        events += EventRowBlockDespawn(engine, world.rowDpad, -1, 55f)

        engine.addEvents(events)
    }
}