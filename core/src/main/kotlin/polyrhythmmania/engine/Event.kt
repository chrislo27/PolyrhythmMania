package polyrhythmmania.engine


/**
 * An [Event] is an object that has a start beat and a non-negative (zero or larger) width in beats.
 * There are three functions: [onStart], [onUpdate], and [onEnd].
 * 
 * [onStart] is called when the event is first updated,
 * then [onUpdate] is called at least once after [onStart] and every update frame while this event is being updated,
 * and [onEnd] is called at the very end.
 */
open class Event(val engine: Engine) {
    
    var beat: Float = 0f
    var width: Float = 0f
    var updateCompletion: UpdateCompletion = UpdateCompletion.PENDING

    open fun onStart(currentBeat: Float) {

    }

    open fun onUpdate(currentBeat: Float) {

    }

    open fun onEnd(currentBeat: Float) {
        
    }
    
    fun isBeatInside(beat: Float): Boolean = beat in this.beat..(this.beat + width)
    
    enum class UpdateCompletion {
        PENDING,
        UPDATING,
        COMPLETED,
        ;
    }

}
