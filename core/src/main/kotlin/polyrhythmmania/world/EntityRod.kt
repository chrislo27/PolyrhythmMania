package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.WorldRenderer
import kotlin.math.floor

class EntityRod(world: World, val deployBeat: Float, val row: Row) : Entity(world) {

    companion object {
        private val tmpVec = Vector3()
    }

    var isInAir: Boolean = false
    var collidedWithWall: Boolean = false
        private set
    var collidedWithFloor: Boolean = false
    private var initializedActiveBlocks = false
    private val activeBlocks: BooleanArray = BooleanArray(row.length)
    private val xUnitsPerBeat: Float = 2f
    private val killAfterBeats: Float = 4f + row.length / xUnitsPerBeat + 1 // 4 prior to first index 0 + rowLength/xUnitsPerBeat + 1 buffer
    private var explodeAtSec: Float = Float.MAX_VALUE

    init {
        this.position.z = row.startZ.toFloat()
        this.position.y = row.startY.toFloat() + 1f
    }

    override fun getRenderWidth(): Float = 0.75f
    override fun getRenderHeight(): Float = 0.5f

    fun getPosXFromBeat(beatsFromDeploy: Float): Float {
        return (row.startX + 0.5f - 4 * xUnitsPerBeat) + (beatsFromDeploy) * xUnitsPerBeat - (6 / 32f)
    }

    fun explode(engine: Engine) {
        if (isKilled) return
        kill()
        world.addEntity(EntityExplosion(world, engine.seconds, this.getRenderWidth()).also {
            it.position.set(this.position)
        })
    }

    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine) {
        val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(this.position))

        val beatsFullAnimation = 60f / 128f
        val animationAlpha = (((this.position.x / xUnitsPerBeat) % beatsFullAnimation) / beatsFullAnimation).coerceIn(0f, 1f) //MathHelper.getSawtoothWave(System.currentTimeMillis(), 0.2f)
        val texReg: TextureRegion = if (!isInAir) {
            tileset.rodGroundAnimations[(animationAlpha * tileset.rodGroundFrames).toInt().coerceIn(0, tileset.rodGroundFrames - 1)]
        } else {
            tileset.rodAerialAnimations[(animationAlpha * tileset.rodAerialFrames).toInt().coerceIn(0, tileset.rodAerialFrames - 1)]
        }

        batch.draw(texReg, convertedVec.x - (1 / 32f), convertedVec.y, getRenderWidth(), getRenderHeight())

    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)

        collisionCheck(engine, beat, seconds)

        if (seconds >= explodeAtSec) {
            explode(engine)
        } else if ((beat - deployBeat) >= killAfterBeats && !isKilled) {
            kill()
        }
    }

    private fun collisionCheck(engine: Engine, beat: Float, seconds: Float) {
        val prevPosX = this.position.x
        val prevPosY = this.position.y
        val prevPosZ = this.position.z

        // Do collision check. Only works on the EntityRowBlocks for the given row.
        // 1. Stops instantly if it hits a prematurely deployed piston
        // 2. Stops if it hits a block when falling
        val beatsFromDeploy = beat - deployBeat
        val beatsFromFirst = beatsFromDeploy - 4
        val targetX = getPosXFromBeat(beatsFromDeploy)
        // The index that the rod is on
        val currentIndexFloat = targetX - row.startX
        val currentIndex = floor(currentIndexFloat).toInt()

        // Check for wall stop
        if (!collidedWithWall && (currentIndexFloat - currentIndex) >= 0.7f) {
            val nextIndex = currentIndex + 1
            if (nextIndex in 0 until row.length) {
                val next = row.rowBlocks[nextIndex]
                val heightOfNext = 1f + (if (next.pistonState != EntityRowBlock.PistonState.RETRACTED) (6f / 40f) else 0f)
                if (next.active && prevPosY in next.position.y..(next.position.y + heightOfNext - (1f / 40f))) {
                    collidedWithWall = true
                    this.position.x = currentIndex + 0.7f + row.startX
                    explodeAtSec = seconds + (1 / 3f)
                }
            }
        }
        if (!collidedWithWall) {
            this.position.x = targetX
        }
        
        // TODO implement falling down physics
    }
}