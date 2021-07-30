package polyrhythmmania.world.tileset

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.packing.PackedSheet


class Tileset(val packedSheet: PackedSheet) {

    companion object {
        const val rodFrameCount: Int = 6
        
        private fun TextureRegion.toTinted(spacing: TintedRegion.Spacing = TintedRegion.Spacing.ZERO): TintedRegion = TintedRegion(this, spacing)
        private fun TextureRegion.toTinted(initColor: Color, spacing: TintedRegion.Spacing = TintedRegion.Spacing.ZERO): TintedRegion =
            TintedRegion(this, initColor, spacing)

        private fun TextureRegion.toTinted(bindTo: ReadOnlyVar<Color>, spacing: TintedRegion.Spacing = TintedRegion.Spacing.ZERO): TintedRegion =
            TintedRegion(this, binding = { bindTo.use() }, spacing = spacing)

        private fun TextureRegion.toEditableTinted(spacing: TintedRegion.Spacing = TintedRegion.Spacing.ZERO): EditableTintedRegion = EditableTintedRegion(this, spacing)
        private fun TextureRegion.toEditableTinted(initColor: Color, spacing: TintedRegion.Spacing = TintedRegion.Spacing.ZERO): EditableTintedRegion =
            EditableTintedRegion(this, initColor, spacing)
        
        private fun mapRodTexture(tr: TextureRegion, count: Int, col: Int, color: ReadOnlyVar<Color>): List<TintedRegion> {
            val suggestedColumns = 2
            val suggestedRows = rodFrameCount
            val texture = tr.texture
            val regionWidthU = tr.u2 - tr.u
            val regionHeightV = tr.v2 - tr.v
            val partWidthU = regionWidthU / suggestedColumns
            val partHeightV = regionHeightV / suggestedRows
            return (0 until count).map { i ->
                val u = tr.u + col * partWidthU
                val v = tr.v + i * partHeightV
                TextureRegion(texture, u, v, u + partWidthU, v + partHeightV).toTinted(color)
            }
        }
    }

    /**
     * A white cube. Used as the platform for the rods.
     */
    val platform: TintedRegion = packedSheet["platform"].toTinted(TintedRegion.Spacing(1, 32, 32))

    /**
     * Like [platform] but with the red marking line on it.
     */
    val platformWithLine: TintedRegion = packedSheet["platform_with_line"].toTinted(spacing = TintedRegion.Spacing(1, 32, 32))

    /**
     * The red line to be drawn on top of [cubeBorder].
     */
    val redLine: TintedRegion = packedSheet["red_line"].toTinted(spacing = TintedRegion.Spacing(1, 32, 32))
    val cubeBorder: EditableTintedRegion = packedSheet["cube_border"].toEditableTinted(spacing = TintedRegion.Spacing(1, 32, 32))
    /**
     * Just the top -Z edge of [platform] is a black line.
     */
    val cubeBorderPlatform: TintedRegion = packedSheet["cube_border_platform"].toTinted(cubeBorder.color, spacing = TintedRegion.Spacing(1, 32, 32))
    val cubeBorderZ: EditableTintedRegion = packedSheet["cube_border_z"].toEditableTinted(spacing = TintedRegion.Spacing(1, 32, 32))
    val cubeFaceX: EditableTintedRegion = packedSheet["cube_face_x"].toEditableTinted(spacing = TintedRegion.Spacing(1, 32, 32))
    val cubeFaceY: EditableTintedRegion = packedSheet["cube_face_y"].toEditableTinted(spacing = TintedRegion.Spacing(1, 32, 32))
    val cubeFaceZ: EditableTintedRegion = packedSheet["cube_face_z"].toEditableTinted(spacing = TintedRegion.Spacing(1, 32, 32))
    
    val pistonFaceXColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val pistonFaceZColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    
    val pistonARetracted: TintedRegion = packedSheet["piston_a"].toTinted(spacing = TintedRegion.Spacing(1, 32, 40))
    val pistonAPartial: TintedRegion = packedSheet["piston_a_partial"].toTinted(spacing = TintedRegion.Spacing(1, 32, 40))
    val pistonAPartialFaceX: TintedRegion = packedSheet["piston_a_partial_face_x"].toTinted(pistonFaceXColor, spacing = TintedRegion.Spacing(1, 32, 40))
    val pistonAPartialFaceZ: TintedRegion = packedSheet["piston_a_partial_face_z"].toTinted(pistonFaceZColor, spacing = TintedRegion.Spacing(1, 32, 40))
    val pistonAExtended: TintedRegion = packedSheet["piston_a_extended"].toTinted(spacing = TintedRegion.Spacing(1, 32, 40))
    val pistonAExtendedFaceX: TintedRegion = packedSheet["piston_a_extended_face_x"].toTinted(pistonFaceXColor, spacing = TintedRegion.Spacing(1, 32, 40))
    val pistonAExtendedFaceZ: TintedRegion = packedSheet["piston_a_extended_face_z"].toTinted(pistonFaceZColor, spacing = TintedRegion.Spacing(1, 32, 40))
    
    val pistonDpadRetracted: TintedRegion = packedSheet["piston_dpad"].toTinted(spacing = TintedRegion.Spacing(1, 32, 40))
    val pistonDpadPartial: TintedRegion = packedSheet["piston_dpad_partial"].toTinted(spacing = TintedRegion.Spacing(1, 32, 40))
    val pistonDpadPartialFaceX: TintedRegion = packedSheet["piston_dpad_partial_face_x"].toTinted(pistonFaceXColor, spacing = TintedRegion.Spacing(1, 32, 40))
    val pistonDpadPartialFaceZ: TintedRegion = packedSheet["piston_dpad_partial_face_z"].toTinted(pistonFaceZColor, spacing = TintedRegion.Spacing(1, 32, 40))
    val pistonDpadExtended: TintedRegion = packedSheet["piston_dpad_extended"].toTinted(spacing = TintedRegion.Spacing(1, 32, 40))
    val pistonDpadExtendedFaceX: TintedRegion = packedSheet["piston_dpad_extended_face_x"].toTinted(pistonFaceXColor, spacing = TintedRegion.Spacing(1, 32, 40))
    val pistonDpadExtendedFaceZ: TintedRegion = packedSheet["piston_dpad_extended_face_z"].toTinted(pistonFaceZColor, spacing = TintedRegion.Spacing(1, 32, 40))
    
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
    private val rodBordersSection: TextureRegion = packedSheet["rods_borders"]
    private val rodFillSection: TextureRegion = packedSheet["rods_fill"]
    val rodGroundBorderAnimations: List<TintedRegion> = mapRodTexture(rodBordersSection, rodFrameCount, 0, rodBorderColor)
    val rodGroundFillAnimations: List<TintedRegion> = mapRodTexture(rodFillSection, rodFrameCount, 0, rodFillColor)
    val rodAerialBorderAnimations: List<TintedRegion> = mapRodTexture(rodBordersSection, rodFrameCount, 1, rodBorderColor)
    val rodAerialFillAnimations: List<TintedRegion> = mapRodTexture(rodFillSection, rodFrameCount, 1, rodFillColor)

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
    val dunkBasketFront: TintedRegion = packedSheet["basket_front"].toTinted()
    val dunkBasketFrontFaceZ: TintedRegion = packedSheet["basket_front_face_z"].toTinted()
    val dunkBasketRear: TintedRegion = packedSheet["basket_rear"].toTinted()
    val dunkBacking: TintedRegion = packedSheet["hoop_back"].toTinted()
}
