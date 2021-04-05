package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3
import io.github.chrislo27.paintbox.util.MathHelper
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.WorldRenderer


class EntityPlatform(world: World, val withLine: Boolean = false) : SimpleRenderedEntity(world) {
    override fun getTextureRegionFromTileset(tileset: Tileset): TextureRegion {
        return if (withLine) tileset.platformWithLine else tileset.platform
    }
}

class EntityCube(world: World, val withLine: Boolean = false) : SimpleRenderedEntity(world) {
    override fun getTextureRegionFromTileset(tileset: Tileset): TextureRegion {
        return if (withLine) tileset.cubeWithLine else tileset.cube
    }
}


class EntityRod(world: World, val deployBeat: Float, val row: Row) : Entity(world) {

    companion object {
        private val tmpVec = Vector3()
    }

    var isInAir: Boolean = false
    var collidedWithWall: Boolean = false
    var collidedWithFloor: Boolean = false
    private var initializedActiveBlocks = false
    private val activeBlocks: BooleanArray = BooleanArray(row.length)
    private val xUnitsPerBeat: Float = 2f
    private val killAfterBeats: Float = 4f + row.length / xUnitsPerBeat + 1 // 4 prior to first index 0 + rowLength/xUnitsPerBeat + 1 buffer

    init {
        this.position.z = row.startZ.toFloat()
        this.position.y = row.startY.toFloat() + 1f
    }

    override fun getRenderWidth(): Float = 0.75f
    override fun getRenderHeight(): Float = 0.5f

    fun getPosXFromBeat(beat: Float): Float {
        return (row.startX + 0.5f - 4 * xUnitsPerBeat) + (beat - deployBeat) * xUnitsPerBeat - (6 / 32f)
    }
    
    fun explode(engine: Engine) {
        if (isKilled) return
        kill()
        world.addEntity(EntityExplosion(world, engine.seconds).also {
            it.position.set(this.position)
            it.position.x += this.getRenderWidth() / 2f
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


        if ((beat - deployBeat) >= killAfterBeats && !isKilled) {
            kill()
        }
    }
    
    private fun collisionCheck(engine: Engine, beat: Float, seconds: Float) {
        val prevPosX = this.position.x
        val prevPosZ = this.position.z
        
        if (!collidedWithWall) {
            val targetX = (row.startX + 0.5f - 4 * xUnitsPerBeat) + (beat - deployBeat) * xUnitsPerBeat
            // Do collision check. Only works on the EntityRowBlocks for the given row.
            // 1. Stops instantly if it hits a prematurely deployed piston
            // 2. Stops if it hits a block when falling
            this.position.x = targetX
        }
    }
}

class EntityExplosion(world: World, val secondsStarted: Float) : Entity(world) {

    companion object {
        private val tmpVec = Vector3()
        private val STATES: List<State> = listOf(
                State(40f / 32f, 24f / 40f),
                State(32f / 32f, 24f / 40f),
                State(24f / 32f, 16f / 40f),
                State(16f / 32f, 16f / 40f),
        )
    }

    private data class State(val renderWidth: Float, val renderHeight: Float)

    private var state: State = STATES[0]
    private val duration: Float = 8 / 60f

    override fun getRenderWidth(): Float = state.renderWidth
    override fun getRenderHeight(): Float = state.renderHeight

    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine) {
        if (isKilled) return
        val secondsElapsed = engine.seconds - secondsStarted
        val percentage = (secondsElapsed / duration).coerceIn(0f, 1f)
        if (percentage >= 1f) {
            kill()
        } else {
            val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(this.position))
            val index = (percentage * STATES.size).toInt()
            state = STATES[index]
            val texReg = tileset.rodExplodeAnimations[index]
            val renderWidth = getRenderWidth()
            val renderHeight = getRenderHeight()
            batch.draw(texReg, convertedVec.x - renderWidth / 2f + (3f / 32f), convertedVec.y + (0f / 32f), renderWidth, renderHeight)
        }
    }
}
