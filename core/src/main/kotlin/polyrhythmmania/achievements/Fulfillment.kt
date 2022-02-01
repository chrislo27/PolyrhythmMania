package polyrhythmmania.achievements

import com.eclipsesource.json.JsonObject
import java.time.Instant


/**
 * Denotes if an achievement was fulfilled or not.
 * 
 * Null if not yet fulfilled.
 */
data class Fulfillment(val gotAt: Instant) {
    
    companion object {
        fun fromJson(json: JsonObject): Fulfillment? {
            val gotAt = json.getLong("gotAt", 0L)
            if (gotAt == 0L) return null
            return Fulfillment(Instant.ofEpochSecond(gotAt))
        }
    }
    
    fun toJson(json: JsonObject) {
        json.add("gotAt", gotAt.epochSecond)
    }
    
}
