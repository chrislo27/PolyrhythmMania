package polyrhythmmania.engine

import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.util.DecimalFormats
import polyrhythmmania.world.World
import java.util.concurrent.CopyOnWriteArrayList


/**
 * An [Engine] fires the [Event]s based on the internal [TimingProvider].
 * It also contains the [World] upon which these events operate in.
 */
class Engine(timingProvider: TimingProvider, val world: World, soundSystem: SoundSystem?)
    : Clock(timingProvider) {

    val soundInterface: SoundInterface = SoundInterface.createFromSoundSystem(soundSystem)
    val events: List<Event> = CopyOnWriteArrayList()

    fun addEvent(event: Event) {
        (this.events as MutableList) += event
    }

    fun removeEvent(event: Event) {
        (this.events as MutableList) -= event
    }
    
    fun addEvents(events: List<Event>) {
        (this.events as MutableList).addAll(events)
    }

    fun removeEvents(events: List<Event>) {
        (this.events as MutableList).removeAll(events)
    }
    
    fun updateEvent(event: Event, atBeat: Float) {
        val eventBeat = event.beat
        val eventWidth = event.width
        val eventEndBeat = eventBeat + eventWidth
        when (event.updateCompletion) {
            Event.UpdateCompletion.PENDING -> {
                if (atBeat >= eventEndBeat) {
                    // Do all three updates and jump to COMPLETED
                    event.onStart(atBeat)
                    event.onUpdate(atBeat)
                    event.onEnd(atBeat)
                    event.updateCompletion = Event.UpdateCompletion.COMPLETED
                } else if (atBeat > eventBeat) {
                    // Now inside the event. Call onStart and onUpdate
                    event.onStart(atBeat)
                    event.onUpdate(atBeat)
                    event.updateCompletion = Event.UpdateCompletion.UPDATING
                }
            }
            Event.UpdateCompletion.UPDATING -> {
                event.onUpdate(atBeat)
                if (atBeat >= eventEndBeat) {
                    event.onEnd(atBeat)
                    event.updateCompletion = Event.UpdateCompletion.COMPLETED
                }
            }
            Event.UpdateCompletion.COMPLETED -> return
        }
    }

    override fun updateSeconds(delta: Float) {
        super.updateSeconds(delta)
        val currentSeconds = this.seconds
        val currentBeat = this.beat
        events.forEach { event ->
            updateEvent(event, currentBeat)
        }
        world.engineUpdate(this, currentBeat, currentSeconds)
    }

    fun getDebugString(): String {
        return """TimingProvider: ${DecimalFormats.format("0.000", timingProvider.seconds)} s
Time: ${DecimalFormats.format("0.000", this.beat)} b / ${DecimalFormats.format("0.000", this.seconds)} s
Events: ${events.size}
""".dropLast(1)
    }

}