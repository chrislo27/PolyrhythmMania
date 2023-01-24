package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Engine
import polyrhythmmania.world.World
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TintedRegion


open class EntityPiston(world: World)
    : SpriteEntity(world) {

    companion object {
        val FULL_EXTENSION_TIME_BEATS: Float = 0.05f
    }

    enum class Type(val renderHeight: Float, val isPiston: Boolean) {
        PLATFORM(1f, false),
        PISTON_A(1.25f, true),
        PISTON_DPAD(1.25f, true)
    }

    enum class PistonState {
        FULLY_EXTENDED,
        PARTIALLY_EXTENDED,
        RETRACTED
    }

    var pistonState: PistonState = PistonState.RETRACTED
    var type: Type = Type.PLATFORM
        set(value) {
            field = value
            pistonState = PistonState.RETRACTED
        }
    var active: Boolean = true

    val collisionHeight: Float
        get() = if (type == Type.PLATFORM || pistonState == PistonState.RETRACTED) 1f else 1.15f

    // Piston extension animation properties
    private var shouldPartiallyExtend: Boolean = false
    private var fullyExtendedAtBeat: Float = 0f

    override val renderWidth: Float get() = 1f
    override val renderHeight: Float get() = this.type.renderHeight

    open fun fullyExtend(engine: Engine, beat: Float) {
        pistonState = PistonState.FULLY_EXTENDED
        shouldPartiallyExtend = true
        fullyExtendedAtBeat = beat
    }

    open fun retract(): Boolean {
        val oldPistonState = pistonState
        pistonState = PistonState.RETRACTED
        shouldPartiallyExtend = false
        fullyExtendedAtBeat = 0f
        return oldPistonState != PistonState.RETRACTED
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

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        if (active) {
            super.renderSimple(renderer, batch, tileset, vec)
        }
    }
}