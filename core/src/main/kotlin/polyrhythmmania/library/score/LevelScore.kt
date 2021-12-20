package polyrhythmmania.library.score

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import java.time.Instant
import java.util.*


data class LevelScore(val uuid: UUID, val playCount: Int, val attempts: List<LevelScoreAttempt>,
                      val lastPlayed: Instant?) {
    
    companion object {
        fun fromJson(obj: JsonObject): LevelScore {
            val attemptsObj = obj.get("attempts").asArray()
            return LevelScore(UUID.fromString(obj.getString("uuid", "")), obj.getInt("playCount", 0),
                    attemptsObj.map { LevelScoreAttempt.fromJson(it.asObject()) },
                    try { obj.get("lastPlayed")?.let { Instant.ofEpochSecond(it.asLong()) } } catch (ignored: Exception) { null })
        } 
    }
    
    fun toJson(obj: JsonObject) {
        obj.add("uuid", uuid.toString())
        obj.add("playCount", playCount)
        if (lastPlayed != null) {
            obj.add("lastPlayed", lastPlayed.epochSecond)
        }
        obj.add("attempts", Json.array().also { arr ->
            attempts.forEach { attempt ->
                arr.add(Json.`object`().also { obj ->
                    attempt.toJson(obj)
                })
            }
        })
    }
    
    fun keepXBestAttempts(limit: Int = 10): LevelScore {
        return this.copy(attempts = this.attempts.sorted().asReversed().take(limit))
    }
}
