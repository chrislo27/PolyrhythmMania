package paintbox.packing

import com.badlogic.gdx.graphics.g2d.TextureRegion


/**
 * Represents a data structure that holds string-indexed [TextureRegion]s.
 */
interface TextureRegionMap {

    /**
     * Gets the specified region by its [id].
     */
    operator fun get(id: String): TextureRegion

    /**
     * Gets the specified region by its [id], or returns null if none is found.
     */
    fun getOrNull(id: String): TextureRegion?

    /**
     * Returns a map of regions by their number.
     * A region is considered indexed if its ID is in the format id_X, where X is a number.
     */
    fun getIndexedRegions(id: String): Map<Int, TextureRegion>
    
    /**
     * Returns a map of regions by their number.
     * A region is considered indexed if its ID is in the format id_X, where X is a number.
     */
    fun getIndexedRegionsOrNull(id: String): Map<Int, TextureRegion>?
    
}