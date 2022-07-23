package polyrhythmmania.world.spotlights

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.tileset.PaletteTransition


abstract class AbstractSpotlightEvent(engine: Engine, startBeat: Float) : Event(engine) {
    
    protected val spotlights: Spotlights 
        get() = engine.world.spotlights
    
    init {
        this.beat = startBeat
    }
}

class EventSpotlightTransition(
        engine: Engine, startBeat: Float, val paletteTransition: PaletteTransition,
        val lightColor: LightColor,
        val targetColor: Color?, val targetStrength: Float?,
) : AbstractSpotlightEvent(engine, startBeat) {

    private val startColor: Color = Color(1f, 1f, 1f, 1f)
    private var startStrength: Float = 0f

    init {
        this.width = paletteTransition.duration
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        this.startColor.set(lightColor.color)
        this.startStrength = lightColor.strength
    }

    override fun onUpdate(currentBeat: Float) {
        super.onUpdate(currentBeat)

        val percentage = this.paletteTransition.translatePercentage(getBeatPercentage(currentBeat)).coerceIn(0f, 1f)
        if (this.targetColor != null) {
            lightColor.color.set(startColor).lerp(this.targetColor, percentage)
        }
        if (this.targetStrength != null) {
            lightColor.strength = MathUtils.lerp(this.startStrength, this.targetStrength, percentage)
        }
    }
}
