package polyrhythmmania.sidemodes

import com.badlogic.gdx.math.MathUtils
import net.beadsproject.beads.ugens.SamplePlayer
import paintbox.registry.AssetRegistry
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.GlobalContainerSettings
import polyrhythmmania.container.TexturePackSource
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockEndState
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.engine.*
import polyrhythmmania.engine.input.ResultsText
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.sidemodes.endlessmode.EndlessPolyrhythm
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.statistics.PlayTimeType
import polyrhythmmania.util.Semitones
import polyrhythmmania.world.*
import polyrhythmmania.world.render.ForceTexturePack
import polyrhythmmania.world.render.ForceTilesetPalette
import polyrhythmmania.world.tileset.StockTexturePacks
import polyrhythmmania.world.tileset.TilesetPalette
import java.util.*
import kotlin.math.sign


class AssembleMode(main: PRManiaGame, prevHighScore: EndlessModeScore)
    : AbstractEndlessMode(main, prevHighScore, PlayTimeType.ASSEMBLE) {

    init {
        container.world.worldMode = WorldMode(WorldType.ASSEMBLE, false)
        container.renderer.showEndlessModeScore.set(false)
//        container.engine.inputter.endlessScore.maxLives.set(3)
        container.renderer.worldBackground = AssembleWorldBackground
        container.texturePackSource.set(TexturePackSource.STOCK_GBA)
        TilesetPalette.createAssembleTilesetPalette().applyTo(container.renderer.tileset)
        container.world.tilesetPalette.copyFrom(container.renderer.tileset)
        container.renderer.flashHudRedWhenLifeLost.set(true)
        
        container.resultsText = ResultsText(
                Localization.getValue("play.results.assemble.title"),
                Localization.getValue("play.results.assemble.ok"),
                Localization.getValue("play.results.assemble.tryAgain1"),
                Localization.getValue("play.results.assemble.tryAgain2"),
                Localization.getValue("play.results.assemble.superb1"),
                Localization.getValue("play.results.assemble.superb2"),
        )
    }

    override fun createGlobalContainerSettings(): GlobalContainerSettings {
        return super.createGlobalContainerSettings().copy(forceTexturePack = ForceTexturePack.FORCE_GBA, forceTilesetPalette = ForceTilesetPalette.NO_FORCE)
    }

    override fun initialize() {
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

        val music: BeadsMusic = SidemodeAssets.assembleTheme
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 0f
        musicData.loopParams = LoopParams(SamplePlayer.LoopType.NO_LOOP_FORWARDS, 0.0, music.musicSample.lengthMs)
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
        blocks += InitializationBlock().apply {
            this.beat = 0f
        }
        
        blocks += BlockEndState(engine).apply { 
            this.beat = 142f
        }

        container.addBlocks(blocks)
    }

    inner class InitializationBlock : Block(engine, EnumSet.allOf(BlockType::class.java)) {
        
        override fun compileIntoEvents(): List<Event> {
            fun patternA(startBeat: Float): List<Event> {
                val list = mutableListOf<Event>()
                list += EventAsmSpawnWidgetHalves(engine, 0f, startBeat + 8f)
                list += EventAsmRodBounce(engine, startBeat + 0f, 999, 3, false)
                list += EventAsmRodBounce(engine, startBeat + 1f, 3, 2, false)
                list += EventAsmRodBounce(engine, startBeat + 2f, 2, 1, false)
                list += EventAsmRodBounce(engine, startBeat + 3f, 1, 0, false)
                list += EventAsmRodBounce(engine, startBeat + 4f, 0, 1, false)
                list += EventAsmRodBounce(engine, startBeat + 5f, 1, 2, false)
                list += EventAsmRodBounce(engine, startBeat + 6f, 2, 3, false)
                list += EventAsmRodBounce(engine, startBeat + 7f, 3, 2, true)

                list += EventAsmPistonSpringCharge(engine, world.asmPlayerPiston, startBeat + 7f)
                list += EventAsmPistonSpringUncharge(engine, world.asmPlayerPiston, startBeat + 8f)
                list += EventAsmPrepareSfx(engine, startBeat + 6f)
                
                return list
            }
            fun patternAHalf(startBeat: Float, halvesAreHalf: Boolean = true): List<Event> {
                val list = mutableListOf<Event>()
                list += EventAsmSpawnWidgetHalves(engine, 0f, startBeat + 8f, beatsPerUnit = if (halvesAreHalf) 0.5f else 1f)
                list += EventAsmRodBounce(engine, startBeat + 0f + 0.5f, -1, 0, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 1f, 0, 1, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 1.5f, 1, 2, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 2f, 2, 3, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 2.5f, 3, 2, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 3f, 2, 1, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 3.5f, 1, 0, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 4f, 0, 1, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 4.5f, 1, 2, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 5f, 2, 3, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 5.5f, 3, 2, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 6f, 2, 1, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 6.5f, 1, 0, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 7f, 0, 1, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 7.5f, 1, 2, true, 0.5f)

                list += EventAsmPistonSpringCharge(engine, world.asmPlayerPiston, startBeat + 7f)
                list += EventAsmPistonSpringUncharge(engine, world.asmPlayerPiston, startBeat + 8f)
                list += EventAsmPrepareSfx(engine, startBeat + 6f)
                
                return list
            }
            fun patternAHalfSlow(startBeat: Float): List<Event> {
                val list = mutableListOf<Event>()
                list += EventAsmSpawnWidgetHalves(engine, 0f, startBeat + 8f, beatsPerUnit = 1f)
                list += EventAsmRodBounce(engine, startBeat + 0f + 0.5f, -1, 0, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 1f, 0, 1, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 1.5f, 1, 2, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 2f, 2, 3, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 2.5f, 3, 2, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 3f, 2, 1, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 3.5f, 1, 0, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 4f, 0, -1, false, 0.5f)

                list += EventAsmRodBounce(engine, startBeat + 4f, -1, 0, false, 1f)
                list += EventAsmRodBounce(engine, startBeat + 5f, 0, 1, false, 1f)
                list += EventAsmRodBounce(engine, startBeat + 6f, 1, 3, false, 1f)
                list += EventAsmRodBounce(engine, startBeat + 7f, 3, 2, true, 1f)

                list += EventAsmPistonSpringCharge(engine, world.asmPlayerPiston, startBeat + 7f)
                list += EventAsmPistonSpringUncharge(engine, world.asmPlayerPiston, startBeat + 8f)
                list += EventAsmPrepareSfx(engine, startBeat + 6f)
                
                return list
            }

            fun patternB(startBeat: Float): List<Event> {
                val list = mutableListOf<Event>()
                list += EventAsmSpawnWidgetHalves(engine, 0f, startBeat + 5f)
                list += EventAsmRodBounce(engine, startBeat + 0f, -1, 0, false)
                list += EventAsmRodBounce(engine, startBeat + 1f, 0, 1, false)
                list += EventAsmRodBounce(engine, startBeat + 2f, 1, 2, false)
                list += EventAsmRodBounce(engine, startBeat + 3f, 2, 3, false)
                list += EventAsmRodBounce(engine, startBeat + 4f, 3, 2, true)

                list += EventAsmPistonSpringCharge(engine, world.asmPlayerPiston, startBeat + 4f)
                list += EventAsmPistonSpringUncharge(engine, world.asmPlayerPiston, startBeat + 5f)
                list += EventAsmPrepareSfx(engine, startBeat + 3f)

                return list
            }
            fun patternBHalf(startBeat: Float, halvesAreHalf: Boolean = true): List<Event> {
                val list = mutableListOf<Event>()
                list += EventAsmSpawnWidgetHalves(engine, 0f, startBeat + 5f, beatsPerUnit = if (halvesAreHalf) 0.5f else 1f)
                list += EventAsmRodBounce(engine, startBeat + 0.5f + 0f, -1, 0, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 0.5f + 0.5f, 0, 1, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 0.5f + 1f, 1, 2, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 0.5f + 1.5f, 2, 3, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 0.5f + 2f, 3, 2, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 0.5f + 2.5f, 2, 1, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 0.5f + 3f, 1, 0, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 0.5f + 3.5f, 0, 1, false, 0.5f)
                list += EventAsmRodBounce(engine, startBeat + 0.5f + 4f, 1, 2, true, 0.5f)

                list += EventAsmPistonSpringCharge(engine, world.asmPlayerPiston, startBeat + 4f)
                list += EventAsmPistonSpringUncharge(engine, world.asmPlayerPiston, startBeat + 5f)
                list += EventAsmPrepareSfx(engine, startBeat + 3f)

                return list
            }

            fun patternC(startBeat: Float): List<Event> {
                val list = mutableListOf<Event>()
                list += EventAsmSpawnWidgetHalves(engine, 0f, startBeat + 3f)
                list += EventAsmRodBounce(engine, startBeat + 0f, -1, 0, false)
                list += EventAsmRodBounce(engine, startBeat + 1f, 0, 1, false)
                list += EventAsmRodBounce(engine, startBeat + 2f, 1, 2, true)

                list += EventAsmPistonSpringCharge(engine, world.asmPlayerPiston, startBeat + 2f)
                list += EventAsmPistonSpringUncharge(engine, world.asmPlayerPiston, startBeat + 3f)
                list += EventAsmPrepareSfx(engine, startBeat + 1f)

                return list
            }

            fun patternBoth(startBeat: Float): List<Event> {
                return patternA(startBeat) + patternB(startBeat + 8f)
            }

            val patterns = listOf( // 98, 104, 112, 132, 98, 60, 132, 160
                    patternBoth(7f + 0 * 16f), // 98 Normal

                    patternBoth(7f + 1 * 16f), // 104 Normal + C
                    patternC(7f + 1 * 16f + 13f),

                    patternA(7f + 2 * 16f + 0.5f), // 112 Offbeat
                    patternB(7f + 2 * 16f + 8f),

                    patternA(7f + 3 * 16f + 0.5f), // 132 Offbeat + C
                    patternB(7f + 3 * 16f + 8f),
                    patternC(7f + 3 * 16f + 13f),

                    patternA(7f + 4 * 16f), // 98 Introduce fast B
                    patternBHalf(7f + 4 * 16f + 8f),

                    patternAHalfSlow(7f + 5 * 16f), // 60 Introduce fast A 
                    patternB(7f + 5 * 16f + 8f),

                    patternA(7f + 6 * 16f), // 132 fast B + C
                    patternBHalf(7f + 6 * 16f + 8f, halvesAreHalf = false),
                    patternC(7f + 6 * 16f + 13f),

                    patternAHalf(7f + 7 * 16f), // 160 Both fast + C
                    patternBHalf(7f + 7 * 16f + 8f, halvesAreHalf = false),
                    patternC(7f + 7 * 16f + 13f),

                    ).flatten()
            val playerPistonIndex = world.asmPistons.indexOf(world.asmPlayerPiston)
            val minInputs = patterns.count { it is EventAsmRodBounce && it.toIndex == playerPistonIndex }
            engine.inputter.minimumInputCount = minInputs
            
            val list = listOf(
                    object : Event(engine) {
                        override fun onStart(currentBeat: Float) {
                            engine.playbackSpeed = 1f
                            world.asmPistons.forEach { it.animation = EntityPistonAsm.Animation.Neutral(it) }
                        }
                    }.also { e ->
                        e.beat = -10000f
                    },

                    EventAsmPistonRetractAll(engine, -10000f),
                    ) + patterns
            
            
            return list
        }

        override fun copy(): Block = throw NotImplementedError()
    }
}

// EVENTS ------------------------------------------------------------------------------------------------------

abstract class AbstractEventAsmRod(engine: Engine, startBeat: Float) : AudioEvent(engine) {
    
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
                        val fromIndex: Int, val toIndex: Int,
                        val nextInputIsFire: Boolean = false, val timePerBounce: Float = 1f)
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

            if (engine.areStatisticsEnabled) {
                GlobalStats.rodsDeployed.increment()
                GlobalStats.rodsDeployedAssemble.increment()
            }
        } else {
            super.onStart(currentBeat)
        }
    }

    override fun onAudioStart(atBeat: Float, actualBeat: Float) {
        val world = engine.world
        val pistons = world.asmPistons
        
        if (fromIndex in pistons.indices && !engine.world.entities.any { e -> e is EntityRodAsm && e.acceptingInputs }) {
            return
        }
        
        val playerIndex = pistons.indexOf(world.asmPlayerPiston)
        
        if (fromIndex != playerIndex || engine.autoInputs) {
            if (toIndex == playerIndex) {
                if (nextInputIsFire) {
                    engine.soundInterface.playAudioNoOverlap(SidemodeAssets.assembleSfx.getValue("sfx_asm_compress"), SoundInterface.SFXType.NORMAL)
                }
            }

            if (fromIndex in (0 until pistons.size)) {
                engine.soundInterface.playAudioNoOverlap(SidemodeAssets.assembleSfx.getValue(when (fromIndex) {
                    0 -> "sfx_asm_left"
                    1 -> "sfx_asm_middle_left"
                    2 -> "sfx_asm_middle_right"
                    3 -> "sfx_asm_right"
                    else -> "sfx_asm_left"
                }), SoundInterface.SFXType.NORMAL)
            }
        }
    }

    private fun bounceRod(rod: EntityRodAsm) {
        val world = engine.world
        val pistons = world.asmPistons
        val playerIndex = pistons.indexOf(world.asmPlayerPiston)

        val fromPos = rod.getPistonPosition(engine, fromIndex)
        val toPos = rod.getPistonPosition(engine, toIndex)
        var bounce = EntityRodAsm.BounceAsm(this.beat, timePerBounce, toPos.y + 1f + (if (nextInputIsFire) 4.5f else 3.5f),
                fromPos.x, fromPos.y + 1f, toPos.x, toPos.y + 1f, rod.bounce)
        
        if (nextInputIsFire || toIndex !in 0 until pistons.size) {
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
                val inputBeat = this.beat + timePerBounce
                rod.addExpectedInput(EntityRodAsm.NextExpected(inputBeat, nextInputIsFire))
                // SFX handled in onAudioStart
                world.asmPlayerPiston.retract()
            } else if (toIndex !in 0 until pistons.size) {
                rod.disableInputs = true
            }
            
            // Bounce the rod
            rod.bounce = bounce
            
            if (fromIndex in pistons.indices) {
                // Play piston extend animation
                world.asmPistons[fromIndex].fullyExtend(engine, this.beat, timePerBounce, doWiggle = true)
                // SFX handled in onAudioStart
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

class EventAsmPrepareSfx(engine: Engine, startBeat: Float) : AudioEvent(engine) {
    
    init {
        this.beat = startBeat
    }

    override fun onAudioStart(atBeat: Float, actualBeat: Float) {
        super.onAudioStart(atBeat, actualBeat)
        val beadsSound = SidemodeAssets.assembleSfx.getValue("sfx_asm_prepare")
        engine.soundInterface.playAudio(beadsSound, SoundInterface.SFXType.NORMAL) { player ->
            player.pitch = engine.tempos.tempoAtBeat(this.beat) / 98f
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
            val score = engine.inputter.endlessScore.score
            score.set(score.get() + 1)
            
            if (engine.areStatisticsEnabled) {
                GlobalStats.widgetsAssembledAssemble.increment()
            }
        }
    }
}
