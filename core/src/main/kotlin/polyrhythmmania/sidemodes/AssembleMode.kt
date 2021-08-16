package polyrhythmmania.sidemodes

import com.badlogic.gdx.math.MathUtils
import net.beadsproject.beads.ugens.SamplePlayer
import paintbox.registry.AssetRegistry
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.Block
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.world.*
import polyrhythmmania.world.tileset.StockTexturePacks
import polyrhythmmania.world.tileset.TilesetPalette
import kotlin.math.sign


class AssembleMode(main: PRManiaGame, prevHighScore: EndlessModeScore)
    : AbstractEndlessMode(main, prevHighScore) {

    init {
        container.world.worldMode = WorldMode.ASSEMBLE
        container.engine.inputter.endlessScore.maxLives.set(3)
        container.renderer.worldBackground = AssembleWorldBackground
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
        
        SidemodeAssets.assembleSfx // Call get to load

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

// EVENTS ------------------------------------------------------------------------------------------------------

abstract class AbstractEventAsmRod(engine: Engine, startBeat: Float) : Event(engine) {
    
    init {
        this.beat = startBeat
    }
    
    abstract fun onStartRod(currentBeat: Float, rod: EntityRodAsm)

    override fun onStart(currentBeat: Float) {
        engine.world.entities.forEach { e ->
            if (e is EntityRodAsm && e.acceptingInputs) {
                onStartRod(currentBeat, e)
            }
        }
    }
}

/**
 * The core event of Assemble mode.
 * 
 * If the [fromIndex] is out of bounds, a new rod is spawned and the bounce is applied only to it.
 * If the [fromIndex] is in bounds and is NOT the player index, then the piston extend animation is also played.
 * 
 * If the [toIndex] is the player index, an expected input is scheduled for the next beat.
 * If the [fromIndex] is the player index, then a conditional bounce is scheduled,
 * requiring the previous input to be hit for the bounce to succeed.
 */
class EventAsmRodBounce(engine: Engine, startBeat: Float,
                        val fromIndex: Int, val toIndex: Int, val nextInputIsFire: Boolean = false)
    : AbstractEventAsmRod(engine, startBeat) {

    override fun onStartRod(currentBeat: Float, rod: EntityRodAsm) {
        bounceRod(rod)
    }

    override fun onStart(currentBeat: Float) {
        val world = engine.world
        if (fromIndex !in 0 until world.asmPistons.size) {
            // Out of bounds. Spawn a new rod
            val newRod = EntityRodAsm(world, this.beat)
            world.addEntity(newRod)
            bounceRod(newRod)
        } else {
            super.onStart(currentBeat)
        }
    }
    
    private fun bounceRod(rod: EntityRodAsm) {
        val world = engine.world
        val pistons = world.asmPistons
        val playerIndex = pistons.indexOf(world.asmPlayerPiston)

        val fromPos = rod.getPistonPosition(engine, fromIndex)
        val toPos = rod.getPistonPosition(engine, toIndex)
        var bounce = EntityRodAsm.BounceAsm(this.beat, 1f, toPos.y + 1f + (if (nextInputIsFire) 4.5f else 3.5f),
                fromPos.x, fromPos.y + 1f, toPos.x, toPos.y + 1f, rod.bounce)
        
        if (nextInputIsFire) {
            bounce = EntityRodAsm.BounceAsm(this.beat + bounce.duration, 1f, bounce.endY - 5f,
                    bounce.endX, bounce.endY,
                    bounce.endX + (bounce.endX - bounce.startX).sign * 3f, bounce.endY - 11f, bounce)
        }
        
        if (fromIndex == playerIndex && !engine.autoInputs) {
            // Have a conditional bounce. Bounce will only happen if the PREVIOUSLY hit input was at the same time and was successful
            rod.expectedInputs.lastOrNull()?.addConditionalBounce(rod, bounce)
        } else {
            if (toIndex == playerIndex) {
                // Schedule an expected input
                val inputBeat = this.beat + 1
                rod.addExpectedInput(EntityRodAsm.NextExpected(inputBeat, nextInputIsFire))
                if (nextInputIsFire) {
                    engine.soundInterface.playAudioNoOverlap(SidemodeAssets.assembleSfx.getValue("sfx_asm_compress"))
                }
            }
            
            // Bounce the rod
            rod.bounce = bounce
            
            if (fromIndex in 0 until world.asmPistons.size) {
                // Play piston extend animation
                world.asmPistons[fromIndex].fullyExtend(engine, this.beat, 1f)
                engine.soundInterface.playAudioNoOverlap(SidemodeAssets.assembleSfx.getValue(when (fromIndex) {
                    0 -> "sfx_asm_left"
                    1 -> "sfx_asm_middle_left"
                    2 -> "sfx_asm_middle_right"
                    3 -> "sfx_asm_right"
                    else -> "sfx_asm_left"
                }))
            }
        }
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

class EventAsmPistonSpringCharge(engine: Engine, val piston: EntityPistonAsm, startBeat: Float)
    : Event(engine) {

    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        if (engine.world.entities.any { e -> e is EntityRodAsm && e.acceptingInputs }) {
            piston.chargeUp(currentBeat)
            piston.retract()
        }
    }
}

class EventAsmPistonSpringUncharge(engine: Engine, val piston: EntityPistonAsm, startBeat: Float)
    : Event(engine) {

    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        if (piston.animation is EntityPistonAsm.Animation.Charged) {
            piston.uncharge(currentBeat)
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

class EventAsmPrepareSfx(engine: Engine, startBeat: Float) : Event(engine) {
    
    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        val beadsSound = SidemodeAssets.assembleSfx.getValue("sfx_asm_prepare")
        engine.soundInterface.playAudio(beadsSound) { player ->
            player.pitch = engine.tempos.tempoAtBeat(currentBeat) / 98f
        }
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
            it is EntityAsmWidgetHalf && MathUtils.isEqual(this.combineBeat, it.combineBeat, 0.01f)
        }.forEach {
            it.kill()
            any = true
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
