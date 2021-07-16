package polyrhythmmania.world.render.bg

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import polyrhythmmania.engine.Engine


abstract class WorldBackground {
    
    abstract fun render(batch: SpriteBatch, engine: Engine, camera: OrthographicCamera)
    
}

object NoOpWorldBackground : WorldBackground() {
    override fun render(batch: SpriteBatch, engine: Engine, camera: OrthographicCamera) {
        // NO-OP
    }
}