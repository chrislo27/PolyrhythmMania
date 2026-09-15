package polyrhythmmania.world.render.bg

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import polyrhythmmania.world.World


abstract class WorldBackground {
    
    abstract fun render(batch: Batch, world: World, camera: OrthographicCamera)
    
}

object NoOpWorldBackground : WorldBackground() {
    override fun render(batch: Batch, world: World, camera: OrthographicCamera) {
        // NO-OP
    }
}

object WorldBackgroundFromWorldType : WorldBackground() {
    override fun render(batch: Batch, world: World, camera: OrthographicCamera) {
        world.worldMode.worldType.defaultBackground.render(batch, world, camera)
    }
}
