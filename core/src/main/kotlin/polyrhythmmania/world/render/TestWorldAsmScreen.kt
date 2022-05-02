package polyrhythmmania.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import net.beadsproject.beads.ugens.SamplePlayer
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
import polyrhythmmania.gamemodes.*
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.sample.MusicSamplePlayer
import polyrhythmmania.world.*
import polyrhythmmania.world.tileset.StockTexturePack
import polyrhythmmania.world.tileset.StockTexturePacks
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TilesetPalette
import kotlin.system.measureNanoTime


class TestWorldAsmScreen(main: PRManiaGame) : PRManiaScreen(main) {

    companion object {
//        private val music: BeadsMusic = GdxAudioReader.newMusic(Gdx.files.internal("music/Polyrhythm.ogg"))
    }

    val world: World = World().apply { 
        this.worldMode = WorldMode(WorldType.ASSEMBLE, EndlessType.NOT_ENDLESS)
        resetWorld()
    }
    val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem()
    val timing: TimingProvider = SimpleTimingProvider({ throw it })
    val engine: Engine = Engine(timing, world, soundSystem, null)
    val renderer: WorldRenderer by lazy {
        WorldRenderer(world, Tileset(StockTexturePacks.gba).apply { 
            TilesetPalette.createAssembleTilesetPalette().applyTo(this)
        }, engine)
    }

    private val player: MusicSamplePlayer = SidemodeAssets.assembleTheme.createPlayer(soundSystem.audioContext).apply {
        this.gain = 1f
        this.prepareStartBuffer()
    }

    init {
        soundSystem.audioContext.out.addInput(player)
        soundSystem.startRealtime()
        
        engine.autoInputs = false
        engine.inputter.areInputsLocked = false

        addEvents()
    }
    
    private fun addEvents() {
        var tempoChangeBeat = 0f
        engine.tempos.addTempoChangesBulk("""98.002 24.0
104.001 12.0
108.011 4.0
111.993 12.0
122.075 2.0
128.068 2.0
131.965 12.0
120.0 2.0
106.007 2.0
97.999 12.0
86.022 2.0
70.012 2.0
60.0 12.0
96.0 2.0
115.942 2.0
132.013 12.0
140.023 2.0
150.0 2.0
160.0 20.0""".lines().map {
            val split = it.split(' ')
            val newTempo = split[0].toFloat()
            val dur = split[1].toFloat()
            val tc = TempoChange(tempoChangeBeat, newTempo)
            tempoChangeBeat += dur
            tc
        })

        
        fun patternA(startBeat: Float) {
            engine.addEvent(EventAsmSpawnWidgetHalves(engine, 0f, startBeat + 8f))
            engine.addEvent(EventAsmRodBounce(engine, startBeat + 0f, 999, 3, false))
            engine.addEvent(EventAsmRodBounce(engine, startBeat + 1f, 3, 2, false))
            engine.addEvent(EventAsmRodBounce(engine, startBeat + 2f, 2, 1, false))
            engine.addEvent(EventAsmRodBounce(engine, startBeat + 3f, 1, 0, false))
            engine.addEvent(EventAsmRodBounce(engine, startBeat + 4f, 0, 1, false))
            engine.addEvent(EventAsmRodBounce(engine, startBeat + 5f, 1, 2, false))
            engine.addEvent(EventAsmRodBounce(engine, startBeat + 6f, 2, 3, false))
            engine.addEvent(EventAsmRodBounce(engine, startBeat + 7f, 3, 2, true))

            engine.addEvent(EventAsmPistonSpringCharge(engine, world.asmPlayerPiston, startBeat + 7f))
            engine.addEvent(EventAsmPistonSpringUncharge(engine, world.asmPlayerPiston, startBeat + 8f))
            engine.addEvent(EventAsmPrepareSfx(engine, startBeat + 6f))
        }
        
        fun patternB(startBeat: Float) {
            engine.addEvent(EventAsmSpawnWidgetHalves(engine, 0f, startBeat + 5f))
            engine.addEvent(EventAsmRodBounce(engine, startBeat + 0f, -1, 0, false))
            engine.addEvent(EventAsmRodBounce(engine, startBeat + 1f, 0, 1, false))
            engine.addEvent(EventAsmRodBounce(engine, startBeat + 2f, 1, 2, false))
            engine.addEvent(EventAsmRodBounce(engine, startBeat + 3f, 2, 3, false))
            engine.addEvent(EventAsmRodBounce(engine, startBeat + 4f, 3, 2, true))

            engine.addEvent(EventAsmPistonSpringCharge(engine, world.asmPlayerPiston, startBeat + 4f))
            engine.addEvent(EventAsmPistonSpringUncharge(engine, world.asmPlayerPiston, startBeat + 5f))
            engine.addEvent(EventAsmPrepareSfx(engine, startBeat + 3f))
        }
        
        fun patternBoth(startBeat: Float) {
            patternA(startBeat)
            patternB(startBeat + 8f)
        }

        repeat(8) { 
            patternBoth(7f + it * 16f)
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val batch = main.batch

        renderer.render(batch)

        super.render(delta)
    }

    override fun renderUpdate() {
        super.renderUpdate()

        if (timing is SimpleTimingProvider && !soundSystem.isPaused) {
            timing.seconds += Gdx.graphics.deltaTime
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            timing.seconds += 1f / 60f
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
                main.screen = TestWorldAsmScreen(main).apply { 
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
        } else if (Gdx.input.isKeyJustReleased(Input.Keys.J)) {
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