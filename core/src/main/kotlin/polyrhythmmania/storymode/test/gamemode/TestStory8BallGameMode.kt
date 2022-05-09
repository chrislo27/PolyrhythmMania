package polyrhythmmania.storymode.test.gamemode

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector3
import paintbox.util.Vector3Stack
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.*
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EndlessType
import polyrhythmmania.world.World
import polyrhythmmania.world.WorldMode
import polyrhythmmania.world.WorldType
import polyrhythmmania.world.entity.EntityCameraFrame
import java.util.*


class TestStory8BallGameMode(main: PRManiaGame) : TestStoryGameMode(main) {
    
    init {
//        container.renderer.camera.zoom = 2f
        
        container.world.worldMode = WorldMode(WorldType.Polyrhythm(true), EndlessType.NOT_ENDLESS)
        container.world.worldResetListener = World.WorldResetListener { world ->
//            world.addEntity(EntityCameraFrame(world, Color.RED).apply {
//                this.position.y = 3f
//            })
//            world.addEntity(EntityCameraFrame(world, Color.BLUE).apply {
//                this.position.y = 3f 
//                this.position.x += 24f
//            })
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
            val b = block.beat - 2f
            
            val newBlock = object : Block(container.engine, EnumSet.allOf(BlockType::class.java)) {
                override fun compileIntoEvents(): List<Event> {
                    return listOf(
                            object : Event(container.engine) {
                                init {
                                    this.beat = b
                                    this.width = 3f
                                }
                                
                                val camera = container.renderer.camera
                                val originalCameraPos = Vector3(camera.viewportWidth / 2, camera.viewportHeight / 2, camera.position.z)

                                override fun onStart(currentBeat: Float) {
                                    container.renderer.camera.position.set(originalCameraPos)
                                }

                                override fun onUpdate(currentBeat: Float) {
                                    val progress = ((currentBeat - this.beat) / width.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
                                    val interpolated = Interpolation.smooth2.apply(progress)
                                    
                                    val targetPosVec = Vector3Stack.getAndPush()
                                    val dist = 24f
                                    targetPosVec.set(originalCameraPos)
                                    targetPosVec.x += dist / 2
                                    targetPosVec.y += dist / 4

                                    val tmpVec = Vector3Stack.getAndPush()
                                            .set(originalCameraPos)
                                            .lerp(targetPosVec, interpolated)
                                    container.renderer.camera.position.set(tmpVec)
                                    
                                    Vector3Stack.pop()
                                    Vector3Stack.pop()
                                }

                                override fun onEnd(currentBeat: Float) {
                                    container.renderer.camera.position.set(originalCameraPos)
                                }
                            }
                    )
                }

                override fun copy(): Block {
                    throw NotImplementedError()
                }
            }
            
            container.addBlock(newBlock)
        }
    }
}