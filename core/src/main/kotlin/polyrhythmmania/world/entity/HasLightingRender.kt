package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset


/**
 * Implemented by an [Entity] to indicate it has a lighting pass.
 * 
 * Note that entities with this interface are not render culled for the lighting pass.
 */
interface HasLightingRender {
    fun renderLightingPass(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset)
}
