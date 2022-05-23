package polyrhythmmania.animation


/**
 * An animation is a sequence of steps.
 */
class Animation(val steps: List<Step>) {
    
    val numberOfFrames: Int = steps.sumOf { it.delay }
    
    var loops: Boolean = false
    var framesPerSecond: Float = 30f
    
    fun createPlayer(): AnimationPlayer = AnimationPlayer(this)
    
}