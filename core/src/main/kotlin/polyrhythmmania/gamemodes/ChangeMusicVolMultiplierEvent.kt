package polyrhythmmania.gamemodes

import com.badlogic.gdx.math.Interpolation
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event


class ChangeMusicVolMultiplierEvent(
        engine: Engine,
        val fromVol: Float, val toVol: Float, startBeat: Float, beatDuration: Float,
        val interpolation: Interpolation = Interpolation.linear,
) : Event(engine) {
    
    init {
        this.beat = startBeat
        this.width = beatDuration
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        
        this.engine.musicData.musicVolumeMultiplier = fromVol
    }

    override fun onUpdate(currentBeat: Float) {
        super.onUpdate(currentBeat)
        
        if (this.width <= 0f)
            return
        
        val percentage = ((currentBeat - this.beat) / this.width).coerceIn(0f, 1f)
        this.engine.musicData.musicVolumeMultiplier = interpolation.apply(fromVol, toVol, percentage)
    }

    override fun onEnd(currentBeat: Float) {
        super.onEnd(currentBeat)
        
        this.engine.musicData.musicVolumeMultiplier = toVol
    }
}
