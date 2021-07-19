package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import paintbox.util.ColorStack
import paintbox.util.Vector3Stack
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.World
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.TintedRegion
import polyrhythmmania.world.render.WorldRenderer
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.roundToInt


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
    
    companion object {
        fun createCubemapIndex(x: Int, y: Int, z: Int): Long {
            val overflow = x !in Short.MIN_VALUE..Short.MAX_VALUE || y !in Short.MIN_VALUE..Short.MAX_VALUE || z !in Short.MIN_VALUE..Short.MAX_VALUE

            return 0L or (if (overflow) (1L shl 63) else 0L).toLong() or (
                    x.toShort().toLong() or (y.toShort().toLong() shl 16) or (z.toShort().toLong() shl 32)
                    )
        }
    }

    override val numLayers: Int = 6

    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        // Uncomment if cube map culling is to be used
//        val cubeOccludesX = world.cubeMap[createCubemapIndex(this.position.x.roundToInt() - 1, this.position.y.roundToInt(), this.position.z.roundToInt())] != null
//        val cubeOccludesY = world.cubeMap[createCubemapIndex(this.position.x.roundToInt(), this.position.y.roundToInt() + 1, this.position.z.roundToInt())] != null
//        val cubeOccludesZ = world.cubeMap[createCubemapIndex(this.position.x.roundToInt(), this.position.y.roundToInt(), this.position.z.roundToInt() + 1)] != null
//
//        return when (index) { // Update when with non-culling
//            0 -> tileset.cubeBorder
//            1 -> if (cubeOccludesZ) null else tileset.cubeBorderZ
//            2 -> if (cubeOccludesX) null else tileset.cubeFaceX
//            3 -> if (cubeOccludesY) null else tileset.cubeFaceY
//            4 -> if (cubeOccludesZ) null else tileset.cubeFaceZ
//            5 -> if (withLine) tileset.redLine else null
//            6 -> if (withBorder) tileset.platformBorder else null
//            else -> null
//        }
        return when (index) {
            0 -> if (withBorder) tileset.cubeBorderPlatform else tileset.cubeBorder
            1 -> tileset.cubeBorderZ
            2 -> tileset.cubeFaceX
            3 -> tileset.cubeFaceY
            4 -> tileset.cubeFaceZ
            5 -> if (withLine) tileset.redLine else null
            else -> null
        }
    }
    
    fun getCubemapIndex(): Long {
        return createCubemapIndex(this.position.x.roundToInt(), this.position.y.roundToInt(), this.position.z.roundToInt())
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
            Type.A -> if (index == 0) tileset.signAShadow else tileset.signA
            Type.DPAD -> if (index == 0) tileset.signDpadShadow else tileset.signDpad
            Type.BO -> if (index == 0) tileset.signBoShadow else tileset.signBo
            Type.TA -> if (index == 0) tileset.signTaShadow else tileset.signTa
            Type.N -> if (index == 0) tileset.signNShadow else tileset.signN
        }
    }
}

class EntityInputFeedback(world: World, val end: End, color: Color, val flashIndex: Int)
    : SimpleRenderedEntity(world) {
    
    companion object {
        val ACE_COLOUR: Color = Color.valueOf("FFF800")
        val GOOD_COLOUR: Color = Color.valueOf("6DE23B")
        val BARELY_COLOUR: Color = Color.valueOf("FF7C26")
        val MISS_COLOUR: Color = Color.valueOf("E82727")
    }
    
    enum class End {
        LEFT, MIDDLE, RIGHT;
    }
    
    private val originalColor: Color = color.cpy()
    private val color: Color = color.cpy()

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine, vec: Vector3) {
        super.renderSimple(renderer, batch, tileset, engine, vec)

        val currentSec = engine.seconds
        val flashSec = engine.inputter.inputFeedbackFlashes[flashIndex]
        val flashTime = 0.25f
        if (currentSec - flashSec < flashTime) {
            val percentage = ((currentSec - flashSec) / flashTime).coerceIn(0f, 1f)
            color.set(originalColor).lerp(Color.WHITE, 1f - percentage)
        } else {
            color.set(originalColor)
        }
        
        val tintedRegion = when (end) {
            End.LEFT -> tileset.inputFeedbackStart
            End.MIDDLE -> tileset.inputFeedbackMiddle
            End.RIGHT -> tileset.inputFeedbackEnd
        }
        val tmpColor = ColorStack.getAndPush().set(tintedRegion.color.getOrCompute())
        tmpColor.mul(this.color)
        batch.color = tmpColor
        batch.draw(tintedRegion.region, vec.x, vec.y, renderWidth, renderHeight)
        ColorStack.pop()
    }
}

class EntityInputIndicator(world: World, var isDpad: Boolean)
    : SimpleRenderedEntity(world) {

    var visible: Boolean = true

    override val renderWidth: Float = 16f / 32f
    override val renderHeight: Float = 16f / 32f

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine, vec: Vector3) {
        if (!visible || !world.worldSettings.showInputIndicators) return
        val tintedRegion = if (isDpad) tileset.indicatorDpad else tileset.indicatorA
        val renderWidth = this.renderWidth
        val renderHeight = this.renderHeight

        val bumpHeight = 2f / 32f
        val beat = engine.beat
        val bumpTime = 0.28f
        val normalizedBeat = if (beat < 0f) (beat + floor(beat).absoluteValue) else (beat)
        val bumpAmt = (1f - (normalizedBeat % 1f).coerceIn(0f, bumpTime) / bumpTime)//.coerceIn(0f, 1f)

        batch.color = tintedRegion.color.getOrCompute()
        batch.draw(tintedRegion.region, vec.x - renderWidth / 2f,
                vec.y + (bumpAmt) * bumpHeight - (2f / 32f),
                renderWidth, renderHeight)
    }
}
