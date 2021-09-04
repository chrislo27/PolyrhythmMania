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
    val soundInterface: SoundInterface = SoundInterface.createFromSoundSystem(soundSystem, this)
    private val _events: MutableList<Event> = CopyOnWriteArrayList()
    val events: List<Event> = _events
    val musicData: MusicData = MusicData(this)
    val timeSignatures: TimeSignatureMap = TimeSignatureMap()
    
    var endSignalReceived: Var<Boolean> = Var(false)
    
    var deleteEventsAfterCompletion: Boolean = true
    var autoInputs: Boolean = false
    var inputCalibration: InputCalibration = InputCalibration.NONE
    
    var activeTextBox: ActiveTextBox? = null
        private set
    
    fun resetEndSignal() {
        endSignalReceived.set(false)
    }
    
    fun postRunnable(runnable: Runnable) {
        queuedRunnables += runnable
    }
    
    fun addEvent(event: Event) {
        this._events += event
        this._events.sort()
    }

    fun removeEvent(event: Event) {
        this._events -= event
    }
    
    fun addEvents(events: List<Event>) {
        if (events.isEmpty()) return
        this._events.addAll(events)
        this._events.sort()
    }

    fun removeEvents(events: List<Event>) {
        if (events.isEmpty()) return
        this._events.removeAll(events)
    }
    
    fun setActiveTextbox(newTextbox: TextBox): ActiveTextBox {
        val newActive = newTextbox.toActive()
        this.activeTextBox = newActive
        if (newActive.textBox.requiresInput) {
            newActive.wasSoundInterfacePaused = soundInterface.isPaused()
            soundInterface.setPaused(true)
        }
        return newActive
    }
    
    fun removeActiveTextbox(unpauseSound: Boolean) {
        val old = activeTextBox
        activeTextBox = null
        if (unpauseSound && old != null && old.textBox.requiresInput && !old.wasSoundInterfacePaused) {
            soundInterface.setPaused(false)
        }
        old?.onComplete?.invoke(this)
    }
    
    fun updateEvent(event: Event, atBeat: Float) {
        val container = this.container
        val eventBeat = event.beat
        val eventWidth = event.width
        val eventEndBeat = eventBeat + eventWidth
        
        if (event is AudioEvent) {
            val msOffset = inputCalibration.audioOffsetMs
            val actualBeat = atBeat
            @Suppress("NAME_SHADOWING")
            val atBeat = tempos.secondsToBeats(tempos.beatsToSeconds(atBeat) - (msOffset / 1000f) * this.playbackSpeed)
            
            when (event.audioUpdateCompletion) {
                Event.UpdateCompletion.PENDING -> {
                    if (atBeat >= eventEndBeat) {
                        // Do all three updates and jump to COMPLETED
                        event.onAudioStart(atBeat, actualBeat)
                        event.onAudioUpdate(atBeat, actualBeat)
                        event.onAudioEnd(atBeat, actualBeat)
                        event.audioUpdateCompletion = Event.UpdateCompletion.COMPLETED
                    } else if (atBeat > eventBeat) {
                        // Now inside the event. Call onStart and onUpdate
                        event.onAudioStart(atBeat, actualBeat)
                        event.onAudioUpdate(atBeat, actualBeat)
                        event.audioUpdateCompletion = Event.UpdateCompletion.UPDATING
                    }
                }
                Event.UpdateCompletion.UPDATING -> {
                    event.onAudioUpdate(atBeat, actualBeat)
                    if (atBeat >= eventEndBeat) {
                        event.onAudioEnd(atBeat, actualBeat)
                        event.audioUpdateCompletion = Event.UpdateCompletion.COMPLETED
                    }
                }
                Event.UpdateCompletion.COMPLETED -> {}
            }
        }
        
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
            Event.UpdateCompletion.COMPLETED -> {}
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
                    removeActiveTextbox(true)
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
            if (!anyToDelete && event.readyToDelete()) {
                anyToDelete = true
            }
        }
        if (anyToDelete && deleteEventsAfterCompletion) {
            removeEvents(_events.filter { it.readyToDelete() })
        }
        world.engineUpdate(this, currentBeat, currentSeconds)
        
        musicData.update()
    }

    fun getDebugString(): String {
        return """TimingProvider: ${DecimalFormats.format("0.000", timingProvider.seconds)} s
Time: ${DecimalFormats.format("0.000", this.beat)} b / ${DecimalFormats.format("0.000", this.seconds)} s / Rate ${DecimalFormats.format("0.##", playbackSpeed * 100)}%
Events: ${events.size}
Inputs: ${if (inputter.areInputsLocked) "locked" else "unlocked"} | results: ${inputter.inputResults.size} | totalExpected: ${inputter.totalExpectedInputs}
Practice: ${if (inputter.practice.practiceModeEnabled) "enabled" else "disabled"} | ${inputter.practice.moreTimes} more times | [${inputter.practice.requiredInputs.joinToString(separator = ", ") { "${it.beat} ${it.inputType}${if (it.wasHit) "!" else ""}" }}]
Music: vol: ${musicData.volumeMap.volumeAtBeat(this.beat)}
""".dropLast(1)
    }

}