package polyrhythmmania.animation

import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyFloatVar


/**
 * An animation player handles the state machine in playing back an [Animation].
 */
class AnimationPlayer(val animation: Animation) {
    
    val numberOfFrames: Int get() = animation.numberOfFrames
    
    private var secCounter: Float = 0f
    
    private var currentFrame: Int = 0
    
    private var currentStepIndex: Int = 0
    private var currentStepFrameOffset: Int = 0
    
    val framesPerSec: FloatVar = FloatVar(30f)
    val secondsPerFrame: ReadOnlyFloatVar = FloatVar { 1f / framesPerSec.use() }
    
    
    fun reset() {
        setFrame(0)
    }
    
    fun getCurrentFrame(): Int = currentFrame
    
    fun setFrame(frame: Int) {
        currentFrame = frame
        if (animation.loops) {
            currentFrame %= numberOfFrames
        }
        secCounter = 0f
        
        currentStepIndex = 0
        currentStepFrameOffset = 0
        lookupCurrentStep(frame)
    }

    /**
     * Looks up the current step info from the beginning of the step list.
     */
    private fun lookupCurrentStep(frame: Int) {
        var counter = 0
        for ((index, step) in animation.steps.withIndex()) {
            counter += step.delay
            if (counter > frame) {
                currentStepIndex = index
                currentStepFrameOffset = counter - frame
                break
            }
        }
    }
    
    fun update(deltaSec: Float) {
        val secPerFrame = secondsPerFrame.get()
        secCounter += deltaSec
        
        if (secCounter > secPerFrame) {
            val addFrames = (secCounter / secPerFrame).toInt()
            secCounter -= addFrames * secPerFrame
            
            if (currentFrame + addFrames >= numberOfFrames) {
                currentFrame += addFrames
                if (animation.loops) {
                    currentFrame %= numberOfFrames
                }
                lookupCurrentStep(currentFrame)
            } else {
                var framesToAdd = addFrames
                
                while (framesToAdd > 0) {
                    val remainingFramesForStep = animation.steps[currentStepIndex].delay - currentStepFrameOffset
                    if (framesToAdd < remainingFramesForStep || currentStepIndex == animation.steps.size - 1) {
                        currentStepFrameOffset += framesToAdd
                        break
                    } else {
                        framesToAdd -= remainingFramesForStep
                        currentStepIndex++
                    }
                }
            }
        }
    }
    
}