package polyrhythmmania.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import paintbox.util.gdxutils.disposeQuietly
import net.beadsproject.beads.ugens.SamplePlayer
import paintbox.util.gdxutils.isKeyJustReleased
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.input.EventLockInputs
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.sample.GdxAudioReader
import polyrhythmmania.soundsystem.sample.MusicSamplePlayer
import paintbox.util.DecimalFormats
import polyrhythmmania.world.*
import polyrhythmmania.world.entity.EntityPiston
import polyrhythmmania.world.tileset.StockTexturePacks
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TilesetPalette
import kotlin.system.measureNanoTime


class TestWorldRenderScreen(main: PRManiaGame) : PRManiaScreen(main) {

    companion object {
        private val music: BeadsMusic = GdxAudioReader.newMusic(Gdx.files.internal("music/Polyrhythm.ogg"))
    }

    val world: World = World()
    val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem()
    val timing: TimingProvider = soundSystem
    val engine: Engine = Engine(timing, world, soundSystem, null)
    val renderer: WorldRenderer by lazy {
        WorldRenderer(world, Tileset(StockTexturePacks.gba).apply { 
            TilesetPalette.createGBA1TilesetPalette().applyTo(this)
        }, engine)
    }

    private val player: MusicSamplePlayer = music.createPlayer(soundSystem.audioContext).apply {
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
//        engine.tempos.addTempoChange(TempoChange(88f, 148.5f))

        robotMode = false
        
        if (robotMode) {
            engine.autoInputs = true
        }
        
        addEvents()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val batch = main.batch

        renderer.render(batch)

        super.render(delta)
    }

    private var nanoTime = System.nanoTime()

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
                main.screen = TestWorldRenderScreen(main).apply { 
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
        return """player pos: ${DecimalFormats.format("0.000", player.position / 1000f)}
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
        val events: MutableList<Event> = when (1) {
            0 -> addPr1Patterns()
            1 -> {
                val speed = 1f
                engine.tempos.addTempoChange(TempoChange(0f, 148.5f * speed))
                player.pitch = 148.5f / 129f * speed
                addPr2Patterns()
            }
            2 -> addInputTestPatterns()
            3 -> addTestPatterns()
            4 -> addRemixTestPatterns()
            else -> addTestPatterns()
        }.toMutableList()

        if (robotMode) {
            events.removeIf { e ->
                e is EventLockInputs
            }
        } else {
            events.removeIf { e ->
                e is EventRowBlockExtend
            }
        }
        
        engine.addEvents(events)
    }

    private fun addTestPatterns(): List<Event> {
        val events = mutableListOf<Event>()
        events += EventRowBlockSpawn(engine, world.rowA, 0, EntityPiston.Type.PISTON_A, 8f)
        events += EventRowBlockSpawn(engine, world.rowA, 4, EntityPiston.Type.PISTON_A, 10f)
        events += EventRowBlockSpawn(engine, world.rowA, 8, EntityPiston.Type.PLATFORM, 12f, true)

        // Explode test
        events += EventRowBlockSpawn(engine, world.rowDpad, 0, EntityPiston.Type.PISTON_DPAD, 5f)
        events += EventRowBlockSpawn(engine, world.rowDpad, 1, EntityPiston.Type.PLATFORM, 5.5f)
        events += EventRowBlockSpawn(engine, world.rowDpad, 2, EntityPiston.Type.PISTON_DPAD, 6f)
        events += EventRowBlockSpawn(engine, world.rowDpad, 7, EntityPiston.Type.PLATFORM, 7f)
        events += EventRowBlockExtend(engine, world.rowDpad, 2, 11f)
        events += EventRowBlockExtend(engine, world.rowDpad, 0, 13f)
        events += EventDeployRod(engine, world.rowDpad, 5f)
        events += EventDeployRod(engine, world.rowDpad, 8f)
        events += EventDeployRod(engine, world.rowDpad, 12f)
        events += EventRowBlockDespawn(engine, world.rowDpad, -1, 23f)

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
        events += EventRowBlockSpawn(engine, world.rowA, 0, EntityPiston.Type.PISTON_A, 24f)
        events += EventRowBlockSpawn(engine, world.rowA, 2, EntityPiston.Type.PISTON_A, 25f)
        events += EventRowBlockSpawn(engine, world.rowA, 4, EntityPiston.Type.PISTON_A, 26f)
        events += EventRowBlockSpawn(engine, world.rowA, 6, EntityPiston.Type.PISTON_A, 27f)
        events += EventRowBlockSpawn(engine, world.rowA, 8, EntityPiston.Type.PLATFORM, 28f, true)
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
        events += EventRowBlockSpawn(engine, world.rowA, 0, EntityPiston.Type.PISTON_A, 40f)
        events += EventRowBlockSpawn(engine, world.rowA, 2, EntityPiston.Type.PISTON_A, 41f)
        events += EventRowBlockSpawn(engine, world.rowDpad, 0, EntityPiston.Type.PLATFORM, 41f)
        events += EventRowBlockSpawn(engine, world.rowDpad, 1, EntityPiston.Type.PLATFORM, 41f)
        events += EventRowBlockSpawn(engine, world.rowDpad, 2, EntityPiston.Type.PISTON_DPAD, 41f)
        events += EventRowBlockSpawn(engine, world.rowA, 4, EntityPiston.Type.PISTON_A, 42f)
        events += EventRowBlockSpawn(engine, world.rowA, 6, EntityPiston.Type.PISTON_A, 43f)
        events += EventRowBlockSpawn(engine, world.rowDpad, 6, EntityPiston.Type.PISTON_DPAD, 43f)
        events += EventRowBlockSpawn(engine, world.rowA, 8, EntityPiston.Type.PLATFORM, 44f, true)
        events += EventRowBlockSpawn(engine, world.rowDpad, 10, EntityPiston.Type.PLATFORM, 44f, true)
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

        return events
    }

    data class Spawn(val index: Int, val type: EntityPiston.Type, val beat: Float, val forward: Boolean = false)

    fun addPattern(events: MutableList<Event>, startBeat: Float, rowA: List<Spawn>, rowD: List<Spawn>, repeat: Boolean = true) {
        fun doIt(row: Row, list: List<Spawn>) {
            list.forEach { s ->
                events += EventRowBlockSpawn(engine, row, s.index, s.type, startBeat + s.beat, s.forward)
                events += EventRowBlockExtend(engine, row, s.index, startBeat + (s.index * 0.5f) + 4f, false)
                if (repeat) {
                    events += EventRowBlockExtend(engine, row, s.index, startBeat + (s.index * 0.5f) + 8f, false)
                }
            }
            
            if (repeat) {
                events += EventLockInputs(engine, false, startBeat + 2f)
                events += EventDeployRod(engine, row, startBeat)
                events += EventDeployRod(engine, row, startBeat + 4)
                if (list.any { it.index == 7 && it.type != EntityPiston.Type.PLATFORM }) {
                    events += EventRowBlockRetract(engine, row, -1, startBeat + 7.5f + 0.5f)
                } else {
                    events += EventRowBlockRetract(engine, row, -1, startBeat + 7.5f)
                }
                events += EventLockInputs(engine, true, startBeat + 13.75f)
                events += EventRowBlockRetract(engine, row, -1, startBeat + 14f)
                events += EventRowBlockDespawn(engine, row, -1, startBeat + 15f)
            } else {
                events += EventLockInputs(engine, false, startBeat + 2f)
                events += EventDeployRod(engine, row, startBeat)
                if (list.any { it.index == 7 && it.type != EntityPiston.Type.PLATFORM }) {
                    events += EventRowBlockRetract(engine, row, -1, startBeat + 7.5f + 0.5f)
                } else {
                    events += EventRowBlockRetract(engine, row, -1, startBeat + 7.5f)
                }
                events += EventLockInputs(engine, true, startBeat + 7.25f)
                events += EventRowBlockDespawn(engine, row, -1, startBeat + 8f)
            }
        }

        if (rowA.isNotEmpty()) {
            doIt(world.rowA, rowA)
        }
        if (rowD.isNotEmpty()) {
            doIt(world.rowDpad, rowD)
        }
    }

    private fun addPr1Patterns(): List<Event> {
        val events = mutableListOf<Event>()

        addPattern(events, 0 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), emptyList())
        addPattern(events, 1 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true)
        ), emptyList())

        addPattern(events, 2 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), emptyList())
        addPattern(events, 3 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), emptyList())

        addPattern(events, 4 * 16 + 8f, emptyList(), listOf(
                Spawn(0, EntityPiston.Type.PISTON_DPAD, 0f),
                Spawn(2, EntityPiston.Type.PISTON_DPAD, 1f),
                Spawn(4, EntityPiston.Type.PISTON_DPAD, 2f),
                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ))
        addPattern(events, 5 * 16 + 8f, emptyList(), listOf(
                Spawn(0, EntityPiston.Type.PISTON_DPAD, 0f),
                Spawn(2, EntityPiston.Type.PISTON_DPAD, 1f),
                Spawn(4, EntityPiston.Type.PISTON_DPAD, 2f),
                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ))

        addPattern(events, 6 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), listOf(
                Spawn(0, EntityPiston.Type.PLATFORM, 1f),
                Spawn(1, EntityPiston.Type.PLATFORM, 1f),
                Spawn(2, EntityPiston.Type.PISTON_DPAD, 1f),
                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
                Spawn(10, EntityPiston.Type.PLATFORM, 5f, true),
        ))
        addPattern(events, 7 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), listOf(
                Spawn(0, EntityPiston.Type.PLATFORM, 1f),
                Spawn(1, EntityPiston.Type.PLATFORM, 1f),
                Spawn(2, EntityPiston.Type.PISTON_DPAD, 1f),
                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
                Spawn(10, EntityPiston.Type.PLATFORM, 5f, true),
        ))

        addPattern(events, 8 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), listOf(
                Spawn(0, EntityPiston.Type.PISTON_DPAD, 0f),
                Spawn(2, EntityPiston.Type.PISTON_DPAD, 1f),
                Spawn(4, EntityPiston.Type.PISTON_DPAD, 2f),
                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ))
        addPattern(events, 9 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), listOf(
                Spawn(0, EntityPiston.Type.PISTON_DPAD, 0f),
                Spawn(2, EntityPiston.Type.PISTON_DPAD, 1f),
                Spawn(4, EntityPiston.Type.PISTON_DPAD, 2f),
                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ))

        addPattern(events, 10 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), listOf(
                Spawn(0, EntityPiston.Type.PLATFORM, 1f),
                Spawn(1, EntityPiston.Type.PLATFORM, 1f),
                Spawn(2, EntityPiston.Type.PISTON_DPAD, 1f),
                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
                Spawn(10, EntityPiston.Type.PLATFORM, 5f, true),
        ))
        addPattern(events, 11 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), listOf(
                Spawn(0, EntityPiston.Type.PLATFORM, 1f),
                Spawn(1, EntityPiston.Type.PLATFORM, 1f),
                Spawn(2, EntityPiston.Type.PISTON_DPAD, 1f),
                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
                Spawn(10, EntityPiston.Type.PLATFORM, 5f, true),
        ))

        return events
    }



    private fun addPr2Patterns(): List<Event> {
        val events = mutableListOf<Event>()

        addPattern(events, 0 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), emptyList())
        addPattern(events, 1 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PLATFORM, 0f + 0.5f),
                Spawn(0 + 1, EntityPiston.Type.PISTON_A, 0f + 0.5f),
                Spawn(2 + 1, EntityPiston.Type.PISTON_A, 1f + 0.5f),
                Spawn(4 + 1, EntityPiston.Type.PISTON_A, 2f + 0.5f),
                Spawn(6 + 1, EntityPiston.Type.PISTON_A, 3f + 0.5f),
                Spawn(8 + 1, EntityPiston.Type.PLATFORM, 4f + 0.5f, true),
        ), emptyList())

        addPattern(events, 2 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), emptyList())
        addPattern(events, 3 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PLATFORM, 0f + 0.5f),
                Spawn(0 + 1, EntityPiston.Type.PISTON_A, 0f + 0.5f),
                Spawn(2 + 1, EntityPiston.Type.PISTON_A, 1f + 0.5f),
                Spawn(4 + 1, EntityPiston.Type.PISTON_A, 2f + 0.5f),
                Spawn(6 + 1, EntityPiston.Type.PISTON_A, 3f + 0.5f),
                Spawn(8 + 1, EntityPiston.Type.PLATFORM, 4f + 0.5f, true),
        ), emptyList())

        addPattern(events, 4 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), listOf(
                Spawn(0, EntityPiston.Type.PLATFORM, 3f),
                Spawn(1, EntityPiston.Type.PLATFORM, 3f),
                Spawn(2, EntityPiston.Type.PLATFORM, 3f),
                Spawn(3, EntityPiston.Type.PLATFORM, 3f),
                Spawn(4, EntityPiston.Type.PLATFORM, 3f),
                Spawn(5, EntityPiston.Type.PLATFORM, 3f),
                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ))
        addPattern(events, 5 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), listOf(
                Spawn(0, EntityPiston.Type.PLATFORM, 3f),
                Spawn(1, EntityPiston.Type.PLATFORM, 3f),
                Spawn(2, EntityPiston.Type.PLATFORM, 3f),
                Spawn(3, EntityPiston.Type.PLATFORM, 3f),
                Spawn(4, EntityPiston.Type.PLATFORM, 3f),
                Spawn(5, EntityPiston.Type.PLATFORM, 3f),
                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ))

        addPattern(events, 6 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), listOf(
                Spawn(0, EntityPiston.Type.PLATFORM, 1.5f),
                Spawn(1, EntityPiston.Type.PLATFORM, 1.5f),
                Spawn(2, EntityPiston.Type.PLATFORM, 1.5f),
                Spawn(3, EntityPiston.Type.PISTON_DPAD, 1.5f),
                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
                Spawn(9, EntityPiston.Type.PLATFORM, 4.5f, true),
        ))
        addPattern(events, 7 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), listOf(
                Spawn(0, EntityPiston.Type.PLATFORM, 1.5f),
                Spawn(1, EntityPiston.Type.PLATFORM, 1.5f),
                Spawn(2, EntityPiston.Type.PLATFORM, 1.5f),
                Spawn(3, EntityPiston.Type.PISTON_DPAD, 1.5f),
                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
                Spawn(9, EntityPiston.Type.PLATFORM, 4.5f, true),
        ))
        
        addPattern(events, 8 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), listOf(
                Spawn(0, EntityPiston.Type.PISTON_DPAD, 0f),
                Spawn(3, EntityPiston.Type.PISTON_DPAD, 1.5f),
                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
                Spawn(9, EntityPiston.Type.PLATFORM, 4.5f, true),
        ))
        addPattern(events, 9 * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), listOf(
                Spawn(0, EntityPiston.Type.PISTON_DPAD, 0f),
                Spawn(3, EntityPiston.Type.PISTON_DPAD, 1.5f),
                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
                Spawn(9, EntityPiston.Type.PLATFORM, 4.5f, true),
        ))
        

        return events
    }


    private fun addRemixTestPatterns(): List<Event> {
        val events = mutableListOf<Event>()

//        list.forEach { s ->
//            events += EventRowBlockSpawn(engine, row, s.index, s.type, startBeat + s.beat, s.forward)
//            events += EventRowBlockExtend(engine, row, s.index, startBeat + (s.index * 0.5f) + 4f, false)
//            if (repeat) {
//                events += EventRowBlockExtend(engine, row, s.index, startBeat + (s.index * 0.5f) + 8f, false)
//            }
//        }
//
//        if (repeat) {
//            events += EventLockInputs(engine, false, startBeat + 2f)
//            events += EventDeployRod(engine, row, startBeat)
//            events += EventDeployRod(engine, row, startBeat + 4)
//            events += EventRowBlockRetract(engine, row, -1, startBeat + 7.5f)
//            events += EventLockInputs(engine, true, startBeat + 13.75f)
//            events += EventRowBlockRetract(engine, row, -1, startBeat + 14f)
//            events += EventRowBlockDespawn(engine, row, -1, startBeat + 15f)
//        } else {
//            events += EventLockInputs(engine, false, startBeat + 2f)
//            events += EventDeployRod(engine, row, startBeat)
//            events += EventRowBlockRetract(engine, row, -1, startBeat + 7.5f)
//            events += EventLockInputs(engine, true, startBeat + 7.25f)
//            events += EventRowBlockDespawn(engine, row, -1, startBeat + 8f)
//        }
        
        events += EventRowBlockSpawn(engine, world.rowA, 0, EntityPiston.Type.PISTON_A, (0 * 8f + 8f) + 0f, false)
        events += EventRowBlockSpawn(engine, world.rowA, 2, EntityPiston.Type.PISTON_A, (0 * 8f + 8f) + 1f, false)
        events += EventRowBlockSpawn(engine, world.rowA, 4, EntityPiston.Type.PISTON_A, (0 * 8f + 8f) + 2f, false)
        events += EventRowBlockSpawn(engine, world.rowA, 6, EntityPiston.Type.PISTON_A, (0 * 8f + 8f) + 3f, false)
        events += EventRowBlockSpawn(engine, world.rowA, 8, EntityPiston.Type.PLATFORM, (0 * 8f + 8f) + 3f, true)
        events += EventDeployRod(engine, world.rowA, (0 * 8f + 8f))
        events += EventLockInputs(engine, false, (0 * 8f + 8f) + 2f)
        events += EventLockInputs(engine, false, (0 * 8f + 8f) + 7.25f)
        events += EventRowBlockRetract(engine, world.rowA, -1, (0 * 8f + 8f) + 7.5f)
        events += EventRowBlockDespawn(engine, world.rowA, -1, (0 * 8f + 8f) + 7.5f)



        events += EventRowBlockSpawn(engine, world.rowA, 0, EntityPiston.Type.PISTON_A, (1 * 8f + 8f) + 0f, false)
        events += EventRowBlockSpawn(engine, world.rowA, 4, EntityPiston.Type.PISTON_A, (1 * 8f + 8f) + 2f, false)
        events += EventRowBlockSpawn(engine, world.rowA, 8, EntityPiston.Type.PLATFORM, (1 * 8f + 8f) + 2f, true)
        events += EventRowBlockSpawn(engine, world.rowDpad, 0, EntityPiston.Type.PLATFORM, (1 * 8f + 8f) + 1f, false)
        events += EventRowBlockSpawn(engine, world.rowDpad, 1, EntityPiston.Type.PLATFORM, (1 * 8f + 8f) + 1f, false)
        events += EventRowBlockSpawn(engine, world.rowDpad, 2, EntityPiston.Type.PISTON_DPAD, (1 * 8f + 8f) + 1f, false)
        events += EventRowBlockSpawn(engine, world.rowDpad, 6, EntityPiston.Type.PISTON_DPAD, (1 * 8f + 8f) + 3f, false)
        events += EventRowBlockSpawn(engine, world.rowDpad, 10, EntityPiston.Type.PLATFORM, (1 * 8f + 8f) + 3f, true)
        events += EventDeployRod(engine, world.rowA, (1 * 8f + 8f))
        events += EventDeployRod(engine, world.rowDpad, (1 * 8f + 8f))
        events += EventLockInputs(engine, false, (1 * 8f + 8f) + 2f)
        events += EventLockInputs(engine, false, (1 * 8f + 8f) + 7.25f)
        events += EventRowBlockRetract(engine, world.rowA, -1, (1 * 8f + 8f) + 7.5f)
        events += EventRowBlockDespawn(engine, world.rowA, -1, (1 * 8f + 8f) + 7.5f)
        events += EventRowBlockRetract(engine, world.rowDpad, -1, (1 * 8f + 8f) + 7.5f)
        events += EventRowBlockDespawn(engine, world.rowDpad, -1, (1 * 8f + 8f) + 7.5f)

//        addPattern(events, 0 * 8 + 8f, listOf(
//                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
//                Spawn(2, EntityPiston.Type.PISTON_A, 1f),
//                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
//                Spawn(6, EntityPiston.Type.PISTON_A, 3f),
//                Spawn(8, EntityPiston.Type.PLATFORM, 3f, true),
//        ), emptyList(), repeat = false)
//        addPattern(events, 1 * 8 + 8f, listOf(
//                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
//                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
//                Spawn(8, EntityPiston.Type.PLATFORM, 2f, true),
//        ), listOf(
//                Spawn(0, EntityPiston.Type.PLATFORM, 1f),
//                Spawn(1, EntityPiston.Type.PLATFORM, 1f),
//                Spawn(2, EntityPiston.Type.PISTON_DPAD, 1f),
//                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
//                Spawn(10, EntityPiston.Type.PLATFORM, 3f, true),
//        ), repeat = false)


        return events
    }

    private fun addInputTestPatterns(): List<Event> {
        val events = mutableListOf<Event>()

        var patternIndex = 0
//        addPattern(events, patternIndex++ * 16 + 8f, listOf(
//                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
//                Spawn(4, EntityPiston.Type.PISTON_A, 2f),
//                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
//        ), listOf(
//                Spawn(0, EntityPiston.Type.PLATFORM, 1f),
//                Spawn(1, EntityPiston.Type.PLATFORM, 1f),
//                Spawn(2, EntityPiston.Type.PISTON_DPAD, 1f),
//                Spawn(6, EntityPiston.Type.PISTON_DPAD, 3f),
//                Spawn(10, EntityPiston.Type.PLATFORM, 5f, true),
//        ))
        addPattern(events, patternIndex++ * 16 + 8f, listOf(
                Spawn(0, EntityPiston.Type.PISTON_A, 0f),
                Spawn(1, EntityPiston.Type.PISTON_A, 1 * 0.5f),
                Spawn(2, EntityPiston.Type.PISTON_A, 2 * 0.5f),
                Spawn(3, EntityPiston.Type.PISTON_A, 3 * 0.5f),
                Spawn(4, EntityPiston.Type.PISTON_A, 4 * 0.5f),
                Spawn(5, EntityPiston.Type.PISTON_A, 5 * 0.5f),
                Spawn(6, EntityPiston.Type.PISTON_A, 6 * 0.5f),
                Spawn(7, EntityPiston.Type.PISTON_A, 7 * 0.5f),
                Spawn(8, EntityPiston.Type.PLATFORM, 4f, true),
        ), emptyList())
        
        return events
    }
}