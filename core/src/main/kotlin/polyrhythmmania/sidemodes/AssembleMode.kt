package polyrhythmmania.sidemodes

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import net.beadsproject.beads.ugens.SamplePlayer
import paintbox.registry.AssetRegistry
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.Block
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.EventPlaySFX
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.world.*
import polyrhythmmania.world.entity.EntityExplosion
import polyrhythmmania.world.entity.EntityRod
import polyrhythmmania.world.tileset.StockTexturePacks
import polyrhythmmania.world.tileset.TilesetPalette
import kotlin.math.sign


class AssembleMode(main: PRManiaGame, prevHighScore: EndlessModeScore)
    : AbstractEndlessMode(main, prevHighScore) {

    init {
        container.world.worldMode = WorldMode.ASSEMBLE
        container.engine.inputter.endlessScore.maxLives.set(3)
        container.renderer.worldBackground = DunkWorldBackground
        container.renderer.tileset.texturePack.set(StockTexturePacks.gba)
        TilesetPalette.createAssembleTilesetPalette().applyTo(container.renderer.tileset)
        container.world.tilesetPalette.copyFrom(container.renderer.tileset)
    }

    override fun initialize() {
        engine.tempos.addTempoChange(TempoChange(0f, 129f))

        val music: BeadsMusic = SidemodeAssets.practiceTheme
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 0f
        musicData.loopParams = LoopParams(SamplePlayer.LoopType.LOOP_FORWARDS, 0.0, music.musicSample.lengthMs)
        musicData.beadsMusic = music
        musicData.update()

        addInitialBlocks()
    }

    private fun addInitialBlocks() {
        val blocks = mutableListOf<Block>()
        blocks += ResetMusicVolumeBlock(engine).apply {
            this.beat = 0f
        }
        

        container.addBlocks(blocks)
    }
}

class EventAsmPistonExtend(engine: Engine, val piston: EntityPistonAsm, startBeat: Float) : Event(engine) {
    
    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        piston.fullyExtend(engine, currentBeat)
        engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_spawn_d")) { 
            it.pitch = 1.25f
        }
    }
}

class EventAsmPistonRetract(engine: Engine, val piston: EntityPistonAsm, startBeat: Float) : Event(engine) {
    
    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        piston.retract()
    }
}

class EventAsmPistonRetractAll(engine: Engine, startBeat: Float) : Event(engine) {
    
    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        engine.world.asmPistons.forEach { piston ->
            piston.retract()
        }
    }
}

class EventAsmPistonSpringCompress(engine: Engine, val piston: EntityPistonAsm, startBeat: Float,
                                   val fire: Boolean = false)
    : Event(engine) {

    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        piston.spring(this.beat, !fire)
    }
}


class EventAsmAssemble(engine: Engine, val combineBeat: Float)
    : Event(engine) {

    init {
        this.beat = combineBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        
        val world = engine.world
        var any = false
        world.entities.filter { 
            it is EntityAsmWidgetHalf && MathUtils.isEqual(this.combineBeat, it.combineBeat, 0.001f)
        }.forEach { 
            it.kill()
            any = true
        }
        world.entities.filter {
            it is EntityRodAsm && MathUtils.isEqual(this.combineBeat, it.combineBeat, 0.001f)
        }.forEach {
            it.kill()
        }
        
        if (any) {
            // Spawn full widget.
            val complete = EntityAsmWidgetComplete(world, combineBeat)
            world.addEntity(complete)
            world.addEntity(EntityAsmWidgetCompleteBlur(world, combineBeat).also { 
                it.position.set(complete.position)
            })
        }
    }
}

class EventAsmPrepareSfx(engine: Engine, startBeat: Float) : Event(engine) {
    
    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        val beadsSound = AssetRegistry.get<BeadsSound>("sfx_asm_prepare")
        engine.soundInterface.playAudio(beadsSound) { player ->
            player.pitch = engine.tempos.tempoAtBeat(currentBeat) / 98f
        }
    }
}


class EventAsmRodBounce(engine: Engine, val rod: EntityRodAsm, startBeat: Float, val duration: Float, 
                        val fromIndex: Int, val toIndex: Int, 
                        val shouldAimInPit: Boolean = false,
                        val shouldRetractAllFirst: Boolean = true, val extendPistonBelow: Boolean = true)
    : Event(engine) {

    init {
        this.beat = startBeat
    }
    
    fun getPistonPosition(index: Int): Vector3 {
        val vec = Vector3(0f, 0f, 0f)
        val pistons = engine.world.asmPistons
        
        if (index < 0) {
            vec.set(pistons[0].position)
            vec.x -= 4f
        } else if (index >= pistons.size) {
            vec.set(pistons[pistons.size - 1].position)
            vec.x += 4f
        } else {
            vec.set(pistons[index].position)
        }
        
        return vec
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)

        if (shouldRetractAllFirst) {
            engine.world.asmPistons.forEach { piston ->
                piston.retract()
            }
        }
        
        val previousBounce = rod.bounce
        val fromPos = getPistonPosition(fromIndex)
        val toPos = getPistonPosition(toIndex)
        val mainBounce = EntityRodAsm.BounceAsm(this.beat, duration, toPos.y + 1f + 3.5f,
                fromPos.x, fromPos.y + 1f, toPos.x, toPos.y + 1f,
                previousBounce)
        if (shouldAimInPit) {
            rod.bounce = EntityRodAsm.BounceAsm(this.beat + duration, duration, mainBounce.endY - 7f,
                    mainBounce.endX, mainBounce.endY,
                    mainBounce.endX - (mainBounce.endX - mainBounce.startBeat).sign * 1f, mainBounce.endY - 10f,
                    mainBounce)
            rod.killAtBeat = this.beat + duration * 2
            rod.combineBeat = this.beat + duration
        } else {
            rod.bounce = mainBounce
        }
        
        if (fromIndex in 0 until engine.world.asmPistons.size) {
            if (extendPistonBelow) {
                engine.world.asmPistons[fromIndex].fullyExtend(engine, currentBeat)
                engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_spawn_d")) {
                    it.pitch = if (fromIndex == 2) 1.35f else 1.25f
                }
            }
            
            engine.world.addEntity(EntityExplosion(engine.world, engine.seconds, rod.renderWidth).apply {
                this.duration = 4f / 60f
                this.position.set(fromPos)
                this.position.y += 1f
                this.position.x += 0.75f
                this.position.z -= 0.25f
            })
        }
    }
}


class EventAsmSpawnWidgetHalves(engine: Engine, startBeat: Float, val combineBeat: Float, val beatsPerUnit: Float = 1f)
    : Event(engine) {

    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)

        val world = engine.world
        world.addEntity(EntityAsmWidgetHalf(world, true, combineBeat, 0f, beatsPerUnit))
        world.addEntity(EntityAsmWidgetHalf(world, false, combineBeat, 0f, beatsPerUnit))
    }
}
