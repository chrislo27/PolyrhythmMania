package polyrhythmmania.world.tileset

import com.badlogic.gdx.graphics.Color
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import polyrhythmmania.world.texturepack.PackTexRegion
import polyrhythmmania.world.texturepack.TexturePack


/**
 * A [Tileset] is a container that combines texture region info and current colour info.
 * 
 * Texture region info is handled in [TexturePack]. The [TexturePack] can be changed for a [Tileset] at any time.
 * 
 * Colours presets are handled in containers called [TilesetPalette]s.
 * A [TilesetPalette] can be applied to a [Tileset].
 */
class Tileset(val texturePack: ReadOnlyVar<TexturePack>) {

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
    
    fun getTilesetRegionForTinted(tinted: TintedRegion): PackTexRegion {
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
    
//    val pistonFaceXColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
//    val pistonFaceZColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))

    val pistonAFaceXColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val pistonAFaceZColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val pistonARetracted: TintedRegion = TintedRegion("piston_a")
    val pistonAPartial: TintedRegion = TintedRegion("piston_a_partial")
    val pistonAPartialFaceX: TintedRegion = TintedRegion("piston_a_partial_face_x", pistonAFaceXColor)
    val pistonAPartialFaceZ: TintedRegion = TintedRegion("piston_a_partial_face_z", pistonAFaceZColor)
    val pistonAExtended: TintedRegion = TintedRegion("piston_a_extended")
    val pistonAExtendedFaceX: TintedRegion = TintedRegion("piston_a_extended_face_x", pistonAFaceXColor)
    val pistonAExtendedFaceZ: TintedRegion = TintedRegion("piston_a_extended_face_z", pistonAFaceZColor)

    val pistonDpadFaceXColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val pistonDpadFaceZColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val pistonDpadRetracted: TintedRegion = TintedRegion("piston_dpad")
    val pistonDpadPartial: TintedRegion = TintedRegion("piston_dpad_partial")
    val pistonDpadPartialFaceX: TintedRegion = TintedRegion("piston_dpad_partial_face_x", pistonDpadFaceXColor)
    val pistonDpadPartialFaceZ: TintedRegion = TintedRegion("piston_dpad_partial_face_z", pistonDpadFaceZColor)
    val pistonDpadExtended: TintedRegion = TintedRegion("piston_dpad_extended")
    val pistonDpadExtendedFaceX: TintedRegion = TintedRegion("piston_dpad_extended_face_x", pistonDpadFaceXColor)
    val pistonDpadExtendedFaceZ: TintedRegion = TintedRegion("piston_dpad_extended_face_z", pistonDpadFaceZColor)
    
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

    val rodABorderColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val rodAFillColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val rodDpadBorderColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val rodDpadFillColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    private val rodABordersSection: TintedRegion = TintedRegion("rods_borders", rodABorderColor)
    private val rodAFillSection: TintedRegion = TintedRegion("rods_fill", rodAFillColor)
    private val rodDpadBordersSection: TintedRegion = TintedRegion("rods_borders", rodDpadBorderColor)
    private val rodDpadFillSection: TintedRegion = TintedRegion("rods_fill", rodDpadFillColor)
    val rodAGroundBorderAnimations: List<TintedRegion> = mapRodTexture(rodABordersSection, TexturePack.rodFrameCount, 0)
    val rodAGroundFillAnimations: List<TintedRegion> = mapRodTexture(rodAFillSection, TexturePack.rodFrameCount, 0)
    val rodAAerialBorderAnimations: List<TintedRegion> = mapRodTexture(rodABordersSection, TexturePack.rodFrameCount, 1)
    val rodAAerialFillAnimations: List<TintedRegion> = mapRodTexture(rodAFillSection, TexturePack.rodFrameCount, 1)
    val rodDpadGroundBorderAnimations: List<TintedRegion> = mapRodTexture(rodDpadBordersSection, TexturePack.rodFrameCount, 0)
    val rodDpadGroundFillAnimations: List<TintedRegion> = mapRodTexture(rodDpadFillSection, TexturePack.rodFrameCount, 0)
    val rodDpadAerialBorderAnimations: List<TintedRegion> = mapRodTexture(rodDpadBordersSection, TexturePack.rodFrameCount, 1)
    val rodDpadAerialFillAnimations: List<TintedRegion> = mapRodTexture(rodDpadFillSection, TexturePack.rodFrameCount, 1)
    
    private val defectiveRodABordersSection: TintedRegion = TintedRegion("defective_rods_borders", rodABorderColor)
    private val defectiveRodAFillSection: TintedRegion = TintedRegion("defective_rods_fill", rodAFillColor)
    private val defectiveRodDpadBordersSection: TintedRegion = TintedRegion("defective_rods_borders", rodDpadBorderColor)
    private val defectiveRodDpadFillSection: TintedRegion = TintedRegion("defective_rods_fill", rodDpadFillColor)
    val defectiveRodAGroundBorderAnimations: List<TintedRegion> = mapRodTexture(defectiveRodABordersSection, TexturePack.rodFrameCount, 0)
    val defectiveRodAGroundFillAnimations: List<TintedRegion> = mapRodTexture(defectiveRodAFillSection, TexturePack.rodFrameCount, 0)
    val defectiveRodAAerialBorderAnimations: List<TintedRegion> = mapRodTexture(defectiveRodABordersSection, TexturePack.rodFrameCount, 1)
    val defectiveRodAAerialFillAnimations: List<TintedRegion> = mapRodTexture(defectiveRodAFillSection, TexturePack.rodFrameCount, 1)
    val defectiveRodDpadGroundBorderAnimations: List<TintedRegion> = mapRodTexture(defectiveRodDpadBordersSection, TexturePack.rodFrameCount, 0)
    val defectiveRodDpadGroundFillAnimations: List<TintedRegion> = mapRodTexture(defectiveRodDpadFillSection, TexturePack.rodFrameCount, 0)
    val defectiveRodDpadAerialBorderAnimations: List<TintedRegion> = mapRodTexture(defectiveRodDpadBordersSection, TexturePack.rodFrameCount, 1)
    val defectiveRodDpadAerialFillAnimations: List<TintedRegion> = mapRodTexture(defectiveRodDpadFillSection, TexturePack.rodFrameCount, 1)

    val explosionFrames: List<TintedRegion> by lazy {
        (0 until TexturePack.explosionFrameCount).map { i ->
            TintedRegion("explosion_${i}")
        }
    }
    
    val inputFeedbackStart: TintedRegion = TintedRegion("input_feedback_0")
    val inputFeedbackMiddle: TintedRegion = TintedRegion("input_feedback_1")
    val inputFeedbackEnd: TintedRegion = TintedRegion("input_feedback_2")
    
    val backgroundBack: EditableTintedRegion = EditableTintedRegion("background_back", Color(1f, 1f, 1f, 1f))
    val backgroundMiddle: EditableTintedRegion = EditableTintedRegion("background_middle", Color(1f, 1f, 1f, 1f))
    val backgroundFore: EditableTintedRegion = EditableTintedRegion("background_fore", Color(1f, 1f, 1f, 1f))
    
    // DUNK
    val dunkBasketBack: TintedRegion = TintedRegion("basket_back")
    val dunkBasketFront: TintedRegion = TintedRegion("basket_front")
    val dunkBasketFrontFaceZ: TintedRegion = TintedRegion("basket_front_face_z")
    val dunkBasketRear: TintedRegion = TintedRegion("basket_rear")
    val dunkBacking: TintedRegion = TintedRegion("hoop_back")
    val dunkStar: TintedRegion = TintedRegion("dunk_star")
    val dunkStarAnimation: List<TintedRegion> = (0 until 4).map {
        // 39x9, 9x9 each with 1 px spacing on the x axis
        val w = 9f / 39f
        val wWithPad = 10f / 39f
        TintedSubregion(dunkStar, wWithPad * it, 0f, w, 1f)
    }
    
    // ASSEMBLE
    val asmLaneBorder: TintedRegion = TintedRegion("asm_lane_border", pistonDpadFaceXColor) // Uses color mapping for pistonDpadFaceX
    val asmLaneTop: TintedRegion = TintedRegion("asm_lane_top", pistonDpadFaceZColor) // Uses color mapping for pistonDpadFaceZ
    val asmLaneSides: TintedRegion = TintedRegion("asm_lane_sides", cubeFaceZ.color) // Uses color mapping for cubeFaceZ
    val asmCentrePerp: TintedRegion = TintedRegion("asm_centre_perp")
    val asmCentrePerpTarget: TintedRegion = TintedRegion("asm_centre_perp_target")
    val asmCubeFaceY: TintedRegion = TintedRegion("asm_cube_face_y", cubeFaceY.color)
    val asmPistonA: TintedRegion = TintedRegion("asm_piston_a")
    val asmPistonAExtended: TintedRegion = TintedRegion("asm_piston_a_extended")
    val asmPistonAPartial: TintedRegion = TintedRegion("asm_piston_a_partial")
    val asmWidgetComplete: TintedRegion = TintedRegion("asm_widget_complete")
    val asmWidgetCompleteBlur: TintedRegion = TintedRegion("asm_widget_complete_blur")
    val asmWidgetRoll: TintedRegion = TintedRegion("asm_widget_roll")
    
}
