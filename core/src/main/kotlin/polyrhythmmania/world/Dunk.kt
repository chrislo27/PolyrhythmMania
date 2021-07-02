package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.TintedRegion
import polyrhythmmania.world.render.WorldRenderer


class EntityDunkBasketBack(world: World) : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return tileset.dunkBasketBack
    }
}
class EntityDunkBacking(world: World) : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return tileset.dunkBacking
    }
}

class EntityDunkBasketFaceZ(world: World) : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return tileset.dunkBasketFaceZ
    }

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine, vec: Vector3) {
        for (i in 0 until numLayers) {
            val tr = getTintedRegion(tileset, i)
            drawTintedRegion(batch, vec, tr, 0f, 0f, renderWidth, renderHeight)
        }
    }
}

class EntityDunkBasketFaceX(world: World) : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return tileset.dunkBasketFaceX
    }
    
    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine, vec: Vector3) {
        for (i in 0 until numLayers) {
            val tr = getTintedRegion(tileset, i)
            drawTintedRegion(batch, vec, tr, 0.5f, 0.25f, renderWidth, renderHeight)
        }
    }
}