package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.TextureRegion
import polyrhythmmania.world.render.Tileset


class EntityPlatform(world: World, val withLine: Boolean = false) : SimpleRenderedEntity(world) {
    override fun getTextureRegionFromTileset(tileset: Tileset): TextureRegion {
        return if (withLine) tileset.platformWithLine else tileset.platform
    }
}

class EntityCube(world: World, val withLine: Boolean = false) : SimpleRenderedEntity(world) {
    override fun getTextureRegionFromTileset(tileset: Tileset): TextureRegion {
        return if (withLine) tileset.cubeWithLine else tileset.cube
    }
}