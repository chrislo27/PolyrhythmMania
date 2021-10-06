package polyrhythmmania.container.manifest

import com.eclipsesource.json.JsonObject
import paintbox.util.Version
import polyrhythmmania.container.Container
import polyrhythmmania.container.ContainerException
import polyrhythmmania.container.LevelMetadata
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


data class ExportStatistics(
        val durationSec: Float,
        val inputCount: Int,
        val averageBPM: Float,
        val minBPM: Float,
        val maxBPM: Float,
) {
    
    companion object {
        fun fromJson(jsonObj: JsonObject): ExportStatistics {
            return ExportStatistics(jsonObj.getFloat("durationSec", 0f), jsonObj.getInt("inputCount", 0),
                    jsonObj.getFloat("averageBPM", 120f), jsonObj.getFloat("minBPM", 120f), jsonObj.getFloat("maxBPM", 120f))
        }
    }
    
    val averageInputsPerMinute: Float = inputCount / (durationSec / 60f)

    fun writeToJson(jsonObj: JsonObject) {
        jsonObj.add("durationSec", this.durationSec)
        jsonObj.add("inputCount", this.inputCount)
        jsonObj.add("averageBPM", this.averageBPM)
        jsonObj.add("minBPM", this.minBPM)
        jsonObj.add("maxBPM", this.maxBPM)
    }
}
