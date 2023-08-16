package polyrhythmmania.editor.block.data

import com.badlogic.gdx.graphics.Color
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import polyrhythmmania.world.spotlights.LightColor
import polyrhythmmania.world.spotlights.Spotlights


class SwitchedLightColor(resetColor: Color, defaultStrength: Float) : LightColor(resetColor, defaultStrength) {
    
    var enabled: Boolean = true // If enabled, the event for this light will be added

    fun copyTo(other: SwitchedLightColor) {
        other.color.set(this.color)
        other.strength = this.strength
        other.enabled = this.enabled
    }
    
    fun toPair(): Pair<Color?, Float?> {
        if (!enabled) return Pair(null, null)
        return Pair(this.color, this.strength)
    }
}

class SpotlightsColorData(val numSpotlightsPerRow: Int) {
    
    val ambientLight: SwitchedLightColor = SwitchedLightColor(Spotlights.AMBIENT_LIGHT_RESET_COLOR, 1f)
    val rows: List<List<SwitchedLightColor>> = List(2) { 
        List(numSpotlightsPerRow) { SwitchedLightColor(Spotlights.SPOTLIGHT_RESET_COLOR, 0f) }
    }
    val rowA: List<SwitchedLightColor> = rows[0]
    val rowDpad: List<SwitchedLightColor> = rows[1]
    val allSpotlights: List<SwitchedLightColor> = rows.flatten()
    
    fun copyTo(other: SpotlightsColorData) {        
        other.also { 
            this.ambientLight.copyTo(it.ambientLight)
            this.allSpotlights.forEachIndexed { index, lightColor -> 
                lightColor.copyTo(it.allSpotlights[index])
            }
        }
    }
    
    fun writeToJson(obj: JsonObject) {
        obj.add("ambientLight", this.ambientLight.toJson(Json.`object`()))
        obj.add("rows", Json.array().also { rowsArray ->
            this.rows.forEach { row ->
                val arr = Json.array()
                row.forEach { lc ->
                    arr.add(lc.toJson(Json.`object`()))
                }
                rowsArray.add(arr)
            }
        })
    }

    fun readFromJson(obj: JsonObject) {
        ambientLight.fromJson(obj.get("ambientLight")?.asObject())
        
        val rowsArray = obj.get("rows").asArray()
        allSpotlights.forEach { it.reset() }
        rowsArray.forEachIndexed { index, jsonValue -> 
            if (index in 0..<rows.size) {
                val arr = jsonValue.asArray()
                arr.forEachIndexed { i, v -> 
                    val row = rows[index]
                    if (i in 0..<row.size) {
                        val light = row[i]
                        light.fromJson(v.asObject())
                    }
                }
            }
        }
    }

    private fun SwitchedLightColor.toJson(lightObj: JsonObject): JsonObject {
        lightObj.add("c", this.color.toString())
        lightObj.add("s", this.strength)
        lightObj.add("e", this.enabled)
        return lightObj
    }

    private fun SwitchedLightColor.fromJson(lightObj: JsonObject?) {
        this.reset()
        if (lightObj == null) {
            return
        }
        
        fun attemptParse(id: String): Color? {
            return try {
                val str = lightObj.getString(id, "")
                if (str == "") return null
                Color.valueOf(str)
            } catch (ignored: Exception) {
                null
            }
        }
        this.color.set(attemptParse("c") ?: this.resetColor)
        this.strength = lightObj.getFloat("s", this.defaultStrength)
        this.enabled = lightObj.getBoolean("e", true)
    }

}
