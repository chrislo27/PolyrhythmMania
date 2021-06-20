package polyrhythmmania.engine

import polyrhythmmania.container.Container


/**
 * An [Event] is an object that has a start beat and a non-negative (zero or larger) width in beats.
 * There are three functions: [onStart], [onUpdate], and [onEnd].
 * 
 * [onStart] is called when the event is first updated,
 * then [onUpdate] is called at least once after [onStart] and every update frame while this event is being updated,
 * and [onEnd] is called at the very end.
 */
open class Event(val engine: Engine) : Comparable<Event> {
    
    var beat: Float = 0f
    var width: Float = 0f
    var updateCompletion: UpdateCompletion = UpdateCompletion.PENDING

    open fun onStart(currentBeat: Float) {
    }

    open fun onUpdate(currentBeat: Float) {
    }

    open fun onEnd(currentBeat: Float) {
    }
    
    open fun onStartContainer(container: Container, currentBeat: Float) {
    }

    open fun onUpdateContainer(container: Container, currentBeat: Float) {
    }

    open fun onEndContainer(container: Container, currentBeat: Float) {
    }
    
    protected fun getBeatPercentage(currentBeat: Float): Float = if (width <= 0f) 1f else ((currentBeat - this.beat) / width)
    
    fun isBeatInside(beat: Float): Boolean = beat in this.beat..(this.beat + width)

    final override fun compareTo(other: Event): Int {
        val thisBeat = this.beat
        val otherBeat = other.beat
        return if (thisBeat == otherBeat) {
            this.width.compareTo(other.width)
        } else thisBeat.compareTo(otherBeat)
    }

    enum class UpdateCompletion {
        PENDING,
        UPDATING,
        COMPLETED,
        ;
    }

}
