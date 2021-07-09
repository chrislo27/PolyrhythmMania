package polyrhythmmania.world

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject


data class WorldSettings(
        val showInputIndicators: Boolean = false,
) {
    companion object {
        val DEFAULT: WorldSettings = WorldSettings()

        fun fromJson(obj: JsonObject): WorldSettings {
            val showInputIndicators: Boolean = obj.get("showInputIndicators")?.takeIf { it.isBoolean }?.asBoolean() ?: DEFAULT.showInputIndicators
            
            return WorldSettings(showInputIndicators = showInputIndicators)
        }
    }
    
    fun toJson(): JsonObject {
        return Json.`object`().apply {
            add("showInputIndicators", showInputIndicators)
        }
    }
}
