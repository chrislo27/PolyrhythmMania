package polyrhythmmania.storymode.inbox

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import java.time.LocalDateTime
import java.time.ZoneOffset


data class StageCompletionData(
        /**
         * First time clearing the stage, in UTC.
         */
        val firstClearTime: LocalDateTime,

        val bestClearTime: LocalDateTime,
        val score: Int,
        val skillStar: Boolean,
        val noMiss: Boolean,
) {

    companion object {
        fun fromJson(obj: JsonObject): StageCompletionData {
            return StageCompletionData(
                    LocalDateTime.ofEpochSecond(obj.getLong("firstClearTime", System.currentTimeMillis() / 1000), 0, ZoneOffset.UTC),
                    LocalDateTime.ofEpochSecond(obj.getLong("bestClearTime", System.currentTimeMillis() / 1000), 0, ZoneOffset.UTC),
                    obj.getInt("score", 0),
                    obj.getBoolean("skillStar", false),
                    obj.getBoolean("noMiss", false),
            )
        }
    }

    fun toJson(): JsonObject {
        return Json.`object`().also { o ->
            o.add("firstClearTime", firstClearTime.toEpochSecond(ZoneOffset.UTC))
            o.add("bestClearTime", bestClearTime.toEpochSecond(ZoneOffset.UTC))
            o.add("score", score)
            o.add("skillStar", skillStar)
            o.add("noMiss", noMiss)
        }        
    }

}
