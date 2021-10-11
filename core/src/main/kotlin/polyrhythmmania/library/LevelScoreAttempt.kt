package polyrhythmmania.library

import com.eclipsesource.json.JsonObject


/**
 * One attempt at a level.
 */
data class LevelScoreAttempt(val playTime: Long, val highScore: Int, val noMiss: Boolean, val skillStar: Boolean)
    : Comparable<LevelScoreAttempt> {
    
    companion object {
        fun fromJson(obj: JsonObject): LevelScoreAttempt {
            return LevelScoreAttempt(obj.getLong("playTime", 0L), obj.getInt("highScore", 0),
                    obj.getBoolean("noMiss", false), obj.getBoolean("skillStar", false))
        }
    }
    
    fun toJson(obj: JsonObject) {
        obj.add("playTime", playTime)
        obj.add("highScore", highScore)
        obj.add("noMiss", noMiss)
        obj.add("skillStar", skillStar)
    }
    
    override fun compareTo(other: LevelScoreAttempt): Int {
        if (this == other) return 0
        if (this.highScore > other.highScore) return 1
        if (this.highScore < other.highScore) return -1
        if (this.noMiss && !other.noMiss) return 1
        if (!this.noMiss && other.noMiss) return -1
        if (this.skillStar && !other.skillStar) return 1
        if (!this.skillStar && other.skillStar) return -1
        return 0
    }
}
