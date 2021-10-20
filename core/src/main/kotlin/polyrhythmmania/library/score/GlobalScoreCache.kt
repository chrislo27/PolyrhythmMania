package polyrhythmmania.library.score

import com.badlogic.gdx.files.FileHandle
import paintbox.Paintbox
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import polyrhythmmania.PRMania
import java.io.File
import java.util.*
import java.util.function.Consumer


object GlobalScoreCache {
    
    val storageLoc: File by lazy { PRMania.MAIN_FOLDER.resolve("prefs/score_cache.json") }
    val scoreCache: ReadOnlyVar<ScoreCache> by lazy {
        Var(ScoreCache.fromJsonFile(FileHandle(storageLoc)))
    }
    
    fun pushNewLevelScoreAttempt(levelUUID: UUID, lsa: LevelScoreAttempt) {
        val oldCache = scoreCache.getOrCompute()
        
        val levelScore = oldCache.map[levelUUID] ?: LevelScore(levelUUID, 0, emptyList())
        val newLevelScore = levelScore.copy(playCount = levelScore.playCount + 1, attempts = levelScore.attempts + lsa).keepXBestAttempts()
        
        val newScoreCache = oldCache.copy(map = oldCache.map + Pair(newLevelScore.uuid, newLevelScore))
        
        (scoreCache as Var).set(newScoreCache)
        Paintbox.LOGGER.debug("Added new level score attempt to score cache: $levelUUID $lsa")
        persist()
    }
    
    fun clearAll() {
        (scoreCache as Var).set(ScoreCache(emptyMap()))
        Paintbox.LOGGER.debug("Cleared score cache")
        persist()
    }
    
    @Synchronized
    private fun persist() {
        try {
            val file: File = storageLoc
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            
            scoreCache.getOrCompute().toJsonFile(FileHandle(file))
            Paintbox.LOGGER.info("Persisted score cache")
        } catch (e: Exception) {
            Paintbox.LOGGER.error("Failed to save score cache!")
            e.printStackTrace()
        }
    }
    
    fun createConsumer(levelUUID: UUID): Consumer<LevelScoreAttempt> {
        return Consumer { lsa ->
            GlobalScoreCache.pushNewLevelScoreAttempt(levelUUID, lsa)
        }
    }
    
}