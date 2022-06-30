package polyrhythmmania.container

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject


data class StoryModeContainerMetadata(
        val nodata: Int = 0 // FIXME
) {
    
    companion object {
        const val METADATA_VERSION: Int = 1
        
        val BLANK: StoryModeContainerMetadata = StoryModeContainerMetadata(
                0
        )
        
        fun fromJson(obj: JsonObject): StoryModeContainerMetadata {
//            val showInputIndicators: Boolean = obj.get("showInputIndicators")?.takeIf { it.isBoolean }?.asBoolean() ?: WorldSettings.DEFAULT.showInputIndicators

            return StoryModeContainerMetadata()
        }
    }
    
    fun toJson(): JsonObject {
        return Json.`object`().apply {
            this.add("_version", METADATA_VERSION)
            // TODO
        }
    }
    
}
