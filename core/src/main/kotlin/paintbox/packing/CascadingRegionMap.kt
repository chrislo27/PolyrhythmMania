package paintbox.packing

import com.badlogic.gdx.graphics.g2d.TextureRegion


class CascadingRegionMap(val suppliers: List<TextureRegionMap>)
    : TextureRegionMap {
    
    override fun get(id: String): TextureRegion {
        return getOrNull(id) ?: error("No region found with ID $id in any of the suppliers")
    }

    override fun getOrNull(id: String): TextureRegion? {
        return suppliers.firstNotNullOfOrNull { it.getOrNull(id) }
    }

    override fun getIndexedRegions(id: String): Map<Int, TextureRegion> {
        return getIndexedRegionsOrNull(id) ?: error("No indexed map of regions found with ID $id in any of the suppliers")
    }

    override fun getIndexedRegionsOrNull(id: String): Map<Int, TextureRegion>? {
        return suppliers.firstNotNullOfOrNull { it.getIndexedRegionsOrNull(id) }
    }
}