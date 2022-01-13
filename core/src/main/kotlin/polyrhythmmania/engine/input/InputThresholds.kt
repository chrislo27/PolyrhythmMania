package polyrhythmmania.engine.input

import polyrhythmmania.engine.Engine
import polyrhythmmania.screen.play.PlayScreen
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
        Score(0, 0f, 0, 1, false, false, Challenges.NO_CHANGES, "", "")
        PlayScreen
        
        // Warm up input-related code paths
        val world = World()
        val engine = Engine(SimpleTimingProvider { false }, world, null, null)
        listOf(
                WorldMode(WorldType.POLYRHYTHM, false), WorldMode(WorldType.POLYRHYTHM, true),
                WorldMode(WorldType.DUNK, true), WorldMode(WorldType.ASSEMBLE, false)
        ).forEach { mode ->
            world.worldMode = mode
            world.resetWorld()
            repeat(60) {
                engine.inputter.onAButtonPressed(false)
                engine.inputter.onAButtonPressed(true)
                engine.inputter.onDpadButtonPressed(false)
                engine.inputter.onDpadButtonPressed(true)
                (engine.timingProvider as SimpleTimingProvider).seconds += 1 / 60f
            }
        }
    }
}