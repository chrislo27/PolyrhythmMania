package polyrhythmmania.editor

import com.badlogic.gdx.math.Interpolation
import polyrhythmmania.util.DecimalFormats

class CameraPan(val duration: Float,
                val startBeat: Float, val endBeat: Float,
                val interpolationBeat: Interpolation = Interpolation.exp10Out) {

    private var timeElapsed: Float = 0f

    val progress: Float
        get() = if (duration <= 0f) 1f else (timeElapsed / duration).coerceIn(0f, 1f)

    val isDone: Boolean
        get() = progress >= 1f

    fun update(delta: Float, trackView: TrackView) {
        timeElapsed += delta

        if (startBeat != endBeat) {
            trackView.beat.set(interpolationBeat.apply(startBeat, endBeat, progress))
        }
    }

    override fun toString(): String {
        return "[start=${DecimalFormats.format("0.000", startBeat)}, end=${DecimalFormats.format("0.000", endBeat)}, duration=${DecimalFormats.format("0.000", duration)}, progress=${DecimalFormats.format("0.000", progress)}]"
    }

}