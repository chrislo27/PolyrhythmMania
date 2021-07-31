package polyrhythmmania.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.isKeyJustReleased
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.world.*
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TilesetConfig
import kotlin.system.measureNanoTime


class TestWorldDunkScreen(main: PRManiaGame) : PRManiaScreen(main) {

    companion object {
//        private val music: BeadsMusic = GdxAudioReader.newMusic(Gdx.files.internal("music/Polyrhythm.ogg"))
    }

    val world: World = World().apply { 
        this.worldMode = WorldMode.DUNK
        resetWorld()
    }
    val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem()
    val timing: TimingProvider = soundSystem
    val engine: Engine = Engine(timing, world, soundSystem, null)
    val renderer: WorldRenderer by lazy {
        WorldRenderer(world, Tileset(AssetRegistry.get<PackedSheet>("tileset_gba")).apply { 
            TilesetConfig.createGBA1TilesetConfig().applyTo(this)
        })
    }

//    private val player: MusicSamplePlayer = music.createPlayer(soundSystem.audioContext).apply {
//        this.gain = 0.75f
////        this.loopStartMs = 3725f
//        this.loopEndMs = 40928f //33482f
//        this.loopType = SamplePlayer.LoopType.LOOP_FORWARDS
//        this.prepareStartBuffer()
//    }
    
    private var robotMode: Boolean = true

    init {
//        soundSystem.audioContext.out.addInput(player)
        soundSystem.startRealtime()

        engine.tempos.addTempoChange(TempoChange(0f, 120f))
//        engine.tempos.addTempoChange(TempoChange(88f, 148.5f))

//        robotMode = false
//        
//        if (robotMode) {
//            engine.autoInputs = true
//        }
        
//        addEvents()
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
                val camera = this.renderer.camera
                val x = camera.position.x
                val y = camera.position.y
                val zoom = camera.zoom
                this.dispose()
                main.screen = TestWorldDunkScreen(main).apply { 
                    renderer.camera.position.x = x
                    renderer.camera.position.y = y
                    renderer.camera.zoom = zoom
                }
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            soundSystem.setPaused(!soundSystem.isPaused)
        }
        
        // Inputs
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            engine.postRunnable {
                engine.inputter.onDpadButtonPressed(false)
            }
        } else if (Gdx.input.isKeyJustReleased(Input.Keys.F)) {
            engine.postRunnable {
                engine.inputter.onDpadButtonPressed(true)
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            engine.postRunnable {
                engine.inputter.onAButtonPressed(false)
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            engine.postRunnable {
                engine.inputter.onDpadButtonPressed(true)
            }
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
        return """
---
${engine.getDebugString()}
---
${renderer.getDebugString()}
"""

    }

    override fun dispose() {
        soundSystem.disposeQuietly()
    }

    
}