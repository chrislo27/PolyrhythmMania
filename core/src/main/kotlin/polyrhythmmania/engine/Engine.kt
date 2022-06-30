package polyrhythmmania.engine

import com.codahale.metrics.*
import paintbox.binding.BooleanVar
import polyrhythmmania.PRMania
import polyrhythmmania.container.Container
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.engine.timesignature.TimeSignatureMap
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.TimingProvider
import paintbox.util.DecimalFormats
import polyrhythmmania.engine.modifiers.EngineModifiers
import polyrhythmmania.util.metrics.timeInline
import polyrhythmmania.world.World
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit


/**
 * An [Engine] fires the [Event]s based on the internal [TimingProvider].
 * It also contains the [World] upon which these events operate in.
 */
class Engine(timingProvider: TimingProvider,
             val world: World, soundSystem: SoundSystem?, val container: Container?,
             disableMetricsReporting: Boolean = false)
    : Clock(timingProvider) {
    
    val metrics: MetricRegistry = MetricRegistry()
    val metricsEnabled: Boolean = PRMania.enableMetrics && !disableMetricsReporting
    val metricsReporter: ScheduledReporter = ConsoleReporter.forRegistry(this.metrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build()
    private val timerUpdateSeconds: Timer = this.metrics.timer("engine.updateSeconds")
    private val timerUpdateEvents: Timer = this.metrics.timer("engine.updateEvents")
    private val timerUpdateWorld: Timer = this.metrics.timer("engine.updateWorld")

    private val queuedRunnables: MutableList<Runnable> = CopyOnWriteArrayList()
    val inputter: EngineInputter = EngineInputter(this)
    val modifiers: EngineModifiers = EngineModifiers(this)
    val soundInterface: SoundInterface = SoundInterface.createFromSoundSystem(soundSystem, this)
    private val _events: MutableList<Event> = CopyOnWriteArrayList()
    val events: List<Event> = _events
    val musicData: MusicData = MusicData(this)
    val timeSignatures: TimeSignatureMap = TimeSignatureMap()
    
    var endSignalReceived: BooleanVar = BooleanVar(false)
    
    var deleteEventsAfterCompletion: Boolean = true
    var autoInputs: Boolean = false
    var inputCalibration: InputCalibration = InputCalibration.NONE
    var statisticsMode: StatisticsMode = StatisticsMode.REGULAR
    
    val areStatisticsEnabled: Boolean
        get() = statisticsMode == StatisticsMode.REGULAR && !autoInputs
    
    var activeTextBox: ActiveTextBox? = null
        private set
    var resultFlag: ResultFlag = ResultFlag.NONE
    
    init {
        // Flush metrics when end signal received
        endSignalReceived.addListener {
            if (it.getOrCompute() && this.metricsEnabled) {
                metricsReporter.report()
            }
        }
    }

    /**
     * Resets all mutable state within this [Engine].
     */
    fun resetMutableState() {
        this.removeEvents(this.events.toList())
        this.resultFlag = ResultFlag.NONE
        this.inputter.areInputsLocked = this.autoInputs
        this.inputter.resetState()
        this.modifiers.resetState()
        this.musicData.resetState()
        this.soundInterface.clearAllNonMusicAudio()
        this.removeActiveTextbox(unpauseSoundInterface = false, runTextboxOnComplete = false)
        this.resetEndSignal()
    }
    
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
    
    fun removeActiveTextbox(unpauseSoundInterface: Boolean, runTextboxOnComplete: Boolean) {
        val old = activeTextBox
        activeTextBox = null
        if (unpauseSoundInterface && old != null && old.textBox.requiresInput && !old.wasSoundInterfacePaused) {
            soundInterface.setPaused(false)
        }
        if (runTextboxOnComplete) {
            old?.onComplete?.invoke(this)
        }
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
                val endlessScore = modifiers.endlessScore
                if (autoInputs && (!endlessScore.enabled.get() || endlessScore.lives.get() > 0)) {
                    removeActiveTextbox(unpauseSoundInterface = true, runTextboxOnComplete = true)
                }
            }
        } else {
            timerUpdateSeconds.timeInline {
                super.updateSeconds(delta)
            }
        }
        
        val currentSeconds = this.seconds
        val currentBeat = this.beat
        
        if (queuedRunnables.isNotEmpty()) {
            val toList = queuedRunnables.toList()
            toList.forEach { it.run() }
            queuedRunnables.clear()
        }

        soundInterface.update(delta)

        timerUpdateEvents.timeInline {
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
        }
        
        timerUpdateWorld.timeInline {
            world.engineUpdate(this, currentBeat, currentSeconds)
        }
        
        musicData.update()
    }

    fun getDebugString(): String {
        return """TimingProvider: ${DecimalFormats.format("0.000", timingProvider.seconds)} s
Time: ${DecimalFormats.format("0.000", this.beat)} b / ${DecimalFormats.format("0.000", this.seconds)} s / BPM ${DecimalFormats.format("0.00", tempos.tempoAtBeat(this.beat))} / Rate ${DecimalFormats.format("0.##", playbackSpeed * 100)}%
Events: ${events.size}
Inputs: ${if (inputter.areInputsLocked) "locked" else "unlocked"} | results: ${inputter.inputResults.size} | totalExpected: ${inputter.totalExpectedInputs}
Practice: ${if (inputter.practice.practiceModeEnabled) "enabled" else "disabled"} | ${inputter.practice.moreTimes} more times | [${inputter.practice.requiredInputs.joinToString(separator = ", ") { "${it.beat} ${it.inputType}${if (it.wasHit) "!" else ""}" }}]
Music: vol: ${musicData.volumeMap.volumeAtBeat(this.beat)}
""".dropLast(1)
    }

}