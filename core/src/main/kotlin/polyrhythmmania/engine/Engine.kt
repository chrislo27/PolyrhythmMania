package polyrhythmmania.engine

import polyrhythmmania.engine.input.EngineInputter
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

    private val queuedRunnables: MutableList<Runnable> = CopyOnWriteArrayList()
    
    val inputter: EngineInputter = EngineInputter(this)
    val soundInterface: SoundInterface = SoundInterface.createFromSoundSystem(soundSystem)
    private val _events: MutableList<Event> = CopyOnWriteArrayList()
    val events: List<Event> = _events
    val musicData: MusicData = MusicData(this)
    
    var autoInputs: Boolean = false
    
    fun postRunnable(runnable: Runnable) {
        queuedRunnables += runnable
    }
    
    fun addEvent(event: Event) {
        this._events += event
    }

    fun removeEvent(event: Event) {
        this._events -= event
    }
    
    fun addEvents(events: List<Event>) {
        this._events.addAll(events)
    }

    fun removeEvents(events: List<Event>) {
        this._events.removeAll(events)
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
        
        if (queuedRunnables.isNotEmpty()) {
            val toList = queuedRunnables.toList()
            toList.forEach { it.run() }
            queuedRunnables.clear()
        }
        
        events.forEach { event ->
            updateEvent(event, currentBeat)
        }
        world.engineUpdate(this, currentBeat, currentSeconds)
        
        musicData.update()
    }

    fun getDebugString(): String {
        return """TimingProvider: ${DecimalFormats.format("0.000", timingProvider.seconds)} s
Time: ${DecimalFormats.format("0.000", this.beat)} b / ${DecimalFormats.format("0.000", this.seconds)} s
Events: ${events.size}
Inputs: ${if (inputter.areInputsLocked) "locked" else "unlocked"} | ${inputter.totalExpectedInputs}
""".dropLast(1)
    }

}