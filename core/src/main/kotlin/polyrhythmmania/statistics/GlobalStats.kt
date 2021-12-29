package polyrhythmmania.statistics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import paintbox.Paintbox
import polyrhythmmania.PRMania
import polyrhythmmania.engine.input.EngineInputter


object GlobalStats : Stats() {

    private class TimeAccumulator(val stat: Stat) {
        private var msAcc: Float = 0f

        fun update() {
            msAcc += Gdx.graphics.deltaTime * 1000
            
            if (msAcc >= 1000f) {
                val addSeconds = msAcc.toLong() / 1000L
                stat.increment(addSeconds.toInt().coerceAtLeast(0))
                msAcc = (msAcc - addSeconds * 1000).coerceAtLeast(0f)
            }
        }
    }

    private val storageLoc: FileHandle by lazy { FileHandle(PRMania.MAIN_FOLDER.resolve("prefs/statistics.json")) }

    // Register statistics
    
    // Play time -------------------------------------------------------------------------------------------------------
    
    /**
     * Total play time while the program is running.
     */
    val totalPlayTime: Stat = register(Stat("totalPlayTime", DurationStatFormatter.DEFAULT))
    /**
     * Total time while the editor is active.
     */
    val editorTime: Stat = register(Stat("editorTime", DurationStatFormatter.DEFAULT))
    /**
     * Total time playing regular Polyrhythm (includes practice and PR1+2).
     */
    val regularModePlayTime: Stat = register(Stat("regularModePlayTime", DurationStatFormatter.DEFAULT))
    /**
     * Total time playing Endless Mode (excludes Daily Challenge).
     */
    val endlessModePlayTime: Stat = register(Stat("endlessModePlayTime", DurationStatFormatter.DEFAULT))
    /**
     * Total time playing Daily Challenge.
     */
    val dailyChallengePlayTime: Stat = register(Stat("dailyChallengePlayTime", DurationStatFormatter.DEFAULT))
    /**
     * Total time playing dunk extra mode.
     */
    val dunkPlayTime: Stat = register(Stat("dunkPlayTime", DurationStatFormatter.DEFAULT))
    /**
     * Total time playing assemble extra mode.
     */
    val assemblePlayTime: Stat = register(Stat("assemblePlayTime", DurationStatFormatter.DEFAULT))
    /**
     * Total time playing solitaire extra mode.
     */
    val solitairePlayTime: Stat = register(Stat("solitairePlayTime", DurationStatFormatter.DEFAULT))

    
    
    // Counters for entering modes (times played) ----------------------------------------------------------------------
    
    /**
     * Total number of times the game was started. Incremented on game start. Reset value is 1 intentionally.
     */
    val timesGameStarted: Stat = register(Stat("timesGameStarted", LocalizedStatFormatter.DEFAULT, initialValue = 0, resetValue = 1))
    
    /**
     * Total number of times any custom level was played (library or direct loading).
     */
    val timesPlayedCustomLevel: Stat = register(Stat("timesPlayedCustomLevel", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of times a NON-LEGACY custom level was played for the first time (library or direct loading).
     * Level's UUID is checked and this stat is incremented ONLY IF its lastPlayed timestamp is null.
     */
    val timesPlayedUniqueCustomLevel: Stat = register(Stat("timesPlayedUniqueCustomLevel", LocalizedStatFormatter.DEFAULT))
    
    /**
     * Total number of times any tutorial was played.
     */
    val timesPlayedTutorial: Stat = register(Stat("timesPlayedTutorial", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of times the Polyrhythm 1 practice was played.
     */
    val timesPlayedPracticePolyrhythm1: Stat = register(Stat("timesPlayedPracticePolyrhythm1", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of times the Polyrhythm 2 practice was played.
     */
    val timesPlayedPracticePolyrhythm2: Stat = register(Stat("timesPlayedPracticePolyrhythm2", LocalizedStatFormatter.DEFAULT))
    
    /**
     * Total number of times Endless Mode was played.
     */
    val timesPlayedEndlessMode: Stat = register(Stat("timesPlayedEndlessMode", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of times Daily Challenge was played.
     */
    val timesPlayedDailyChallenge: Stat = register(Stat("timesPlayedDailyChallenge", LocalizedStatFormatter.DEFAULT))
    
    /**
     * Total number of times Dunk was played.
     */
    val timesPlayedDunk: Stat = register(Stat("timesPlayedDunk", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of times Assemble was played.
     */
    val timesPlayedAssemble: Stat = register(Stat("timesPlayedAssemble", LocalizedStatFormatter.DEFAULT))
   
    
    
    // Inputs and scoring ----------------------------------------------------------------------------------------------

    /**
     * Total number of Try Agains gotten. Incremented at results screen.
     */
    val rankingTryAgain: Stat = register(Stat("rankingTryAgain", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of OKs gotten. Incremented at results screen.
     */
    val rankingOK: Stat = register(Stat("rankingOK", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of Superbs gotten. Incremented at results screen.
     */
    val rankingSuperb: Stat = register(Stat("rankingSuperb", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of Skill Stars earned. Incremented immediately when it happens.
     */
    val skillStarsEarned: Stat = register(Stat("skillStarsEarned", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of No Misses earned. Incremented at results screen.
     */
    val noMissesGotten: Stat = register(Stat("noMissesGotten", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of Perfects earned. Incremented at results screen.
     */
    val perfectsEarned: Stat = register(Stat("perfectsEarned", LocalizedStatFormatter.DEFAULT)) 
    /**
     * Total number of Perfects lost. Incremented immediately when it happens.
     */
    val perfectsLost: Stat = register(Stat("perfectsLost", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of inputs that were not misses.
     * Polyrhythm (non-endless): Incremented at end based on input results.
     * Endless: Incremented in [EngineInputter.submitInputsFromRod] when lives > 0.
     * Assemble: Incremented at end based on input results.
     * Dunk: Increments when a Dunk rod explodes OR when the score is incremented
     */
    val inputsGottenTotal: Stat = register(Stat("inputsGottenTotal", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of inputs that were misses or weren't registered.
     * Polyrhythm (non-endless): Incremented at end based on input results.
     * Endless: Incremented in [EngineInputter.submitInputsFromRod] when lives > 0.
     * Assemble: Incremented at end based on input results.
     * Dunk: Incremented when a Dunk rod explodes
     */
    val inputsMissed: Stat = register(Stat("inputsMissed", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of inputs that were Aces.
     * Polyrhythm (non-endless): Incremented at end based on input results.
     * Endless: Incremented in [EngineInputter.submitInputsFromRod] when lives > 0.
     * Assemble: Incremented at end based on input results.
     * Dunk: Incremented when the score is incremented
     */
    val inputsGottenAce: Stat = register(Stat("inputsGottenAce", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of inputs that were Good.
     * Polyrhythm (non-endless): Incremented at end based on input results.
     * Endless: Incremented in [EngineInputter.submitInputsFromRod] when lives > 0.
     * Assemble: Incremented at end based on input results.
     * Dunk: N/A (no goods in Dunk)
     */
    val inputsGottenGood: Stat = register(Stat("inputsGottenGood", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of inputs that were Barely.
     * Polyrhythm (non-endless): Incremented at end based on input results.
     * Endless: Incremented in [EngineInputter.submitInputsFromRod] when lives > 0.
     * Assemble: Incremented at end based on input results.
     * Dunk: N/A (no barelies in Dunk)
     */
    val inputsGottenBarely: Stat = register(Stat("inputsGottenBarely", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of non-Ace inputs that were early.
     * Polyrhythm (non-endless): Incremented at end based on input results.
     * Endless: Incremented in [EngineInputter.submitInputsFromRod] when lives > 0.
     * Assemble: Incremented at end based on input results.
     * Dunk: Incremented when a non-miss non-ace input is received
     */
    val inputsGottenEarly: Stat = register(Stat("inputsGottenEarly", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of non-Ace inputs that were late.
     * Polyrhythm (non-endless): Incremented at end based on input results.
     * Endless: Incremented in [EngineInputter.submitInputsFromRod] when lives > 0.
     * Assemble: Incremented at end based on input results.
     * Dunk: Incremented during the event that triggers an explosion when a non-miss non-ace input is received
     */
    val inputsGottenLate: Stat = register(Stat("inputsGottenLate", LocalizedStatFormatter.DEFAULT))
    
    
    
    // World entities --------------------------------------------------------------------------------------------------
    
    /**
     * Total number of rods deployed across all modes.
     */
    val rodsDeployed: Stat = register(Stat("rodsDeployed", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of rods exploded across all modes.
     */
    val rodsExploded: Stat = register(Stat("rodsExploded", LocalizedStatFormatter.DEFAULT))
    
    /**
     * Total number of rods deployed in regular Polyrhythm.
     */
    val rodsDeployedPolyrhythm: Stat = register(Stat("rodsDeployedPolyrhythm", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of rods ferried in regular Polyrhythm.
     * Only counts if there was at least one expected input for the rod.
     */
    val rodsFerriedPolyrhythm: Stat = register(Stat("rodsFerriedPolyrhythm", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of rods exploded in regular Polyrhythm.
     */
    val rodsExplodedPolyrhythm: Stat = register(Stat("rodsExplodedPolyrhythm", LocalizedStatFormatter.DEFAULT))
    
    /**
     * Total number of rods deployed in dunk.
     */
    val rodsDeployedDunk: Stat = register(Stat("rodsDeployedDunk", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of rods dunked in dunk.
     */
    val rodsDunkedDunk: Stat = register(Stat("rodsDunkedDunk", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of rods missed/exploded in dunk.
     */
    val rodsMissedDunk: Stat = register(Stat("rodsMissedDunk", LocalizedStatFormatter.DEFAULT))
    
    /**
     * Total number of rods deployed in assemble.
     */
    val rodsDeployedAssemble: Stat = register(Stat("rodsDeployedAssemble", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of widgets assembled in assemble.
     */
    val widgetsAssembledAssemble: Stat = register(Stat("widgetsAssembledAssemble", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of rods dropped/missed in assemble.
     */
    val rodsDroppedAssemble: Stat = register(Stat("rodsDroppedAssemble", LocalizedStatFormatter.DEFAULT))
    
    /**
     * Total number of solitaire games played.
     */
    val solitaireGamesPlayed: Stat = register(Stat("solitaireGamesPlayed", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of games won in solitaire.
     */
    val solitaireGamesWon: Stat = register(Stat("solitaireGamesWon", LocalizedStatFormatter.DEFAULT))
    
    
    // Accumulators for play time --------------------------------------------------------------------------------------
    
    private val totalPlayTimeAccumulator: TimeAccumulator = TimeAccumulator(totalPlayTime)
    private val editorTimeAccumulator: TimeAccumulator = TimeAccumulator(editorTime)
    private val regularModePlayTimeAccumulator: TimeAccumulator = TimeAccumulator(regularModePlayTime)
    private val endlessPlayTimeAccumulator: TimeAccumulator = TimeAccumulator(endlessModePlayTime)
    private val dailyChallengePlayTimeAccumulator: TimeAccumulator = TimeAccumulator(dailyChallengePlayTime)
    private val dunkPlayTimeAccumulator: TimeAccumulator = TimeAccumulator(dunkPlayTime)
    private val assemblePlayTimeAccumulator: TimeAccumulator = TimeAccumulator(assemblePlayTime)
    private val solitairePlayTimeAccumulator: TimeAccumulator = TimeAccumulator(solitairePlayTime)

    // -----------------------------------------------------------------------------------------------------------------
    
    fun load() {
        Paintbox.LOGGER.debug("Statistics loaded", "GlobalStats")
        this.fromJsonFile(storageLoc)
    }

    fun persist() {
        Paintbox.LOGGER.debug("Statistics saved", "GlobalStats")
        this.toJsonFile(storageLoc)
    }

    // -----------------------------------------------------------------------------------------------------------------

    fun updateTotalPlayTime() {
        totalPlayTimeAccumulator.update()
    }

    fun updateEditorPlayTime() {
        editorTimeAccumulator.update()
    }
    
    fun updateModePlayTime(type: PlayTimeType) {
        val acc: TimeAccumulator = when (type) {
            PlayTimeType.REGULAR -> regularModePlayTimeAccumulator
            PlayTimeType.ENDLESS -> endlessPlayTimeAccumulator
            PlayTimeType.DAILY_CHALLENGE -> dailyChallengePlayTimeAccumulator
            PlayTimeType.DUNK -> dunkPlayTimeAccumulator
            PlayTimeType.ASSEMBLE -> assemblePlayTimeAccumulator
            PlayTimeType.SOLITAIRE -> solitairePlayTimeAccumulator
        }
        acc.update()
    }

}