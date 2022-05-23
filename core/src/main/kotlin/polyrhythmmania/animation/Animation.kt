package polyrhythmmania.animation


/**
 * An animation is a sequence of steps.
 */
class Animation(val steps: List<Step>) {
    
    val numberOfFrames: Int = steps.sumOf { it.delay }
    
    var loops: Boolean = false
    
    fun createPlayer(): AnimationPlayer = AnimationPlayer(this)
    
}