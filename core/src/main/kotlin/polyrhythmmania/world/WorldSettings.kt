package polyrhythmmania.world

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject


data class WorldSettings(
    val showInputIndicators: Boolean = false,
    val useEnglishSigns: Boolean = false,
) {

    companion object {

        val DEFAULT: WorldSettings = WorldSettings()

        fun fromJson(obj: JsonObject): WorldSettings {
            val showInputIndicators: Boolean =
                obj.get("showInputIndicators")?.takeIf { it.isBoolean }?.asBoolean() ?: DEFAULT.showInputIndicators
            val useEnglishSigns: Boolean =
                obj.get("useEnglishSigns")?.takeIf { it.isBoolean }?.asBoolean() ?: DEFAULT.useEnglishSigns

            return WorldSettings(
                showInputIndicators = showInputIndicators,
                useEnglishSigns = useEnglishSigns
            )
        }
    }

    fun toJson(): JsonObject {
        return Json.`object`().apply {
            add("showInputIndicators", showInputIndicators)
            add("useEnglishSigns", useEnglishSigns)
        }
    }
}
