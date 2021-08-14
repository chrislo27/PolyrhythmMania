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
import polyrhythmmania.sidemodes.*
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
        this.worldMode = WorldMode.ASSEMBLE
        resetWorld()
    }
    val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem()
    val timing: TimingProvider = SimpleTimingProvider({ throw it })
    val engine: Engine = Engine(timing, world, soundSystem, null)
    val renderer: WorldRenderer by lazy {
        WorldRenderer(world, Tileset(StockTexturePacks.gba).apply { 
            TilesetPalette.createAssembleTilesetPalette().applyTo(this)
        })
    }

    private val player: MusicSamplePlayer = SidemodeAssets.polyrhythmTheme.createPlayer(soundSystem.audioContext).apply {
        this.gain = 0.75f
//        this.loopStartMs = 3725f
        this.loopEndMs = 40928f //33482f
        this.loopType = SamplePlayer.LoopType.LOOP_FORWARDS
        this.prepareStartBuffer()
    }
    
    private var robotMode: Boolean = true

    init {
        soundSystem.audioContext.out.addInput(player)
        soundSystem.startRealtime()

        engine.tempos.addTempoChange(TempoChange(0f, 129f))
        
        engine.autoInputs = true
        
        addEvents()
    }
    
    private fun addEvents() {
        val playerPiston = world.asmPistons[2]
        engine.addEvent(LoopingEvent(engine, 10f, { true }) { engine, startBeat ->
            val rod = EntityRodAsm(world, 0f)
            world.addEntity(rod)
            engine.addEvent(EventAsmRodBounce(engine, rod, startBeat, 1f, 999, 3))
            engine.addEvent(EventAsmRodBounce(engine, rod, startBeat + 1, 1f, 3, 2))
            engine.addEvent(EventAsmRodBounce(engine, rod, startBeat + 2, 1f, 2, 1))
            engine.addEvent(EventAsmRodBounce(engine, rod, startBeat + 3, 1f, 1, 0))
            engine.addEvent(EventAsmRodBounce(engine, rod, startBeat + 4, 1f, 0, 1))
            engine.addEvent(EventAsmRodBounce(engine, rod, startBeat + 5, 1f, 1, 2))
            engine.addEvent(EventAsmRodBounce(engine, rod, startBeat + 6, 1f, 2, 3))
            engine.addEvent(EventAsmRodBounce(engine, rod, startBeat + 7, 1f, 3, 2, shouldAimInPit = true))
            
            engine.addEvent(EventAsmSpawnWidgetHalves(engine, startBeat, startBeat + 8))
            
            engine.addEvent(EventAsmPrepareSfx(engine, startBeat + 8 - 2f))
            engine.addEvent(EventAsmPistonRetractAll(engine, startBeat + 8))
            engine.addEvent(EventAsmPistonSpringCompress(engine, playerPiston, startBeat + 8 - 1f))
            engine.addEvent(EventAsmPistonSpringCompress(engine, playerPiston, startBeat + 8, fire = true))
            
            engine.addEvent(EventAsmAssemble(engine, startBeat + 8))
        }.also { e ->
            e.beat = 8f - 1
        })
        
//        val rod = EntityRodAsm(world, 0f)
//        world.addEntity(rod)
//        
//        world.addEntity(EntityAsmWidgetHalf(world, true, 9f, 0f))
//        world.addEntity(EntityAsmWidgetHalf(world, false, 9f, 0f))
//        
//        engine.addEvent(EventAsmRodBounce(engine, rod, 0f, 1f, -1, 0))
//        engine.addEvent(LoopingEvent(engine, 6f, { true }) { engine, startBeat ->
//            engine.addEvent(EventAsmRodBounce(engine, rod, startBeat, 1f, 0, 1))
//            engine.addEvent(EventAsmRodBounce(engine, rod, startBeat + 1, 1f, 1, 2))
//            engine.addEvent(EventAsmRodBounce(engine, rod, startBeat + 2, 1f, 2, 3))
//            engine.addEvent(EventAsmRodBounce(engine, rod, startBeat + 3, 1f, 3, 2))
//            engine.addEvent(EventAsmRodBounce(engine, rod, startBeat + 4, 1f, 2, 1))
//            engine.addEvent(EventAsmRodBounce(engine, rod, startBeat + 5, 1f, 1, 0))
//            
//            engine.addEvent(EventAsmPistonRetractAll(engine, startBeat))
//            engine.addEvent(EventAsmPistonRetractAll(engine, startBeat + 1))
//            engine.addEvent(EventAsmPistonRetractAll(engine, startBeat + 2))
//            engine.addEvent(EventAsmPistonRetractAll(engine, startBeat + 3))
//            engine.addEvent(EventAsmPistonRetractAll(engine, startBeat + 4))
//            engine.addEvent(EventAsmPistonRetractAll(engine, startBeat + 5))
//            
//            engine.addEvent(EventAsmPistonExtend(engine, world.asmPistons[0], startBeat))
//            engine.addEvent(EventAsmPistonExtend(engine, world.asmPistons[1], startBeat + 1))
//            engine.addEvent(EventAsmPistonExtend(engine, world.asmPistons[2], startBeat + 2))
//            engine.addEvent(EventAsmPistonExtend(engine, world.asmPistons[3], startBeat + 3))
//            engine.addEvent(EventAsmPistonExtend(engine, world.asmPistons[2], startBeat + 4))
//            engine.addEvent(EventAsmPistonExtend(engine, world.asmPistons[1], startBeat + 5))
//        }.also { e -> 
//            e.beat = 1f
//        })
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