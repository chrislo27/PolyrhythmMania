package polyrhythmmania.world.texturepack

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import paintbox.packing.TextureRegionMap
import paintbox.util.gdxutils.disposeQuietly


/**
 * A texture pack represents a mapping of [TextureRegion]s by string ID.
 * 
 * It may represent an incomplete set of game textures. For example, an end-user may choose to only override certain
 * textures and use the game defaults as fallback. Fallback behaviour is supported with the [CascadingTexturePack] impl.
 * 
 * A texture pack does not handle the subregions in certain textures, for example the rod animations.
 */
abstract class TexturePack(val id: String, val deprecatedIDs: Set<String>) {
    
    companion object {
        const val rodFrameCount: Int = 6
        const val explosionFrameCount: Int = 4
    }
    
    protected val allRegions: MutableList<PackTexRegion> = mutableListOf()
    protected val internalMap: MutableMap<String, PackTexRegion> = mutableMapOf()
    
    fun add(region: PackTexRegion?) {
        if (region == null) return
        allRegions += region
        internalMap[region.id] = region
    }
    
    fun remove(region: PackTexRegion?) {
        if (region == null) return
        allRegions -= region
        internalMap.remove(region.id, region)
    }

    open fun getAllUniqueTextures(): List<Texture> = allRegions.mapNotNull { it.texture }.distinct()
    
    open fun getAllTilesetRegions(): List<PackTexRegion> = allRegions.toList()

    /**
     * Gets the specified region by its [id].
     */
    open operator fun get(id: String): PackTexRegion {
        return getOrNull(id) ?: error("TilesetRegion not found with ID '${id}'")
    }

    /**
     * Gets the specified region by its [id], or returns null if none is found.
     */
    open fun getOrNull(id: String): PackTexRegion? {
        return internalMap[id]
    }
    
}

class CascadingTexturePack(id: String, deprecatedIDs: Set<String>, val priorityList: List<TexturePack>,
                           val shouldThrowErrorOnMissing: Boolean = false)
    : TexturePack(id, deprecatedIDs), Disposable {
    
    override fun get(id: String): PackTexRegion {
        return getOrNull(id) ?: if (shouldThrowErrorOnMissing) {
            error("TilesetRegion not found with ID '${id}', searched in ${priorityList.size} pack(s) (${priorityList.joinToString(separator = ", ") { it.id }})")
        } else {
            StockTexturePacks.missingPackTexRegion
        }
    }

    override fun getOrNull(id: String): PackTexRegion? {
        return priorityList.firstNotNullOfOrNull { it.getOrNull(id) }
    }


    override fun getAllUniqueTextures(): List<Texture> = (super.getAllUniqueTextures() + priorityList.flatMap { it.getAllUniqueTextures() }).distinct()

    override fun getAllTilesetRegions(): List<PackTexRegion> {
        val list = mutableListOf<PackTexRegion>()
        val ids = mutableSetOf<String>()
        (listOf(super.getAllTilesetRegions()) + priorityList.map { it.getAllTilesetRegions() }).forEach { l ->
            l.forEach { region ->
                if (region.id !in ids) {
                    ids.add(region.id)
                    list.add(region)
                }
            }
        }
        return list
    }

    override fun dispose() {
        priorityList.forEach { (it as? Disposable)?.disposeQuietly() }
    }
}


/**
 * A [TexturePack] backed by a [TextureRegionMap].
 */
open class StockTexturePack(id: String, deprecatedIDs: Set<String>, val regionMap: TextureRegionMap)
    : TexturePack(id, deprecatedIDs) {
    
    init {
        // REMINDER: If a region is added that is user-editable, update CustomTexturePack's ALLOWED_LIST of IDs
        
        add(PackTexRegion.create("platform", regionMap.getOrNull("platform"), RegionSpacing(1, 32, 32)))

        add(PackTexRegion.create("platform_with_line", regionMap.getOrNull("platform_with_line"), RegionSpacing(1, 32, 32)))

        add(PackTexRegion.create("red_line", regionMap.getOrNull("red_line"), RegionSpacing(1, 32, 32)))
        add(PackTexRegion.create("cube_border", regionMap.getOrNull("cube_border"), RegionSpacing(1, 32, 32)))
        add(PackTexRegion.create("cube_border_platform", regionMap.getOrNull("cube_border_platform"), RegionSpacing(1, 32, 32)))
        add(PackTexRegion.create("cube_border_z", regionMap.getOrNull("cube_border_z"), RegionSpacing(1, 32, 32)))
        add(PackTexRegion.create("cube_face_x", regionMap.getOrNull("cube_face_x"), RegionSpacing(1, 32, 32)))
        add(PackTexRegion.create("cube_face_y", regionMap.getOrNull("cube_face_y"), RegionSpacing(1, 32, 32)))
        add(PackTexRegion.create("cube_face_z", regionMap.getOrNull("cube_face_z"), RegionSpacing(1, 32, 32)))

        add(PackTexRegion.create("piston_a", regionMap.getOrNull("piston_a"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("piston_a_partial", regionMap.getOrNull("piston_a_partial"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("piston_a_partial_face_x", regionMap.getOrNull("piston_a_partial_face_x"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("piston_a_partial_face_z", regionMap.getOrNull("piston_a_partial_face_z"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("piston_a_extended", regionMap.getOrNull("piston_a_extended"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("piston_a_extended_face_x", regionMap.getOrNull("piston_a_extended_face_x"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("piston_a_extended_face_z", regionMap.getOrNull("piston_a_extended_face_z"), RegionSpacing(1, 32, 40)))

        add(PackTexRegion.create("piston_dpad", regionMap.getOrNull("piston_dpad"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("piston_dpad_partial", regionMap.getOrNull("piston_dpad_partial"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("piston_dpad_partial_face_x", regionMap.getOrNull("piston_dpad_partial_face_x"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("piston_dpad_partial_face_z", regionMap.getOrNull("piston_dpad_partial_face_z"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("piston_dpad_extended", regionMap.getOrNull("piston_dpad_extended"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("piston_dpad_extended_face_x", regionMap.getOrNull("piston_dpad_extended_face_x"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("piston_dpad_extended_face_z", regionMap.getOrNull("piston_dpad_extended_face_z"), RegionSpacing(1, 32, 40)))

        add(PackTexRegion.create("indicator_a", regionMap.getOrNull("indicator_a")))
        add(PackTexRegion.create("indicator_dpad", regionMap.getOrNull("indicator_dpad")))

        add(PackTexRegion.create("sign_a", regionMap.getOrNull("sign_a")))
        add(PackTexRegion.create("sign_a_shadow", regionMap.getOrNull("sign_a_shadow")))
        add(PackTexRegion.create("sign_dpad", regionMap.getOrNull("sign_dpad")))
        add(PackTexRegion.create("sign_dpad_shadow", regionMap.getOrNull("sign_dpad_shadow")))
        add(PackTexRegion.create("sign_bo", regionMap.getOrNull("sign_bo")))
        add(PackTexRegion.create("sign_bo_shadow", regionMap.getOrNull("sign_bo_shadow")))
        add(PackTexRegion.create("sign_ta", regionMap.getOrNull("sign_ta")))
        add(PackTexRegion.create("sign_ta_shadow", regionMap.getOrNull("sign_ta_shadow")))
        add(PackTexRegion.create("sign_n", regionMap.getOrNull("sign_n")))
        add(PackTexRegion.create("sign_n_shadow", regionMap.getOrNull("sign_n_shadow")))

        add(PackTexRegion.create("rods_borders", regionMap.getOrNull("rods_borders")))
        add(PackTexRegion.create("rods_fill", regionMap.getOrNull("rods_fill")))

        val explosionRegions = regionMap.getIndexedRegions("explosion")
        (0 until explosionFrameCount).forEach { i ->
            add(PackTexRegion.create("explosion_${i}", explosionRegions.getValue(i)))
        }

        val inputFeedbackRegions = regionMap.getIndexedRegions("input_feedback")
        add(PackTexRegion.create("input_feedback_0", inputFeedbackRegions.getValue(0)))
        add(PackTexRegion.create("input_feedback_1", inputFeedbackRegions.getValue(1)))
        add(PackTexRegion.create("input_feedback_2", inputFeedbackRegions.getValue(2)))
        
        add(PackTexRegion.create("background_back", regionMap.getOrNull("background_back")))
        add(PackTexRegion.create("background_middle", regionMap.getOrNull("background_middle")))
        add(PackTexRegion.create("background_fore", regionMap.getOrNull("background_fore")))
        
        
        // NON-EDITABLE
        add(PackTexRegion.create("defective_rods_borders", regionMap.getOrNull("defective_rods_borders")))
        add(PackTexRegion.create("defective_rods_fill", regionMap.getOrNull("defective_rods_fill")))

        // DUNK
        add(PackTexRegion.create("basket_back", regionMap.getOrNull("basket_back")))
        add(PackTexRegion.create("basket_front", regionMap.getOrNull("basket_front")))
        add(PackTexRegion.create("basket_front_face_z", regionMap.getOrNull("basket_front_face_z")))
        add(PackTexRegion.create("basket_rear", regionMap.getOrNull("basket_rear")))
        add(PackTexRegion.create("hoop_back", regionMap.getOrNull("hoop_back")))
        
        // ASSEMBLE
        add(PackTexRegion.create("asm_lane", regionMap.getOrNull("asm_lane")))
        add(PackTexRegion.create("asm_centre_perp", regionMap.getOrNull("asm_centre_perp")))
        add(PackTexRegion.create("asm_centre_perp_target", regionMap.getOrNull("asm_centre_perp_target")))
        add(PackTexRegion.create("asm_cube", regionMap.getOrNull("asm_cube")))
        add(PackTexRegion.create("asm_piston_a", regionMap.getOrNull("asm_piston_a"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("asm_piston_a_extended", regionMap.getOrNull("asm_piston_a_extended"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("asm_piston_a_partial", regionMap.getOrNull("asm_piston_a_partial"), RegionSpacing(1, 32, 40)))
        add(PackTexRegion.create("asm_widget_complete", regionMap.getOrNull("asm_widget_complete")))
        add(PackTexRegion.create("asm_widget_complete_blur", regionMap.getOrNull("asm_widget_complete_blur")))
        add(PackTexRegion.create("asm_widget_roll", regionMap.getOrNull("asm_widget_roll")))
        add(PackTexRegion.create("asm_widget_roll_test", regionMap.getOrNull("asm_widget_roll_test")))
    }
    
}

