package polyrhythmmania.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import paintbox.registry.AssetRegistry
import polyrhythmmania.engine.Engine
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.TintedRegion
import polyrhythmmania.world.render.WorldRenderer
import kotlin.math.floor


class EntityRowBlock(world: World, val baseY: Float, val row: Row, val rowIndex: Int)
    : SpriteEntity(world) {

    companion object {
        val FULL_EXTENSION_TIME_BEATS: Float = 0.05f
    }

    enum class Type(val renderHeight: Float) {
        PLATFORM(1f),
        PISTON_A(1.25f),
        PISTON_DPAD(1.25f)
    }

    enum class PistonState {
        FULLY_EXTENDED,
        PARTIALLY_EXTENDED,
        RETRACTED
    }

    enum class RetractionState {
        NEUTRAL,
        EXTENDING,
        RETRACTING
    }

    var pistonState: PistonState = PistonState.RETRACTED
    var type: Type = Type.PLATFORM
        set(value) {
            field = value
            pistonState = PistonState.RETRACTED
        }
    var active: Boolean = true
    var retractionState: RetractionState = RetractionState.NEUTRAL
        private set
    private var retractionPercentage: Float = 0f

    val collisionHeight: Float
        get() = if (type == Type.PLATFORM || pistonState == PistonState.RETRACTED) 1f else 1.15f

    private var shouldPartiallyExtend: Boolean = false
    private var fullyExtendedAtBeat: Float = 0f

    override val renderWidth: Float get() = 1f
    override val renderHeight: Float get() = this.type.renderHeight

    init {
        this.position.y = baseY - 1f
    }

    fun fullyExtend(engine: Engine, beat: Float) {
        pistonState = PistonState.FULLY_EXTENDED
        shouldPartiallyExtend = true
        fullyExtendedAtBeat = beat
        
        when (type) {
            Type.PLATFORM -> {
            }
            Type.PISTON_A -> engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_input_a"))
            Type.PISTON_DPAD -> engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_input_d"))
        }

        if (this.type != Type.PLATFORM && engine.autoInputs) {
            // Bounce any rods that are on this index
            world.entities.forEach { entity ->
                if (entity is EntityRod) {
                    // The index that the rod is on
                    val currentIndexFloat = entity.position.x - entity.row.startX
                    val currentIndex = floor(currentIndexFloat).toInt()
                    if (currentIndex == this.rowIndex && MathUtils.isEqual(entity.position.z, this.position.z)
                            && entity.position.y in (this.position.y + 1f - (1f / 32f))..(this.position.y + collisionHeight)) {
                        entity.bounce(currentIndex)
                    }
                }
            }
        }

        row.updateInputIndicators()
    }

    fun spawn(percentage: Float) {
        val clamped = percentage.coerceIn(0f, 1f)
        if (retractionPercentage > clamped) return
        active = clamped > 0f
        position.y = Interpolation.linear.apply(baseY - 1, baseY, clamped)
        row.updateInputIndicators()
        retractionState = if (clamped <= 0f) RetractionState.NEUTRAL else RetractionState.EXTENDING
        retractionPercentage = clamped
    }

    fun despawn(percentage: Float) {
        val clamped = percentage.coerceIn(0f, 1f)
        if (retractionPercentage < (1f - clamped)) return
        if (active) {
            active = clamped < 1f
            position.y = Interpolation.linear.apply(baseY, baseY - 1, clamped)
            row.updateInputIndicators()
            retractionState = if (clamped < 1f) RetractionState.NEUTRAL else RetractionState.RETRACTING
            retractionPercentage = 1f - clamped
        }
    }

    fun retract() {
        pistonState = PistonState.RETRACTED
        row.updateInputIndicators()
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        if (shouldPartiallyExtend) {
            if (beat - fullyExtendedAtBeat >= FULL_EXTENSION_TIME_BEATS) {
                shouldPartiallyExtend = false
                pistonState = PistonState.PARTIALLY_EXTENDED
            }
        }
    }

    override val numLayers: Int
        get() {
            return when (type) {
                Type.PLATFORM -> 1
                else -> when (pistonState) {
                    PistonState.FULLY_EXTENDED -> 3
                    PistonState.PARTIALLY_EXTENDED -> 3
                    PistonState.RETRACTED -> 1
                }
            }
        }

    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        return when (type) {
            Type.PLATFORM -> tileset.platform
            Type.PISTON_A -> when (pistonState) {
                PistonState.FULLY_EXTENDED -> when (index) {
                    0 -> tileset.pistonAExtendedFaceX
                    1 -> tileset.pistonAExtendedFaceZ
                    2 -> tileset.pistonAExtended
                    else -> null
                }
                PistonState.PARTIALLY_EXTENDED -> when (index) {
                    0 -> tileset.pistonAPartialFaceX
                    1 -> tileset.pistonAPartialFaceZ
                    2 -> tileset.pistonAPartial
                    else -> null
                }
                PistonState.RETRACTED -> tileset.pistonARetracted
            }
            Type.PISTON_DPAD -> when (pistonState) {
                PistonState.FULLY_EXTENDED -> when (index) {
                    0 -> tileset.pistonDpadExtendedFaceX
                    1 -> tileset.pistonDpadExtendedFaceZ
                    2 -> tileset.pistonDpadExtended
                    else -> null
                }
                PistonState.PARTIALLY_EXTENDED -> when (index) {
                    0 -> tileset.pistonDpadPartialFaceX
                    1 -> tileset.pistonDpadPartialFaceZ
                    2 -> tileset.pistonDpadPartial
                    else -> null
                }
                PistonState.RETRACTED -> tileset.pistonDpadRetracted
            }
        }
    }

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, engine: Engine, vec: Vector3) {
        if (active) {
            super.renderSimple(renderer, batch, tileset, engine, vec)
        }
    }
}