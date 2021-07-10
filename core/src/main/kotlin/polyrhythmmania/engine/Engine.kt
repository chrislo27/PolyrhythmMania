package polyrhythmmania.engine

import paintbox.binding.Var
import polyrhythmmania.container.Container
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.engine.timesignature.TimeSignatureMap
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.util.DecimalFormats
import polyrhythmmania.world.World
import java.util.concurrent.CopyOnWriteArrayList


/**
 * An [Engine] fires the [Event]s based on the internal [TimingProvider].
 * It also contains the [World] upon which these events operate in.
 */
class Engine(timingProvider: TimingProvider, val world: World, soundSystem: SoundSystem?, val container: Container?)
    : Clock(timingProvider) {

    private val queuedRunnables: MutableList<Runnable> = CopyOnWriteArrayList()
    
    val inputter: EngineInputter = EngineInputter(this)
    val soundInterface: SoundInterface = SoundInterface.createFromSoundSystem(soundSystem)
    private val _events: MutableList<Event> = CopyOnWriteArrayList()
    val events: List<Event> = _events
    val musicData: MusicData = MusicData(this)
    val timeSignatures: TimeSignatureMap = TimeSignatureMap()
    
    var endSignalReceived: Var<Boolean> = Var(false)
    
    var deleteEventsAfterCompletion: Boolean = true
    var autoInputs: Boolean = false
    var musicOffsetMs: Float = 0f
    
    var activeTextBox: ActiveTextBox? = null
        set(value) {
            val old = field
            field = value
            if (value != null && value.textBox.requiresInput) {
                value.wasSoundInterfacePaused = soundInterface.pausedState
                soundInterface.setPaused(true)
            } else {
                if (old != null && old.textBox.requiresInput && !old.wasSoundInterfacePaused) {
                    soundInterface.setPaused(false)
                }
            }
        }
    
    fun resetEndSignal() {
        endSignalReceived.set(false)
    }
    
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
        if (events.isEmpty()) return
        this._events.addAll(events)
    }

    fun removeEvents(events: List<Event>) {
        if (events.isEmpty()) return
        this._events.removeAll(events)
    }
    
    fun updateEvent(event: Event, atBeat: Float) {
        val container = this.container
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
                    if (container != null) {
                        event.onStartContainer(container, atBeat)
                        event.onUpdateContainer(container, atBeat)
                        event.onEndContainer(container, atBeat)
                    }
                    event.updateCompletion = Event.UpdateCompletion.COMPLETED
                } else if (atBeat > eventBeat) {
                    // Now inside the event. Call onStart and onUpdate
                    event.onStart(atBeat)
                    event.onUpdate(atBeat)
                    if (container != null) {
                        event.onStartContainer(container, atBeat)
                        event.onUpdateContainer(container, atBeat)
                    }
                    event.updateCompletion = Event.UpdateCompletion.UPDATING
                }
            }
            Event.UpdateCompletion.UPDATING -> {
                event.onUpdate(atBeat)
                if (container != null) event.onUpdateContainer(container, atBeat)
                if (atBeat >= eventEndBeat) {
                    event.onEnd(atBeat)
                    if (container != null) event.onEndContainer(container, atBeat)
                    event.updateCompletion = Event.UpdateCompletion.COMPLETED
                }
            }
            Event.UpdateCompletion.COMPLETED -> return
        }
    }

    override fun updateSeconds(delta: Float) {
        val activeTextBox = this.activeTextBox
        if (activeTextBox != null && activeTextBox.textBox.requiresInput) {
            if (activeTextBox.secondsTimer > 0f) {
                activeTextBox.secondsTimer -= delta
            }
            if (activeTextBox.secondsTimer <= 0f) {
                if (autoInputs) {
                    this.activeTextBox = null
                }
            }
        } else {
            super.updateSeconds(delta)
        }
        
        val currentSeconds = this.seconds
        val currentBeat = this.beat
        
        if (queuedRunnables.isNotEmpty()) {
            val toList = queuedRunnables.toList()
            toList.forEach { it.run() }
            queuedRunnables.clear()
        }
        
        soundInterface.update(delta)
        
        var anyToDelete = false
        events.forEach { event ->
            updateEvent(event, currentBeat)
            if (!anyToDelete && event.updateCompletion == Event.UpdateCompletion.COMPLETED) {
                anyToDelete = true
            }
        }
        if (anyToDelete && deleteEventsAfterCompletion) {
            removeEvents(_events.filter { it.updateCompletion == Event.UpdateCompletion.COMPLETED })
        }
        world.engineUpdate(this, currentBeat, currentSeconds)
        
        musicData.update()
    }

    fun getDebugString(): String {
        return """TimingProvider: ${DecimalFormats.format("0.000", timingProvider.seconds)} s
Time: ${DecimalFormats.format("0.000", this.beat)} b / ${DecimalFormats.format("0.000", this.seconds)} s
Events: ${events.size}
Inputs: ${if (inputter.areInputsLocked) "locked" else "unlocked"} | results: ${inputter.inputResults.size} | totalExpected: ${inputter.totalExpectedInputs}
Practice: ${if (inputter.practice.practiceModeEnabled) "enabled" else "disabled"} | ${inputter.practice.moreTimes} more times | [${inputter.practice.requiredInputs.joinToString(separator = ", ") { "${it.beat} ${it.inputType}${if (it.wasHit) "!" else ""}" }}]
Music: vol: ${musicData.volumeMap.volumeAtBeat(this.beat)}
""".dropLast(1)
    }

}