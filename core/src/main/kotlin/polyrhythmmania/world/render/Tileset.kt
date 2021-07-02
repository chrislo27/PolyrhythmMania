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
                    this.color.bind { bindTo.use() }
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

    val rodBorderColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val rodFillColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val rodGroundFrameCount: Int = 6
    val rodAerialFrameCount: Int = 6
    private val rodBordersSection: TextureRegion = packedSheet["rods_borders"]
    private val rodFillSection: TextureRegion = packedSheet["rods_fill"]
    val rodGroundBorderAnimations: List<TintedRegion> by lazy {
        (0 until rodGroundFrameCount).map { i ->
            TextureRegion(rodBordersSection, 0, 0 + 17 * i, 24, 16).toTinted(rodBorderColor)
        }
    }
    val rodGroundFillAnimations: List<TintedRegion> by lazy {
        (0 until rodGroundFrameCount).map { i ->
            TextureRegion(rodFillSection, 0, 0 + 17 * i, 24, 16).toTinted(rodFillColor)
        }
    }
    val rodAerialBorderAnimations: List<TintedRegion> by lazy {
        (0 until rodAerialFrameCount).map { i ->
            TextureRegion(rodFillSection, 26, 1 + 17 * i, 24, 16).toTinted(rodBorderColor)
        }
    }
    val rodAerialFillAnimations: List<TintedRegion> by lazy {
        (0 until rodAerialFrameCount).map { i ->
            TextureRegion(rodFillSection, 26, 1 + 17 * i, 24, 16).toTinted(rodFillColor)
        }
    }

    val explosionFrameCount: Int = 4
    val explosionFrames: List<TintedRegion> by lazy {
        val map = packedSheet.getIndexedRegions("explosion")
        (0 until explosionFrameCount).map { i ->
            map.getValue(i).toTinted()
        }
    }
    
    val inputFeedbackStart: TintedRegion = packedSheet.getIndexedRegions("input_feedback").getValue(0).toTinted()
    val inputFeedbackMiddle: TintedRegion = packedSheet.getIndexedRegions("input_feedback").getValue(1).toTinted()
    val inputFeedbackEnd: TintedRegion = packedSheet.getIndexedRegions("input_feedback").getValue(2).toTinted()
    
    val dunkBasketBack: TintedRegion = packedSheet["basket_back"].toTinted()
    val dunkBasketFaceX: TintedRegion = packedSheet["basket_face_x"].toTinted()
    val dunkBasketFaceZ: TintedRegion = packedSheet["basket_face_z"].toTinted()
    val dunkBacking: TintedRegion = packedSheet["hoop_back"].toTinted()
}

class TintedRegion(val region: TextureRegion, initColor: Color = Color.WHITE) {
    val colorObj: Color = Color(1f, 1f, 1f, 1f).set(initColor)
    val color: Var<Color> = Var(colorObj)
}