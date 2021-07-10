package polyrhythmmania.world.render

import com.badlogic.gdx.graphics.Color
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.util.gdxutils.set


/**
 * Describes a particular configuration of the colours for [Tileset].
 */
class TilesetConfig {
    
    companion object {
        fun createGBA1TilesetConfig(): TilesetConfig {
            return TilesetConfig().apply {
                rodBorder.color.getOrCompute().set(0, 0, 0)
                rodFill.color.getOrCompute().set(255, 8, 0)
                
                cubeBorder.color.getOrCompute().set(33, 214, 25)
                cubeBorderZ.color.getOrCompute().set(24, 181, 16)
                cubeFaceX.color.getOrCompute().set(42, 224, 48)
                cubeFaceY.color.getOrCompute().set(74, 255, 74)
                cubeFaceZ.color.getOrCompute().set(58, 230, 49)

                pistonFaceX.color.getOrCompute().set(33, 82, 206)
                pistonFaceZ.color.getOrCompute().set(41, 99, 255)

                signShadow.color.getOrCompute().set(33, 214, 25)
            }
        }

        fun createGBA2TilesetConfig(): TilesetConfig {
            return TilesetConfig().apply {
                rodBorder.color.getOrCompute().set(0, 0, 0)
                rodFill.color.getOrCompute().set(255, 8, 0)

                cubeBorder.color.getOrCompute().set(0, 16, 189)
                cubeBorderZ.color.getOrCompute().set(24, 66, 173)
                cubeFaceX.color.getOrCompute().set(32, 81, 204)
                cubeFaceY.color.getOrCompute().set(41, 99, 255)
                cubeFaceZ.color.getOrCompute().set(33, 82, 214)

                pistonFaceX.color.getOrCompute().set(214, 181, 8)
                pistonFaceZ.color.getOrCompute().set(255, 214, 16)

                signShadow.color.getOrCompute().set(0, 16, 189)
            }
        }
    }

    val cubeBorder: ColorMapping = ColorMapping("cubeBorder", { it.cubeBorder.color })
    val cubeBorderZ: ColorMapping = ColorMapping("cubeBorderZ", { it.cubeBorderZ.color }, fallbackIDs = listOf("cubeBorder"))
    val cubeFaceX: ColorMapping = ColorMapping("cubeFaceX", { it.cubeFaceX.color })
    val cubeFaceY: ColorMapping = ColorMapping("cubeFaceY", { it.cubeFaceY.color })
    val cubeFaceZ: ColorMapping = ColorMapping("cubeFaceZ", { it.cubeFaceZ.color })
    val pistonFaceX: ColorMapping = ColorMapping("pistonFaceX", { it.pistonFaceXColor })
    val pistonFaceZ: ColorMapping = ColorMapping("pistonFaceZ", { it.pistonFaceZColor })
    val signShadow: ColorMapping = ColorMapping("signShadow", { it.signShadowColor })
    val rodBorder: ColorMapping = ColorMapping("rodBorder", { it.rodBorderColor })
    val rodFill: ColorMapping = ColorMapping("rodFill", { it.rodFillColor })

    val allMappings: List<ColorMapping> = listOf(cubeBorder, cubeBorderZ, cubeFaceX, cubeFaceY, cubeFaceZ,
            pistonFaceX, pistonFaceZ, signShadow, rodBorder, rodFill)
    val allMappingsByID: Map<String, ColorMapping> = allMappings.associateBy { it.id }

    fun copy(): TilesetConfig {
        return TilesetConfig().also { copy ->
            val copyMap = copy.allMappings.associateBy { it.id }
            this.allMappings.forEach { m ->
                copyMap.getValue(m.id).color.set(m.color.getOrCompute().cpy())
            }
        }
    }
    
    fun copyFrom(tileset: Tileset) {
        allMappings.forEach { m ->
            m.copyFrom(tileset)
        }
    }
    
    fun applyTo(tileset: Tileset) {
        allMappings.forEach { m ->
            m.applyTo(tileset)
        }
    }

    fun toJson(): JsonObject {
        return Json.`object`().apply {
            allMappings.forEach { m ->
                add(m.id, m.color.getOrCompute().toString())
            }
        }
    }

    fun fromJson(obj: JsonObject) {
        fun attemptParse(id: String): Color? {
            return try {
                val str = obj.getString(id, "")
                if (str == "") return null
                Color.valueOf(str)
            } catch (ignored: Exception) {
                null
            }
        }

        allMappings.forEach { m ->
            val c = attemptParse(m.id)
            if (c != null) {
                m.color.set(c)
            } else {
                if (m.fallbackIDs.isNotEmpty()) {
                    for (fallback in m.fallbackIDs) {
                        val fallbackColor = attemptParse(fallback)
                        if (fallbackColor != null) {
                            m.color.set(fallbackColor)
                            break
                        }
                    }
                }
            }
        }
    }
}