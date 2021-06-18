package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.util.Vector3Stack
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.render.OldTileset
import polyrhythmmania.world.render.WorldRenderer


class EntityPlatform(world: World, val withLine: Boolean = false) : SimpleRenderedEntity(world) {
    override fun getTextureRegionFromTileset(tileset: OldTileset): TextureRegion {
        return if (withLine) tileset.platformWithLine else tileset.platform
    }
}

class EntityCube(world: World, val withLine: Boolean = false)
    : SimpleRenderedEntity(world) {
    override fun getTextureRegionFromTileset(tileset: OldTileset): TextureRegion {
        return if (withLine) tileset.cubeWithLine else tileset.cube
    }
}

class EntityCubeBordered(world: World)
    : SimpleRenderedEntity(world) {

    override fun getTextureRegionFromTileset(tileset: OldTileset): TextureRegion {
        return tileset.cube
    }

    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: OldTileset, engine: Engine) {
        super.render(renderer, batch, tileset, engine)
        val texReg = tileset.cubeWithBlackBorder
        val tmpVec = Vector3Stack.getAndPush()
        val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(this.position))
        val renderWidth = getRenderWidth()
        val renderHeight = getRenderHeight()
        batch.draw(texReg, convertedVec.x, convertedVec.y, renderWidth, renderHeight)
        Vector3Stack.pop()
    }
}


class EntityExplosion(world: World, val secondsStarted: Float, val rodWidth: Float)
    : Entity(world), TemporaryEntity {

    companion object {
        private val STATES: List<State> = listOf(
                State(40f / 32f, 24f / 32f),
                State(32f / 32f, 24f / 32f),
                State(24f / 32f, 16f / 32f),
                State(16f / 32f, 16f / 32f),
        )
    }

    private data class State(val renderWidth: Float, val renderHeight: Float)

    private var state: State = STATES[0]
    private val duration: Float = 8 / 60f

    override fun getRenderWidth(): Float = state.renderWidth
    override fun getRenderHeight(): Float = state.renderHeight

    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: OldTileset, engine: Engine) {
        if (isKilled) return
        val secondsElapsed = engine.seconds - secondsStarted
        val percentage = (secondsElapsed / duration).coerceIn(0f, 1f)
        if (percentage >= 1f) {
            kill()
        } else {
            val tmpVec = Vector3Stack.getAndPush()
            val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(this.position))
            val index = (percentage * STATES.size).toInt()
            state = STATES[index]
            val texReg = tileset.rodExplodeAnimations[index]
            val renderWidth = getRenderWidth()
            val renderHeight = getRenderHeight()
            batch.draw(texReg, convertedVec.x - renderWidth / 2f + rodWidth / 2f - (2f / 32f), convertedVec.y + (3f / 32f), renderWidth, renderHeight)
            Vector3Stack.pop()
        }
    }
}

class EntitySign(world: World, val spriteIndex: Int) : SimpleRenderedEntity(world) {
    override fun getRenderWidth(): Float = 0.5f
    override fun getRenderHeight(): Float = 0.5f

    override fun getTextureRegionFromTileset(tileset: OldTileset): TextureRegion {
        return tileset.buttonSigns[spriteIndex]
    }
}