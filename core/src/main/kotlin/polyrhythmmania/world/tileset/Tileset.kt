package polyrhythmmania.world.tileset

import com.badlogic.gdx.graphics.Color
import paintbox.binding.Var


/**
 * A [Tileset] is a container that combines texture region info and current colour info.
 * 
 * Texture region info is handled in [TexturePack]. The [TexturePack] can be changed for a [Tileset] at any time.
 * 
 * Colours presets are handled in containers called [TilesetPalette]s.
 * A [TilesetPalette] can be applied to a [Tileset].
 */
class Tileset(val texturePack: Var<TexturePack>) {

    companion object {
        private fun mapRodTexture(parent: TintedRegion, count: Int, col: Int): List<TintedSubregion> {
            val suggestedColumns = 2
            val suggestedRows = TexturePack.rodFrameCount
            val partWidthU = 1f / suggestedColumns
            val partHeightV = 1f / suggestedRows
            return (0 until count).map { row ->
                TintedSubregion(parent, col * partWidthU, row * partHeightV, partWidthU, partHeightV)
            }
        }
    }
    
    constructor(initialPack: TexturePack) : this(Var(initialPack))
    
    fun getTilesetRegionForTinted(tinted: TintedRegion): TilesetRegion {
        return texturePack.getOrCompute()[tinted.regionID]
    }

    /**
     * A white cube. Used as the platform for the rods.
     */
    val platform: TintedRegion = TintedRegion("platform")

    /**
     * Like [platform] but with the red marking line on it.
     */
    val platformWithLine: TintedRegion = TintedRegion("platform_with_line")

    /**
     * The red line to be drawn on top of [cubeBorder].
     */
    val redLine: TintedRegion = TintedRegion("red_line")
    val cubeBorder: EditableTintedRegion = EditableTintedRegion("cube_border")
    /**
     * Just the top -Z edge of [platform] is a black line.
     */
    val cubeBorderPlatform: TintedRegion = TintedRegion("cube_border_platform", cubeBorder.color)
    val cubeBorderZ: EditableTintedRegion = EditableTintedRegion("cube_border_z")
    val cubeFaceX: EditableTintedRegion = EditableTintedRegion("cube_face_x")
    val cubeFaceY: EditableTintedRegion = EditableTintedRegion("cube_face_y")
    val cubeFaceZ: EditableTintedRegion = EditableTintedRegion("cube_face_z")
    
    val pistonFaceXColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val pistonFaceZColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    
    val pistonARetracted: TintedRegion = TintedRegion("piston_a")
    val pistonAPartial: TintedRegion = TintedRegion("piston_a_partial")
    val pistonAPartialFaceX: TintedRegion = TintedRegion("piston_a_partial_face_x", pistonFaceXColor)
    val pistonAPartialFaceZ: TintedRegion = TintedRegion("piston_a_partial_face_z", pistonFaceZColor)
    val pistonAExtended: TintedRegion = TintedRegion("piston_a_extended")
    val pistonAExtendedFaceX: TintedRegion = TintedRegion("piston_a_extended_face_x", pistonFaceXColor)
    val pistonAExtendedFaceZ: TintedRegion = TintedRegion("piston_a_extended_face_z", pistonFaceZColor)
    
    val pistonDpadRetracted: TintedRegion = TintedRegion("piston_dpad")
    val pistonDpadPartial: TintedRegion = TintedRegion("piston_dpad_partial")
    val pistonDpadPartialFaceX: TintedRegion = TintedRegion("piston_dpad_partial_face_x", pistonFaceXColor)
    val pistonDpadPartialFaceZ: TintedRegion = TintedRegion("piston_dpad_partial_face_z", pistonFaceZColor)
    val pistonDpadExtended: TintedRegion = TintedRegion("piston_dpad_extended")
    val pistonDpadExtendedFaceX: TintedRegion = TintedRegion("piston_dpad_extended_face_x", pistonFaceXColor)
    val pistonDpadExtendedFaceZ: TintedRegion = TintedRegion("piston_dpad_extended_face_z", pistonFaceZColor)
    
    val indicatorA: TintedRegion = TintedRegion("indicator_a")
    val indicatorDpad: TintedRegion = TintedRegion("indicator_dpad")
    
    val signShadowColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val signA: TintedRegion = TintedRegion("sign_a")
    val signAShadow: TintedRegion = TintedRegion("sign_a_shadow", signShadowColor)
    val signDpad: TintedRegion = TintedRegion("sign_dpad")
    val signDpadShadow: TintedRegion = TintedRegion("sign_dpad_shadow", signShadowColor)
    val signBo: TintedRegion = TintedRegion("sign_bo")
    val signBoShadow: TintedRegion = TintedRegion("sign_bo_shadow", signShadowColor)
    val signTa: TintedRegion = TintedRegion("sign_ta")
    val signTaShadow: TintedRegion = TintedRegion("sign_ta_shadow", signShadowColor)
    val signN: TintedRegion = TintedRegion("sign_n")
    val signNShadow: TintedRegion = TintedRegion("sign_n_shadow", signShadowColor)

    val rodBorderColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val rodFillColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    private val rodBordersSection: TintedRegion = TintedRegion("rods_borders", rodBorderColor)
    private val rodFillSection: TintedRegion = TintedRegion("rods_fill", rodFillColor)
    val rodGroundBorderAnimations: List<TintedRegion> = mapRodTexture(rodBordersSection, TexturePack.rodFrameCount, 0)
    val rodGroundFillAnimations: List<TintedRegion> = mapRodTexture(rodFillSection, TexturePack.rodFrameCount, 0)
    val rodAerialBorderAnimations: List<TintedRegion> = mapRodTexture(rodBordersSection, TexturePack.rodFrameCount, 1)
    val rodAerialFillAnimations: List<TintedRegion> = mapRodTexture(rodFillSection, TexturePack.rodFrameCount, 1)

    val explosionFrames: List<TintedRegion> by lazy {
        (0 until TexturePack.explosionFrameCount).map { i ->
            TintedRegion("explosion_${i}")
        }
    }
    
    val inputFeedbackStart: TintedRegion = TintedRegion("input_feedback_0")
    val inputFeedbackMiddle: TintedRegion = TintedRegion("input_feedback_1")
    val inputFeedbackEnd: TintedRegion = TintedRegion("input_feedback_2")
    
    val backgroundBack: TintedRegion = TintedRegion("background_back")
    val backgroundMiddle: TintedRegion = TintedRegion("background_middle")
    val backgroundFore: TintedRegion = TintedRegion("background_fore")
    
    val dunkBasketBack: TintedRegion = TintedRegion("basket_back")
    val dunkBasketFront: TintedRegion = TintedRegion("basket_front")
    val dunkBasketFrontFaceZ: TintedRegion = TintedRegion("basket_front_face_z")
    val dunkBasketRear: TintedRegion = TintedRegion("basket_rear")
    val dunkBacking: TintedRegion = TintedRegion("hoop_back")
}
