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

        // TODO put score-related things here; score, skill star, no miss etc
) {

    companion object {
        fun fromJson(obj: JsonObject): StageCompletionData {
            return StageCompletionData(
                    LocalDateTime.ofEpochSecond(obj.getLong("firstClearTime", System.currentTimeMillis() / 1000), 0, ZoneOffset.UTC)
            )
        }
    }

    fun toJson(): JsonObject {
        return Json.`object`().also { o ->
            o.add("firstClearTime", firstClearTime.toEpochSecond(ZoneOffset.UTC))
        }        
    }

}
