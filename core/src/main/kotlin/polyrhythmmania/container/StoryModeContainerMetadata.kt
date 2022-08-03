package polyrhythmmania.container

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import polyrhythmmania.engine.input.InputTimingRestriction


data class StoryModeContainerMetadata(
        val inputTimingRestriction: InputTimingRestriction,
        val lives: Int,
        val defectiveRodsThreshold: Int,
        
        val monsterEnabled: Boolean,
        val monsterDifficulty: Float,
        val monsterRecoveryPenalty: Float,
) {
    
    companion object {
        const val METADATA_VERSION: Int = 1
        
        val BLANK: StoryModeContainerMetadata = StoryModeContainerMetadata(
                inputTimingRestriction = InputTimingRestriction.NORMAL,
                lives = 0,
                defectiveRodsThreshold = 0,
                monsterEnabled = false,
                monsterDifficulty = 0f,
                monsterRecoveryPenalty = 50f,
        )
        
        fun fromJson(obj: JsonObject): StoryModeContainerMetadata {
            val versionNumber: Int = obj.get("_version").asInt()

            return StoryModeContainerMetadata(
                    InputTimingRestriction.MAP[obj.getInt("inputTimingRestriction", BLANK.inputTimingRestriction.id)] ?: BLANK.inputTimingRestriction,
                    obj.getInt("lives", 0),
                    obj.getInt("defectiveRodsThreshold", 0),
                    obj.getBoolean("monsterEnabled", false),
                    obj.getFloat("monsterDifficulty", 0f),
                    obj.getFloat("monsterRecoveryPenalty", 50f),
            )
        }
    }
    
    fun toJson(): JsonObject {
        return Json.`object`().apply {
            this.add("_version", METADATA_VERSION)
            
            this.add("inputTimingRestriction", inputTimingRestriction.id)
            this.add("lives", lives)
            this.add("defectiveRodsThreshold", defectiveRodsThreshold)
            
            this.add("monsterEnabled", monsterEnabled)
            this.add("monsterDifficulty", monsterDifficulty)
            this.add("monsterRecoveryPenalty", monsterRecoveryPenalty)
        }
    }
    
}
