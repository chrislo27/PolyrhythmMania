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
                stat.value.incrementAndGetBy(addSeconds.toInt().coerceAtLeast(0))
                msAcc = (msAcc - addSeconds * 1000).coerceAtLeast(0f)
            }
        }
    }

    private val storageLoc: FileHandle by lazy { FileHandle(PRMania.MAIN_FOLDER.resolve("prefs/statistics.json")) }

    // Register statistics
    
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


    // Accumulators for play time
    private val totalPlayTimeAccumulator: TimeAccumulator = TimeAccumulator(totalPlayTime)
    private val editorTimeAccumulator: TimeAccumulator = TimeAccumulator(editorTime)
    private val regularModePlayTimeAccumulator: TimeAccumulator = TimeAccumulator(regularModePlayTime)
    private val endlessPlayTimeAccumulator: TimeAccumulator = TimeAccumulator(endlessModePlayTime)
    private val dailyChallengePlayTimeAccumulator: TimeAccumulator = TimeAccumulator(dailyChallengePlayTime)
    private val dunkPlayTimeAccumulator: TimeAccumulator = TimeAccumulator(dunkPlayTime)
    private val assemblePlayTimeAccumulator: TimeAccumulator = TimeAccumulator(assemblePlayTime)

    // ---------------------------------------------------------------------------------------------------------------

    fun load() {
        this.fromJsonFile(storageLoc)
    }

    fun persist() {
        Paintbox.LOGGER.debug("GlobalStats saved", "Statistics")
        this.toJsonFile(storageLoc)
    }

    // ---------------------------------------------------------------------------------------------------------------

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