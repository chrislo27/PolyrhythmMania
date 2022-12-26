package polyrhythmmania.editor.block.storymode

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.container.Container
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import java.util.*


class Block8BallCamera(engine: Engine) : Block(engine, Block8BallCamera.BLOCK_TYPES) {
    
    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.allOf(BlockType::class.java)
    }
    
    init {
        this.width = 1f
        this.defaultText.set("Camera\nPan")
    }
    
    override fun compileIntoEvents(): List<Event> {
        val width = 3f
        return listOf(
                Event8BallCameraPan(engine, this.beat + 1 - width, width)
        )
    }

    override fun copy(): Block8BallCamera {
        return Block8BallCamera(engine).also { 
            this.copyBaseInfoTo(it)
        }
    }
}

class Event8BallCameraPan(engine: Engine, startBeat: Float, width: Float) : Event(engine) {

    private lateinit var camera: OrthographicCamera
    private val originalCameraPos: Vector3 = Vector3()
    
    // These don't use Vector3Stack due to threading issues with the ResourceStack
    private val tmpVec1: Vector3 = Vector3()
    private val tmpVec2: Vector3 = Vector3()
    
    init {
        this.beat = startBeat
        this.width = width
    }

    override fun onStartContainer(container: Container, currentBeat: Float) {
        super.onStartContainer(container, currentBeat)
        
        this.camera = container.renderer.camera
        
        if (!container.globalSettings.reducedMotion) {
            this.originalCameraPos.set(Vector3(camera.viewportWidth / 2, camera.viewportHeight / 2, camera.position.z))
        }
    }

    override fun onUpdateContainer(container: Container, currentBeat: Float) {
        super.onUpdateContainer(container, currentBeat)

        if (!container.globalSettings.reducedMotion) {
            val progress = ((currentBeat - this.beat) / width.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
            val interpolated = Interpolation.smooth2.apply(progress)

            val targetPosVec = tmpVec1
            val dist = 24f
            targetPosVec.set(originalCameraPos)
            targetPosVec.x += dist / 2
            targetPosVec.y += dist / 4

            val tmpVecPos = tmpVec2.set(originalCameraPos)
                    .lerp(targetPosVec, interpolated)
            camera.position.set(tmpVecPos)
        }
    }

    override fun onEndContainer(container: Container, currentBeat: Float) {
        super.onEndContainer(container, currentBeat)

        if (!container.globalSettings.reducedMotion) {
            camera.position.set(originalCameraPos)
        }
    }
}
