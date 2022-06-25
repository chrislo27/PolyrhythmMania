package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.world.World
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset


class EntityBackgroundImg(world: World, val layer: Layer)
    : SimpleRenderedEntity(world) {
    
    enum class Layer {
        BACK, MIDDLE, FORE
    }

    override val renderHeight: Float = 5f
    override val renderWidth: Float get() = renderHeight * (16f / 9)

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        val tintedRegion = when (this.layer) {
            Layer.BACK -> tileset.backgroundBack
            Layer.MIDDLE -> tileset.backgroundMiddle
            Layer.FORE -> tileset.backgroundFore
        }
        val camera = renderer.camera
        val camW = camera.viewportWidth
        val camH = camera.viewportHeight
        
        batch.color = tintedRegion.color.getOrCompute()
        batch.draw(tileset.getTilesetRegionForTinted(tintedRegion),
                camera.position.x - camW / 2, camera.position.y - camH / 2, camW, camH)
        batch.setColor(1f, 1f, 1f, 1f)
    }

}