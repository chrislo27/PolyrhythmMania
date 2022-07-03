package polyrhythmmania.container

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import polyrhythmmania.engine.input.InputTimingRestriction


data class StoryModeContainerMetadata(
        val lives: Int,
        val inputTimingRestriction: InputTimingRestriction,
) {
    
    companion object {
        const val METADATA_VERSION: Int = 1
        
        val BLANK: StoryModeContainerMetadata = StoryModeContainerMetadata(
                lives = 0,
                inputTimingRestriction = InputTimingRestriction.NORMAL,
        )
        
        fun fromJson(obj: JsonObject): StoryModeContainerMetadata {
            val versionNumber: Int = obj.get("_version").asInt()

            return StoryModeContainerMetadata(
                    obj.getInt("lives", 0),
                    InputTimingRestriction.MAP[obj.getInt("inputTimingRestriction", BLANK.inputTimingRestriction.id)] ?: BLANK.inputTimingRestriction,
            )
        }
    }
    
    fun toJson(): JsonObject {
        return Json.`object`().apply {
            this.add("_version", METADATA_VERSION)
            
            this.add("lives", lives)
            this.add("inputTimingRestriction", inputTimingRestriction.id)
        }
    }
    
}
