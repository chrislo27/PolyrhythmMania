package polyrhythmmania.storymode.test.gamemode

import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.BlockDespawnPattern
import polyrhythmmania.editor.block.BlockSpawnPattern
import polyrhythmmania.editor.block.CubeType
import polyrhythmmania.editor.block.storymode.Block8BallCamera
import polyrhythmmania.world.World
import polyrhythmmania.world.WorldMode
import polyrhythmmania.world.WorldType


class TestStory8BallGameMode(main: PRManiaGame) : TestStoryGameMode(main) {
    
    init {
//        container.renderer.camera.zoom = 2f
        
        container.world.worldMode = WorldMode(WorldType.Polyrhythm())
        container.world.worldResetListeners += World.WorldResetListener { world ->
//            world.addEntity(EntityCameraFrame(world, Color.RED).apply {
//                this.position.y = 3f
//            })
//            world.addEntity(EntityCameraFrame(world, Color.BLUE).apply {
//                this.position.y = 3f
//                this.position.x += 24f
//            })
//            world.addEntity(EntityCameraFrame(world, Color.BLACK).apply {
//                this.position.y = 3f
//                this.position.x += 12f
//            })
//            world.addEntity(EntityCameraFrame(world, Color.ORANGE, lockToCamera = true))
        }
    }

    override fun initialize() {
        super.initialize()
        
        container.blocks.filterIsInstance<BlockDespawnPattern>().forEach { block ->
            block.disableTailEnd.set(true)
        }
        container.addBlock(BlockSpawnPattern(container.engine).apply {
            this.beat = 8f
            this.patternData.rowDpadTypes[8] = CubeType.PLATFORM
            this.patternData.rowDpadTypes[9] = CubeType.PLATFORM
        })

        container.blocks.filterIsInstance<BlockDespawnPattern>().forEach { block ->
            val width = 3f
            // Event must end, at latest, one beat AFTER despawn block starts 
            val b = block.beat + 1 - width
            
            val newBlock = Block8BallCamera(container.engine).apply {
                this.beat = b
            }
            
            container.addBlock(newBlock)
        }
    }
}