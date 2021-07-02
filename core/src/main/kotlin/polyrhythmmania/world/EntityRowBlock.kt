package polyrhythmmania.world

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import paintbox.registry.AssetRegistry
import polyrhythmmania.engine.Engine
import polyrhythmmania.soundsystem.BeadsSound
import kotlin.math.floor


class EntityRowBlock(world: World, val baseY: Float, val row: Row, val rowIndex: Int)
    : EntityPiston(world) {

    enum class RetractionState {
        NEUTRAL,
        EXTENDING,
        RETRACTING
    }

    var retractionState: RetractionState = RetractionState.NEUTRAL
        private set
    private var retractionPercentage: Float = 0f

    init {
        this.position.y = baseY - 1f // Start in the ground
    }

    override fun fullyExtend(engine: Engine, beat: Float) {
        super.fullyExtend(engine, beat)

        when (type) {
            Type.PLATFORM -> {
            }
            Type.PISTON_A -> engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_input_a"))
            Type.PISTON_DPAD -> engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_input_d"))
        }

        // For auto-inputs only. For regular inputs, see EngineInputter
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

    override fun retract(): Boolean {
        val result = super.retract()
        row.updateInputIndicators()
        return result
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

    /**
     * Returns true if some property was affected.
     */
    fun despawn(percentage: Float): Boolean {
        val clamped = percentage.coerceIn(0f, 1f)
        if (retractionPercentage < (1f - clamped)) return false
        if (active) {
            active = clamped < 1f
            position.y = Interpolation.linear.apply(baseY, baseY - 1, clamped)
            row.updateInputIndicators()
            retractionState = if (clamped < 1f) RetractionState.NEUTRAL else RetractionState.RETRACTING
            retractionPercentage = 1f - clamped
            return true
        }
        return false
    }
}