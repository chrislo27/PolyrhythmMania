package polyrhythmmania.storymode.test.gamemode

import com.badlogic.gdx.graphics.Color
import polyrhythmmania.PRManiaGame
import polyrhythmmania.world.EndlessType
import polyrhythmmania.world.World
import polyrhythmmania.world.WorldMode
import polyrhythmmania.world.WorldType
import polyrhythmmania.world.entity.EntityCameraFrame


class TestStory8BallGameMode(main: PRManiaGame) : TestStoryGameMode(main) {
    
    init {
        container.renderer.camera.zoom = 2f
        
        container.world.worldMode = WorldMode(WorldType.Polyrhythm(true), EndlessType.NOT_ENDLESS)
        container.world.worldResetListener = World.WorldResetListener { world ->
            world.addEntity(EntityCameraFrame(world, Color.RED))
        }
    }
    
}