package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.g2d.Batch
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset


/**
 * Implemented by an [Entity] to indicate it has a lighting pass.
 * 
 * Note that entities with this interface are not render culled for the lighting pass.
 */
interface HasLightingRender {

    /**
     * The render for when emitting light.
     */
    fun renderLightingEffect(renderer: WorldRenderer, batch: Batch, tileset: Tileset)


    /**
     * The render for when blocking light before calling [renderLightingEffect].
     * If this is an [Entity], the default implementation uses [Entity.render].
     */
    fun renderBlockingEffectBeforeLighting(renderer: WorldRenderer, batch: Batch, tileset: Tileset) {
        if (this is Entity) {
            this.render(renderer, batch, tileset)
        }
    }
    
    /**
     * The render for when blocking light, after [renderLightingEffect] has been called.
     * The default implementation is to do nothing (you likely do not want to block the light you just drew).
     */
    fun renderBlockingEffectAfterLighting(renderer: WorldRenderer, batch: Batch, tileset: Tileset) {
        // Default implementation is NO-OP
    }
    
    
}
