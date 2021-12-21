package polyrhythmmania.statistics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import paintbox.Paintbox
import polyrhythmmania.PRMania


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
     * Total number of times any tutorial was played.
     */
    val timesPlayedTutorial: Stat = register(Stat("timesPlayedTutorial", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of times Polyrhythm 1 was played.
     */
    val timesPlayedPolyrhythm1: Stat = register(Stat("timesPlayedPolyrhythm1", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of times Polyrhythm 2 was played.
     */
    val timesPlayedPolyrhythm2: Stat = register(Stat("timesPlayedPolyrhythm2", LocalizedStatFormatter.DEFAULT))
    
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
     * Total number of rods dropped/missed in assemble.
     */
    val rodsDroppedAssemble: Stat = register(Stat("rodsDroppedAssemble", LocalizedStatFormatter.DEFAULT))
    /**
     * Total number of widgets assembled in assemble.
     */
    val widgetsAssembledAssemble: Stat = register(Stat("widgetsAssembledAssemble", LocalizedStatFormatter.DEFAULT))
    
    
    // Accumulators for play time --------------------------------------------------------------------------------------
    private val totalPlayTimeAccumulator: TimeAccumulator = TimeAccumulator(totalPlayTime)
    private val editorTimeAccumulator: TimeAccumulator = TimeAccumulator(editorTime)
    private val regularModePlayTimeAccumulator: TimeAccumulator = TimeAccumulator(regularModePlayTime)
    private val endlessPlayTimeAccumulator: TimeAccumulator = TimeAccumulator(endlessModePlayTime)
    private val dailyChallengePlayTimeAccumulator: TimeAccumulator = TimeAccumulator(dailyChallengePlayTime)
    private val dunkPlayTimeAccumulator: TimeAccumulator = TimeAccumulator(dunkPlayTime)
    private val assemblePlayTimeAccumulator: TimeAccumulator = TimeAccumulator(assemblePlayTime)

    // -----------------------------------------------------------------------------------------------------------------

    fun load() {
        this.fromJsonFile(storageLoc)
    }

    fun persist() {
        Paintbox.LOGGER.debug("GlobalStats saved", "Statistics")
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
        }
        acc.update()
    }

}