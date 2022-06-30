package polyrhythmmania.world.texturepack

import com.badlogic.gdx.graphics.g2d.TextureRegion


/**
 * A [PackTexRegion] is a [TextureRegion] with other metadata for [TexturePack].
 *
 */
class PackTexRegion(val id: String, region: TextureRegion, val spacing: RegionSpacing)
    : TextureRegion(region) {

    companion object {
        fun create(id: String, region: TextureRegion?, spacing: RegionSpacing = RegionSpacing.ZERO): PackTexRegion? {
            if (region == null) return null
            return PackTexRegion(id, region, spacing)
        }
    }

}
