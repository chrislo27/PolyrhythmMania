package polyrhythmmania.world.render.bg

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.World


abstract class WorldBackground {
    
    abstract fun render(batch: SpriteBatch, world: World, engine: Engine, camera: OrthographicCamera)
    
}

object NoOpWorldBackground : WorldBackground() {
    override fun render(batch: SpriteBatch, world: World, engine: Engine, camera: OrthographicCamera) {
        // NO-OP
    }
}

object WorldBackgroundFromWorldType : WorldBackground() {
    override fun render(batch: SpriteBatch, world: World, engine: Engine, camera: OrthographicCamera) {
        world.worldMode.worldType.defaultBackground.render(batch, world, engine, camera)
    }
}
