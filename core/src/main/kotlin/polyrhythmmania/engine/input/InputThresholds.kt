package polyrhythmmania.engine.input

import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.score.Ranking
import polyrhythmmania.screen.play.AbstractEnginePlayScreen
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.world.World
import polyrhythmmania.world.WorldMode
import polyrhythmmania.world.WorldType


object InputThresholds {
    val MAX_OFFSET_SEC: Float = 7f / 60
    val ACE_OFFSET: Float = 1f / 60
    val GOOD_OFFSET: Float = 3.5f / 60
    val BARELY_OFFSET: Float = 5f / 60

    /**
     * Forces the classloader to initialize the input-related classes to avoid possible stutter on the first input.
     */
    fun initInputClasses() {
        InputResult(0f, InputType.A, 0f, 0f, 0)
        InputScore.ACE
        Ranking.SUPERB
        AbstractEnginePlayScreen
        
        // Warm up input-related code paths
        val world = World()
        val engine = Engine(SimpleTimingProvider { false }, world, null, null)
        listOf(
                WorldMode(WorldType.Polyrhythm()), WorldMode(WorldType.Dunk), WorldMode(WorldType.Assemble),
        ).forEach { mode ->
            world.worldMode = mode
            world.resetWorld()
            val types = InputType.entries
            repeat(60) {
                types.forEach { t ->
                    engine.inputter.onButtonPressed(false, t)
                    engine.inputter.onButtonPressed(true, t)
                }
                when (mode.worldType) {
                    is WorldType.Polyrhythm -> {
                        world.rows.forEach { row ->
                            row.rowBlocks.forEach { rb ->
                                rb.spawn(0f)
                                rb.spawn(1f)
                                rb.fullyExtend(engine, 0f)
                                rb.despawn(0f)
                                rb.despawn(1f)
                            }
                        }
                    }
                    WorldType.Dunk -> {
                        world.dunkPiston.fullyExtend(engine, 0f)
                    }
                    WorldType.Assemble -> {
                        world.asmPistons.forEach { 
                            it.fullyExtend(engine, 0f, 1f, true, true)
                            it.chargeUp(0f)
                            it.uncharge(0f)
                        }
                    }
                }
                (engine.timingProvider as SimpleTimingProvider).seconds += 1 / 60f
            }
        }
    }
}