package paintbox.ui.animation

import com.badlogic.gdx.Gdx
import paintbox.binding.FloatVar
import paintbox.ui.SceneRoot
import java.util.concurrent.ConcurrentHashMap


class AnimationHandler(val sceneRoot: SceneRoot) {

    private data class AnimationTuple(val animation: Animation, val varr: FloatVar, var accumulatedSeconds: Float = 0f,
                                      var alpha: Float = 0f, var brandNew: Boolean = true)

    private val animations: MutableMap<FloatVar, AnimationTuple> = ConcurrentHashMap()

    /**
     * Set to 0 to disable animations.
     */
    val animationSpeed: FloatVar = FloatVar(1f)

    private val removeList: MutableSet<FloatVar> = mutableSetOf()
    
    fun frameUpdate() {
        val delta = Gdx.graphics.deltaTime
        val speed = animationSpeed.get()
        val isInstant = speed <= 0f
        
        animations.forEach { (_, tuple) ->
            val animation = tuple.animation
            val brandNew = tuple.brandNew
            tuple.accumulatedSeconds += delta
            if (brandNew) {
                tuple.brandNew = false
                tuple.animation.onStart?.invoke()
            }
            
            val newAlpha = if (isInstant || animation.duration <= 0f) 1f
            else (tuple.accumulatedSeconds / animation.duration).coerceIn(0f, 1f)
            tuple.alpha = newAlpha
            
            tuple.varr.set(tuple.animation.applyFunc(newAlpha))

            if (newAlpha >= 1f) {
                tuple.animation.onComplete?.invoke()
                removeList.add(tuple.varr)
            }
        }
        
        if (removeList.isNotEmpty()) {
            removeList.forEach { animations.remove(it) }
            removeList.clear()
        }
    }

    fun enqueueAnimation(animation: Animation, varr: FloatVar) {
        val existing = animations.remove(varr)
        if (existing != null) {
            existing.varr.set(existing.animation.applyFunc(1f))
            existing.animation.onComplete?.invoke()
        }
        animations[varr] = AnimationTuple(animation, varr)
    }
    
    fun cancelAnimation(animation: Animation) {
        val existing = animations.entries.firstOrNull { it.value.animation == animation } ?: return
        animations.remove(existing.key)
        existing.key.set(existing.value.animation.applyFunc(1f))
        existing.value.animation.onComplete?.invoke()
    }
    
    fun cancelAnimationFor(varr: FloatVar): Animation? {
        val existing = animations.remove(varr)
        if (existing != null) {
            existing.varr.set(existing.animation.applyFunc(1f))
            existing.animation.onComplete?.invoke()
        }
        return existing?.animation
    }

}