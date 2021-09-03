package polyrhythmmania.soundsystem

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Disposable
import paintbox.Paintbox
import paintbox.util.Sync
import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.IOAudioFormat
import net.beadsproject.beads.ugens.Gain
import javax.sound.sampled.*
import polyrhythmmania.soundsystem.beads.*
import polyrhythmmania.soundsystem.beads.ugen.Delay
import polyrhythmmania.soundsystem.sample.PlayerLike
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import kotlin.math.abs


/**
 * A wrapper for an [net.beadsproject.beads.core.AudioContext] and associated audio system utilities, using
 * [DaemonJavaSoundAudioIO] as the audio IO interface.
 */
class SoundSystem(private val mixer: Mixer,
                  val ioAudioFormat: IOAudioFormat = DEFAULT_AUDIO_FORMAT,
                  bufferSize: Int = AudioContext.DEFAULT_BUFFER_SIZE,
                  val settings: SoundSystemSettings = SoundSystemSettings())
    : Disposable, TimingProvider {

    companion object {
        val AUDIO_FORMAT_44100: IOAudioFormat = IOAudioFormat(44_100f, 16, 2, 2, true, true)
        val AUDIO_FORMAT_48000: IOAudioFormat = IOAudioFormat(48_000f, 16, 2, 2, true, true)
        val DEFAULT_AUDIO_FORMAT: IOAudioFormat = AUDIO_FORMAT_48000
        
        val defaultMixerHandler: MixerHandler by lazy { MixerHandler(DEFAULT_AUDIO_FORMAT.toJavaAudioFormat()) }

        fun createDefaultSoundSystem(bufferSize: Int = AudioContext.DEFAULT_BUFFER_SIZE,
                                     settings: SoundSystemSettings = SoundSystemSettings()): SoundSystem {
            return createDefaultSoundSystem(defaultMixerHandler, bufferSize, settings)
        }
        
        fun createDefaultSoundSystem(mixerHandler: MixerHandler,
                                     bufferSize: Int = AudioContext.DEFAULT_BUFFER_SIZE,
                                     settings: SoundSystemSettings = SoundSystemSettings()): SoundSystem {
            val mixer = mixerHandler.recommendedMixer
            return SoundSystem(mixer, mixerHandler.audioFormat.toBeadsAudioFormat(), bufferSize, settings)
        }
    }
    
    data class SoundSystemSettings(val reportAdaptiveSync: Boolean = true)

    val audioContext: AudioContext =
            AudioContext(DaemonJavaSoundAudioIO(mixer), bufferSize, ioAudioFormat)
//            object : AudioContext(DaemonJavaSoundAudioIO(mixer), bufferSize, ioAudioFormat) {
//                init { // For testing delays
//                    run {
//                        val field = AudioContext::class.java.getDeclaredField("out")
//                        field.isAccessible = true
//                        field.set(this, Delay(this, this.out.outs, bufferSize * 10))
//                    }
//                }
//            }

    @Volatile
    private var currentlyRealTime: Boolean = true
    
    @Volatile
    private var currentSoundID: Long = 0L
    private val activePlayers: MutableMap<Long, PlayerLike> = ConcurrentHashMap()
    
    @Volatile
    private var isDisposed: Boolean = false
    private val timingProvider: AdaptiveTimingProvider = this.AdaptiveTimingProvider()

    override var seconds: Float
        get() = timingProvider.seconds
        set(value) {
            timingProvider.seconds = value
        }
    override val listeners: MutableList<TimingListener> by timingProvider::listeners

    val isPaused: Boolean
        get() = audioContext.out.isPaused

    init {
        audioContext.out.pause(true)
        audioContext.out.addDependent(timingProvider.timingBead)
    }

    fun setPaused(paused: Boolean) {
        audioContext.out.pause(paused)
        if (paused) {
            timingProvider.forceSyncWithBead()
        }
    }

    fun startRealtime() {
        currentlyRealTime = true
        audioContext.start()
    }

    fun stopRealtime() {
        audioContext.stop()
        currentlyRealTime = false
    }

    fun startNonrealtimeTimed(msToRun: Double) {
        currentlyRealTime = false
        audioContext.runForNMillisecondsNonRealTime(msToRun)
    }
    
    fun isCurrentlyRealtime(): Boolean = currentlyRealTime

    override fun dispose() {
        isDisposed = true
        stopRealtime()
        audioContext.out.clearInputConnections()
        audioContext.out.clearDependents()
    }

    override fun exceptionHandler(throwable: Throwable): Boolean {
        return timingProvider.exceptionHandler(throwable)
    }

    private fun obtainSoundID(): Long = ++currentSoundID
    
    fun playAudio(beadsAudio: BeadsAudio, callback: (player: PlayerLike) -> Unit = {}): Long {
        val id = obtainSoundID()
        val player = beadsAudio.createPlayer(audioContext)
        player.killListeners += {
            activePlayers.remove(id, it)
        }
        callback.invoke(player)
        activePlayers[id] = player
        audioContext.out.addInput(player)
        return id
    }
    
    fun getPlayer(id: Long): PlayerLike? = activePlayers[id]

    private inner class AdaptiveTimingProvider : TimingProvider {
        @Volatile
        override var seconds: Float = 0f
        override val listeners: MutableList<TimingListener> = CopyOnWriteArrayList()

        val timingBead: TimingBead = TimingBead(audioContext)
        val realtimeThread: Thread

        private val reportWhenDesynced: Boolean = settings.reportAdaptiveSync
        
        init {
            timingBead.listeners += TimingListener { oldSeconds, newSeconds ->
                try {
                    if (!currentlyRealTime && !isPaused) {
                        this.seconds = newSeconds
                        this.onUpdate(oldSeconds, newSeconds)
                    }
                } catch (t: Throwable) {
                    exceptionHandler(t)
                }
            }
            realtimeThread = thread(start = true, isDaemon = true, name = "AdaptiveTimingProvider", priority = Thread.MAX_PRIORITY) {
                try {
                    val pollRateHz = 1.0 / (512.0 / 44100.0)//100
                    val forceSyncThreshold = 0.030f
                    val sync = Sync()

                    var nano = System.nanoTime()
                    while (!this@SoundSystem.isDisposed) {
                        sync.sync(pollRateHz)

                        // Action
                        if (currentlyRealTime && !isPaused) {
                            val secondsDiff = (1.0 / pollRateHz).toFloat()
                            val oldSeconds = this.seconds
                            this.seconds += secondsDiff
                            
                            if (abs(this.seconds - timingBead.seconds) >= forceSyncThreshold) {
                                if (reportWhenDesynced) {
                                    Paintbox.LOGGER.debug("AdaptiveTimingProvider Force sync: this was ${this.seconds} and TimingBead was ${timingBead.seconds} (delta ${abs(this.seconds - timingBead.seconds)}, force sync threshold ${forceSyncThreshold})")
                                }
                                // Force sync with timing bead if off by too much
                                this.seconds = timingBead.seconds
                            }
                            
                            this.onUpdate(oldSeconds, this.seconds)

//                            val ns = System.nanoTime() - nano
//                            println("Expected ${1000.0 / pollRateHz}, actual ${ns / 1_000_000.0}  delta ${(1000.0 / pollRateHz) - (ns / 1_000_000.0)}")
//                            nano = System.nanoTime()
                        }
                    }
//                    Paintbox.LOGGER.debug("End of AdaptiveTimingProvider, dying.")
                } catch (t: Throwable) {
                    Paintbox.LOGGER.debug("AdaptiveTimingProvider thread encountered an exception")
                    t.printStackTrace()
                    exceptionHandler(t)
                }
            }
        }
        
        fun forceSyncWithBead() {
            val oldSeconds = this.seconds
            this.seconds = timingBead.seconds
            this.onUpdate(oldSeconds, this.seconds)
        }

        override fun exceptionHandler(throwable: Throwable): Boolean {
            Gdx.app.postRunnable { 
                throw throwable
            }
            return false
        }
    }
}