package polyrhythmmania.container

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject


data class StoryModeContainerMetadata(
        val lives: Int,
) {
    
    companion object {
        const val METADATA_VERSION: Int = 1
        
        val BLANK: StoryModeContainerMetadata = StoryModeContainerMetadata(
                lives = 0,
        )
        
        fun fromJson(obj: JsonObject): StoryModeContainerMetadata {
            val versionNumber: Int = obj.get("_version").asInt()

            return StoryModeContainerMetadata(obj.getInt("lives", 0))
        }
    }
    
    fun toJson(): JsonObject {
        return Json.`object`().apply {
            this.add("_version", METADATA_VERSION)
            
            this.add("lives", lives)
        }
    }
    
}
