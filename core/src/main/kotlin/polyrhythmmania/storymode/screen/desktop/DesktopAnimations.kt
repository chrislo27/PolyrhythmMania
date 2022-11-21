package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.math.Interpolation
import polyrhythmmania.ui.TogglableInputProcessor


class DesktopAnimations(val desktopUI: DesktopUI, private val inputProcessor: TogglableInputProcessor) {
    
    abstract class Anim(val duration: Float) {
        private var progressSec: Float = 0f
        private var progressPercent: Float = 0f
        
        protected abstract fun onUpdate(progressSec: Float, progressPercent: Float) 
        
        fun frameUpdate(delta: Float): Float {
            progressSec += delta
            progressPercent = if (duration <= 0f) 1f else (progressSec / duration).coerceIn(0f, 1f)
            
            onUpdate(progressSec, progressPercent)
            
            return if (isDone()) { 
                // Returns leftover time
                (progressSec - duration).coerceIn(0f, delta)
            } else {
                0f
            }
        }
        
        fun isDone(): Boolean = progressPercent >= 1f
        
        protected fun finishNow() {
            progressSec = duration
            progressPercent = 1f
        }
    }
    
    class AnimDelay(duration: Float) : Anim(duration) {
        override fun onUpdate(progressSec: Float, progressPercent: Float) {
            // NO-OP
        }
    }
    
    class AnimGeneric(duration: Float, private val updateFunc: (progressSec: Float, progressPercent: Float) -> Unit) : Anim(duration) {
        override fun onUpdate(progressSec: Float, progressPercent: Float) {
            updateFunc(progressSec, progressPercent)
        }
    }

    inner class AnimLockInputs(val lock: Boolean) : Anim(0f) {
        override fun onUpdate(progressSec: Float, progressPercent: Float) {
            inputProcessor.enabled.set(!lock)
        }
    }

    inner class AnimScrollBar(duration: Float, val targetPos: Float) : Anim(duration) {
        
        private var oldPos: Float = -1f
        
        override fun onUpdate(progressSec: Float, progressPercent: Float) {
            val scrollbar = desktopUI.inboxItemListScrollbar
            
            if (oldPos < 0f) {
                oldPos = scrollbar.value.get()
                if (oldPos == targetPos) {
                    finishNow()
                    return
                }
            }

            val interpolation = Interpolation.smoother
            scrollbar.setValue(interpolation.apply(oldPos, targetPos, progressPercent))
        }
    }
    
    
    private val animationQueue: MutableList<Anim> = mutableListOf()
    
    fun enqueueAnimation(animation: Anim) {
        animationQueue += animation
    }
    
    fun frameUpdate(delta: Float) {
        if (animationQueue.isNotEmpty()) {
            var remainingDelta = delta
            while (remainingDelta > 0f && animationQueue.isNotEmpty()) {
                val anim = animationQueue.first()
                val leftover = anim.frameUpdate(remainingDelta)
                if (anim.isDone()) {
                    remainingDelta = leftover
                    animationQueue.removeFirst()
                } else break
            }
        }
    }
    
}