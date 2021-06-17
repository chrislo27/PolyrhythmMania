package polyrhythmmania.editor

import com.badlogic.gdx.math.Interpolation


class CameraOffset {
    var target: Float = 0f
        private set
    var current: Float = 0f
        private set
    var speed: Float = 1f / 0.5f
    private var lastPrevious: Float = 0f
    private var time: Float = 0f
    
    fun changeTarget(newTarget: Float) {
        if (target == newTarget) return
        lastPrevious = current
        target = newTarget
        time = 0f
    }
    
    fun update(delta: Float) {
        time += delta * speed
        time = time.coerceIn(0f, 1f)
        current = Interpolation.smooth.apply(lastPrevious, target, time)
    }
}
