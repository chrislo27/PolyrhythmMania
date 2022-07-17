package polyrhythmmania.container

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import polyrhythmmania.engine.input.InputTimingRestriction


data class StoryModeContainerMetadata(
        val inputTimingRestriction: InputTimingRestriction,
        val lives: Int,
        val defectiveRodsThreshold: Int,
) {
    
    companion object {
        const val METADATA_VERSION: Int = 1
        
        val BLANK: StoryModeContainerMetadata = StoryModeContainerMetadata(
                inputTimingRestriction = InputTimingRestriction.NORMAL,
                lives = 0,
                defectiveRodsThreshold = 0,
        )
        
        fun fromJson(obj: JsonObject): StoryModeContainerMetadata {
            val versionNumber: Int = obj.get("_version").asInt()

            return StoryModeContainerMetadata(
                    InputTimingRestriction.MAP[obj.getInt("inputTimingRestriction", BLANK.inputTimingRestriction.id)] ?: BLANK.inputTimingRestriction,
                    obj.getInt("lives", 0),
                    obj.getInt("defectiveRodsThreshold", 0),
            )
        }
    }
    
    fun toJson(): JsonObject {
        return Json.`object`().apply {
            this.add("_version", METADATA_VERSION)
            
            this.add("inputTimingRestriction", inputTimingRestriction.id)
            this.add("lives", lives)
            this.add("defectiveRodsThreshold", defectiveRodsThreshold)
        }
    }
    
}
