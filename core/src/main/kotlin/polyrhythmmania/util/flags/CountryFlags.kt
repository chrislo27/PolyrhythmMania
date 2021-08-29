package polyrhythmmania.util.flags

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import java.util.*


object CountryFlags {

    private const val ICON_SIZE: Int = 32

    data class Flag(val code: String, val index: Int)
    
    val unknownFlag: Flag = Flag("unknown", 0)
    val allFlags: List<Flag> by lazy {
        val array = Json.parse(Gdx.files.internal("textures/flag_icon/regions.json").readString()).asArray()
        array.map { j ->
            j as JsonObject
            Flag(j.getString("code", ""), j.getInt("index", 0))
        }
    }
    val allFlagsByCode: Map<String, Flag> by lazy { allFlags.associateBy { it.code } }
    val allFlagsByCodeLower: Map<String, Flag> by lazy { allFlags.associateBy { it.code.lowercase(Locale.ROOT) } }
    
    fun getFlagByCountryCode(countryCode: String): Flag {
        return allFlagsByCodeLower[countryCode.lowercase(Locale.ROOT)] ?: unknownFlag
    }
    
    fun getTextureRegionForFlag(flag: Flag, texture: Texture): TextureRegion {
        val rows = texture.height / ICON_SIZE
        val x = flag.index / rows
        val y = flag.index % rows
        return TextureRegion(texture, x * ICON_SIZE, y * ICON_SIZE, ICON_SIZE, ICON_SIZE)
    }
}
