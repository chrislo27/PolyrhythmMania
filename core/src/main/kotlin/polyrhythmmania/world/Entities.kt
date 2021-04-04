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
    private val xUnitsPerBeat: Float = 2f
    private val killAfterBeats: Float = 4f + row.length / xUnitsPerBeat + 1 // 4 prior to first index 0 + rowLength/xUnitsPerBeat + 1 buffer
    
    init {
        this.position.z = row.startZ.toFloat()
        this.position.y = row.startY.toFloat() + 1f
    }

    override fun getRenderWidth(): Float = 0.75f
    override fun getRenderHeight(): Float = 0.5f

    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine) {
        val convertedVec = renderer.convertWorldToScreen(tmpVec.set(this.position))
        
        val beatsFullAnimation = 60f / 128f
        val animationAlpha = (((engine.beat - deployBeat) % beatsFullAnimation) / beatsFullAnimation).coerceIn(0f, 1f) //MathHelper.getSawtoothWave(System.currentTimeMillis(), 0.2f)
        val texReg: TextureRegion = if (!isInAir) {
            tileset.rodGroundAnimations[(animationAlpha * tileset.rodGroundFrames).toInt().coerceIn(0, tileset.rodGroundFrames - 1)]
        } else {
            tileset.rodAerialAnimations[(animationAlpha * tileset.rodAerialFrames).toInt().coerceIn(0, tileset.rodAerialFrames - 1)]
        }
        
        batch.draw(texReg, convertedVec.x - (1 / 32f), convertedVec.y, getRenderWidth(), getRenderHeight())
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        this.position.x = (row.startX + 0.5f - 4 * xUnitsPerBeat) + (beat - deployBeat) * xUnitsPerBeat
        
        if ((beat - deployBeat) >= killAfterBeats && !isKilled) {
            kill()
        }
    }
}