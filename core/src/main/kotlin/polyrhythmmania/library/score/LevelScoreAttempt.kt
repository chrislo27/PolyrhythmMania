package polyrhythmmania.library.score

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import polyrhythmmania.engine.input.Challenges


/**
 * One attempt at a level.
 */
data class LevelScoreAttempt(
        val playTime: Long, val score: Int, val noMiss: Boolean, val skillStar: Boolean,
        val challenges: Challenges,
) : Comparable<LevelScoreAttempt> {

    companion object {
        fun fromJson(obj: JsonObject): LevelScoreAttempt {
            val challengesObj = obj.get("challenges")
            val challenges: Challenges = if (challengesObj != null && challengesObj.isObject)
                Challenges.fromJson(challengesObj.asObject())
            else Challenges.NO_CHANGES
            
            return LevelScoreAttempt(obj.getLong("t", 0L), obj.getInt("s", 0),
                    obj.getBoolean("noMiss", false), obj.getBoolean("skillStar", false), challenges)
        }
    }
    
    fun toJson(obj: JsonObject) {
        obj.add("t", playTime)
        obj.add("s", score)
        obj.add("noMiss", noMiss)
        obj.add("skillStar", skillStar)
        if (challenges != Challenges.NO_CHANGES) {
            val o = Json.`object`()
            challenges.toJson(o)
            obj.add("challenges", o)
        }
    }
    
    override fun compareTo(other: LevelScoreAttempt): Int {
        if (this == other) return 0
        if (this.score > other.score) return 1
        if (this.score < other.score) return -1
        if (this.noMiss && !other.noMiss) return 1
        if (!this.noMiss && other.noMiss) return -1
        if (this.skillStar && !other.skillStar) return 1
        if (!this.skillStar && other.skillStar) return -1
        return 0
    }
}
