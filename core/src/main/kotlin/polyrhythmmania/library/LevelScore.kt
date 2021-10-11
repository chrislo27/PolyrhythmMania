package polyrhythmmania.library

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import java.util.*


data class LevelScore(val uuid: UUID, val playCount: Int, val attempts: List<LevelScoreAttempt>) {
    
    companion object {
        fun fromJson(obj: JsonObject): LevelScore {
            val attemptsObj = obj.get("attempts").asArray()
            return LevelScore(UUID.fromString(obj.getString("uuid", "")), obj.getInt("playCount", 0),
                    attemptsObj.map { LevelScoreAttempt.fromJson(it.asObject()) })
        } 
    }
    
    fun toJson(obj: JsonObject) {
        obj.add("uuid", uuid.toString())
        obj.add("playCount", playCount)
        obj.add("attempts", Json.array().also { arr ->
            attempts.forEach { attempt ->
                arr.add(Json.`object`().also { obj ->
                    attempt.toJson(obj)
                })
            }
        })
    }
    
    fun keepXBestAttempts(limit: Int = 10): LevelScore {
        return this.copy(attempts = this.attempts.sorted().take(limit))
    }
}
