package paintbox.ui.animation

import com.badlogic.gdx.math.Interpolation


data class Animation(val interpolation: Interpolation, val duration: Float, val start: Float, val end: Float,
                     val delay: Float = 0f) {
    
    var onStart: (() -> Unit)? = null
    var onComplete: (() -> Unit)? = null
    
    fun applyFunc(alpha: Float): Float = interpolation.apply(start, end, alpha)
    
}
