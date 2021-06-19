package polyrhythmmania.world.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.packing.PackedSheet
import paintbox.util.gdxutils.set


class Tileset(val packedSheet: PackedSheet) {
    
    companion object {
        private fun TextureRegion.toTinted(initColor: Color = Color.WHITE): TintedRegion = 
                TintedRegion(this, initColor)
        
        private fun TextureRegion.toTinted(bindTo: ReadOnlyVar<Color>): TintedRegion = 
                TintedRegion(this).apply { 
                    this.color.sideEffecting { bindTo.use() }
                }
        
        fun createGBA1Tileset(packedSheet: PackedSheet): Tileset {
            return Tileset(packedSheet).apply { 
                cubeBorder.colorObj.set(33, 214, 25)
                cubeFaceX.colorObj.set(42, 224, 48)
                cubeFaceY.colorObj.set(74, 255, 74)
                cubeFaceZ.colorObj.set(42, 230, 49)

                pistonFaceXColor.set(Color().set(33, 82, 206))
                pistonFaceZColor.set(Color().set(41, 99, 255))

                signShadowColor.set(Color().set(33, 214, 25))
            }
        }
        
        fun createGBA2Tileset(packedSheet: PackedSheet): Tileset {
            return Tileset(packedSheet).apply { 
                cubeBorder.colorObj.set(0, 16, 189)
                cubeFaceX.colorObj.set(32, 81, 204)
                cubeFaceY.colorObj.set(41, 99, 255)
                cubeFaceZ.colorObj.set(33, 82, 214)
                
                pistonFaceXColor.set(Color().set(214, 181, 8))
                pistonFaceZColor.set(Color().set(255, 214, 16))
                
                signShadowColor.set(Color().set(0, 16, 189))
            }
        }
    }

    /**
     * A white cube. Used as the platform for the rods.
     */
    val platform: TintedRegion = packedSheet["platform"].toTinted()

    /**
     * Like [platform] but with the red marking line on it.
     */
    val platformWithLine: TintedRegion = packedSheet["platform_with_line"].toTinted()

    /**
     * Just the top -Z edge of [platform], a black line. Used on top of other cubes.
     */
    val platformBorder: TintedRegion = packedSheet["platform_border"].toTinted()

    /**
     * The red line to be drawn on top of [cubeBorder].
     */
    val redLine: TintedRegion = packedSheet["red_line"].toTinted()
    val cubeBorder: TintedRegion = packedSheet["cube_border"].toTinted()
    val cubeFaceX: TintedRegion = packedSheet["cube_face_x"].toTinted()
    val cubeFaceY: TintedRegion = packedSheet["cube_face_y"].toTinted()
    val cubeFaceZ: TintedRegion = packedSheet["cube_face_z"].toTinted()
    
    val pistonFaceXColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val pistonFaceZColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    
    val pistonARetracted: TintedRegion = packedSheet["piston_a"].toTinted()
    val pistonAPartial: TintedRegion = packedSheet["piston_a_partial"].toTinted()
    val pistonAPartialFaceX: TintedRegion = packedSheet["piston_a_partial_face_x"].toTinted(pistonFaceXColor)
    val pistonAPartialFaceZ: TintedRegion = packedSheet["piston_a_partial_face_z"].toTinted(pistonFaceZColor)
    val pistonAExtended: TintedRegion = packedSheet["piston_a_extended"].toTinted()
    val pistonAExtendedFaceX: TintedRegion = packedSheet["piston_a_extended_face_x"].toTinted(pistonFaceXColor)
    val pistonAExtendedFaceZ: TintedRegion = packedSheet["piston_a_extended_face_z"].toTinted(pistonFaceZColor)
    
    val pistonDpadRetracted: TintedRegion = packedSheet["piston_dpad"].toTinted()
    val pistonDpadPartial: TintedRegion = packedSheet["piston_dpad_partial"].toTinted()
    val pistonDpadPartialFaceX: TintedRegion = packedSheet["piston_dpad_partial_face_x"].toTinted(pistonFaceXColor)
    val pistonDpadPartialFaceZ: TintedRegion = packedSheet["piston_dpad_partial_face_z"].toTinted(pistonFaceZColor)
    val pistonDpadExtended: TintedRegion = packedSheet["piston_dpad_extended"].toTinted()
    val pistonDpadExtendedFaceX: TintedRegion = packedSheet["piston_dpad_extended_face_x"].toTinted(pistonFaceXColor)
    val pistonDpadExtendedFaceZ: TintedRegion = packedSheet["piston_dpad_extended_face_z"].toTinted(pistonFaceZColor)
    
    val indicatorA: TintedRegion = packedSheet["indicator_a"].toTinted()
    val indicatorDpad: TintedRegion = packedSheet["indicator_dpad"].toTinted()
    
    val signShadowColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val signA: TintedRegion = packedSheet["sign_a"].toTinted()
    val signAShadow: TintedRegion = packedSheet["sign_a_shadow"].toTinted(signShadowColor)
    val signDpad: TintedRegion = packedSheet["sign_dpad"].toTinted()
    val signDpadShadow: TintedRegion = packedSheet["sign_dpad_shadow"].toTinted(signShadowColor)
    val signBo: TintedRegion = packedSheet["sign_bo"].toTinted()
    val signBoShadow: TintedRegion = packedSheet["sign_bo_shadow"].toTinted(signShadowColor)
    val signTa: TintedRegion = packedSheet["sign_ta"].toTinted()
    val signTaShadow: TintedRegion = packedSheet["sign_ta_shadow"].toTinted(signShadowColor)
    val signN: TintedRegion = packedSheet["sign_n"].toTinted()
    val signNShadow: TintedRegion = packedSheet["sign_n_shadow"].toTinted(signShadowColor)

    val rodGroundFrameCount: Int = 6
    val rodAerialFrameCount: Int = 6
    private val rodSection: TextureRegion = packedSheet["rods"]
    val rodGroundAnimations: List<TintedRegion> by lazy {
        (0 until rodGroundFrameCount).map { i ->
            TextureRegion(rodSection, 0, 0 + 17 * i, 24, 16).toTinted()
        }
    }
    val rodAerialAnimations: List<TintedRegion> by lazy {
        (0 until rodAerialFrameCount).map { i ->
            TextureRegion(rodSection, 26, 1 + 17 * i, 24, 16).toTinted()
        }
    }

    val explosionFrameCount: Int = 4
    val explosionFrames: List<TintedRegion> by lazy {
        val map = packedSheet.getIndexedRegions("explosion")
        (0 until explosionFrameCount).map { i ->
            map.getValue(i).toTinted()
        }
    }
    
    fun toJson(): JsonObject {
        return Json.`object`().apply { 
            add("cubeBorder", cubeBorder.color.getOrCompute().toString())
            add("cubeFaceX", cubeFaceX.color.getOrCompute().toString())
            add("cubeFaceY", cubeFaceY.color.getOrCompute().toString())
            add("cubeFaceZ", cubeFaceZ.color.getOrCompute().toString())
            add("pistonFaceX", pistonFaceXColor.getOrCompute().toString())
            add("pistonFaceZ", pistonFaceZColor.getOrCompute().toString())
            add("signShadow", signShadowColor.getOrCompute().toString())
        }
    }
    
    fun fromJson(obj: JsonObject) {
        fun attemptParse(id: String): Color? {
            return try {
                Color.valueOf(obj.getString(id, ""))
            } catch (ignored: Exception) { null }
        }
        fun attemptSet(reg: TintedRegion, id: String) {
            val c = attemptParse(id)
            if (c != null) {
                reg.colorObj.set(c)
            }
        }
        fun attemptSet(varr: Var<Color>, id: String) {
            val c = attemptParse(id)
            if (c != null) {
                varr.set(c)
            }
        }
        
        attemptSet(cubeBorder, "cubeBorder")
        attemptSet(cubeFaceX, "cubeFaceX")
        attemptSet(cubeFaceY, "cubeFaceY")
        attemptSet(cubeFaceZ, "cubeFaceZ")
        attemptSet(pistonFaceXColor, "pistonFaceX")
        attemptSet(pistonFaceZColor, "pistonFaceZ")
        attemptSet(signShadowColor, "signShadow")
    }
}

class TintedRegion(val region: TextureRegion, initColor: Color = Color.WHITE) {
    val colorObj: Color = Color(1f, 1f, 1f, 1f).set(initColor)
    val color: Var<Color> = Var(colorObj)
}