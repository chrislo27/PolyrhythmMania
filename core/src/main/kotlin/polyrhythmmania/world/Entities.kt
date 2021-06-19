package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import paintbox.util.Vector3Stack
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.TintedRegion
import polyrhythmmania.world.render.WorldRenderer



open class SimpleRenderedEntity(world: World) : Entity(world) {
    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine) {
        val tmpVec = Vector3Stack.getAndPush()
        val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(this.position))
        val packedColor = batch.packedColor
        renderSimple(renderer, batch, tileset, engine, convertedVec)
        Vector3Stack.pop()
        batch.packedColor = packedColor
    }
    
    protected open fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine, vec: Vector3) {
    }
    
    protected fun drawTintedRegion(batch: SpriteBatch, vec: Vector3, tintedRegion: TintedRegion, offsetX: Float, offsetY: Float, renderWidth: Float, renderHeight: Float) {
        batch.color = tintedRegion.color.getOrCompute()
        batch.draw(tintedRegion.region, vec.x + offsetX, vec.y + offsetY, renderWidth, renderHeight)
    }
    
    protected fun drawTintedRegion(batch: SpriteBatch, vec: Vector3, tintedRegion: TintedRegion) {
        drawTintedRegion(batch, vec, tintedRegion, 0f, 0f, renderWidth, renderHeight)
    }
}

abstract class SpriteEntity(world: World) : SimpleRenderedEntity(world) {
    
    open val numLayers: Int = 1
    
    abstract fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion?

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine, vec: Vector3) {
        for (i in 0 until numLayers) {
            val tr = getTintedRegion(tileset, i)
            if (tr != null)
            drawTintedRegion(batch, vec, tr)
        }
    }
}

class EntityPlatform(world: World, val withLine: Boolean = false) : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return if (withLine) tileset.platformWithLine else tileset.platform
    }
}

open class EntityCube(world: World, val withLine: Boolean = false, val withBorder: Boolean = false)
    : SpriteEntity(world) {

    override val numLayers: Int = 6

    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        return when (index) {
            0 -> tileset.cubeBorder
            1 -> tileset.cubeFaceX
            2 -> tileset.cubeFaceY
            3 -> tileset.cubeFaceZ
            4 -> if (withLine) tileset.redLine else null
            5 -> if (withBorder) tileset.platformBorder else null
            else -> null
        }
    }
}

class EntityExplosion(world: World, val secondsStarted: Float, val rodWidth: Float)
    : SimpleRenderedEntity(world), TemporaryEntity {

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

    override val renderWidth: Float
        get() = state.renderWidth
    override val renderHeight: Float
        get() = state.renderHeight

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine, vec: Vector3) {
        if (isKilled) return
        val secondsElapsed = engine.seconds - secondsStarted
        val percentage = (secondsElapsed / duration).coerceIn(0f, 1f)
        if (percentage >= 1f) {
            kill()
        } else {
            val index = (percentage * STATES.size).toInt()
            state = STATES[index]
            val texReg = tileset.explosionFrames[index]
            val renderWidth = this.renderWidth
            val renderHeight = this.renderHeight
            batch.color = texReg.color.getOrCompute()
            batch.draw(texReg.region, vec.x - renderWidth / 2f + rodWidth / 2f - (2f / 32f), vec.y + (3f / 32f), renderWidth, renderHeight)
        }
    }
}

class EntitySign(world: World, val type: Type) : SpriteEntity(world) {
    enum class Type {
        A, DPAD, BO, TA, N;
    }

    override val numLayers: Int = 2
    override val renderWidth: Float = 0.5f
    override val renderHeight: Float = 0.5f
    
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return when (type) {
            Type.A -> if (index == 1) tileset.signAShadow else tileset.signA
            Type.DPAD -> if (index == 1) tileset.signDpadShadow else tileset.signDpad
            Type.BO -> if (index == 1) tileset.signBoShadow else tileset.signBo
            Type.TA -> if (index == 1) tileset.signTaShadow else tileset.signTa
            Type.N -> if (index == 1) tileset.signNShadow else tileset.signN
        }
    }
}