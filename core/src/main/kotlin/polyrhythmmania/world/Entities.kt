package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.WorldRenderer


class EntityPlatform(world: World, val withLine: Boolean = false) : SimpleRenderedEntity(world) {
    override fun getTextureRegionFromTileset(tileset: Tileset): TextureRegion {
        return if (withLine) tileset.platformWithLine else tileset.platform
    }
}

class EntityCube(world: World, val withLine: Boolean = false)
    : SimpleRenderedEntity(world) {
    override fun getTextureRegionFromTileset(tileset: Tileset): TextureRegion {
        return if (withLine) tileset.cubeWithLine else tileset.cube
    }
}

class EntityCubeBordered(world: World)
    : SimpleRenderedEntity(world) {
    companion object {
        private val tmpVec = Vector3()
    }

    override fun getTextureRegionFromTileset(tileset: Tileset): TextureRegion {
        return tileset.cube
    }

    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine) {
        super.render(renderer, batch, tileset, engine)
        val texReg = tileset.cubeWithBlackBorder
        val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(this.position))
        val w = 16f / 32f
        val h = 8f / 32f
        val renderHeight = getRenderHeight()
        batch.draw(texReg, convertedVec.x, convertedVec.y + (renderHeight - h), w, h)
    }
}


class EntityExplosion(world: World, val secondsStarted: Float, val rodWidth: Float) : Entity(world) {

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
            batch.draw(texReg, convertedVec.x - renderWidth / 2f + rodWidth / 2f - (2f / 32f), convertedVec.y + (3f / 32f), renderWidth, renderHeight)
        }
    }
}

class EntitySign(world: World, val spriteIndex: Int) : SimpleRenderedEntity(world) {
    override fun getRenderWidth(): Float = 0.5f
    override fun getRenderHeight(): Float = 0.5f

    override fun getTextureRegionFromTileset(tileset: Tileset): TextureRegion {
        return tileset.buttonSigns[spriteIndex]
    }
}