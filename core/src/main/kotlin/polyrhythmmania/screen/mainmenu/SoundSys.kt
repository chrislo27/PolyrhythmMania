package polyrhythmmania.screen.mainmenu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Disposable
import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.ugens.Gain
import net.beadsproject.beads.ugens.SamplePlayer
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyFloatVar
import paintbox.util.gdxutils.GdxRunnableTransition
import polyrhythmmania.PRManiaGame
import polyrhythmmania.solitaire.SolitaireMusic
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.sample.MusicSamplePlayer


class SoundSys(private val mainMenuScreen: MainMenuScreen) : Disposable {

    private val main: PRManiaGame = mainMenuScreen.main
    private val menuMusicVolume: ReadOnlyFloatVar = mainMenuScreen.menuMusicVolume
    
    val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem(settings = SoundSystem.SoundSystemSettings()).apply {
        this.setPaused(true)
        this.audioContext.out.gain = menuMusicVolume.get()
    }
    private val audioContext: AudioContext = soundSystem.audioContext
    
    val titleMusicPlayer: MusicSamplePlayer = mainMenuScreen.beadsMusic.createPlayer(audioContext).also { player ->
        val sample = mainMenuScreen.musicSample
        player.loopStartMs = sample.samplesToMs(123_381.0).toFloat()
        player.loopEndMs = sample.samplesToMs(8_061_382.0).toFloat()
        player.loopType = SamplePlayer.LoopType.LOOP_FORWARDS
    }
    
    private val solitaireMusicVolume: ReadOnlyFloatVar = FloatVar {
        (if (bindAndGet(main.settings.solitaireMusic)) 1f else 0f) * solitaireMusicMultiplier.use()
    }
    val solitaireMusicMultiplier: FloatVar = FloatVar(1f) // Used to attenuate music when win sound played
    private val solitaireGain: Gain = Gain(audioContext, 2, solitaireMusicVolume.get())
    private var isSolitaireMusicPlayerLoaded: Boolean = false
    private val solitaireMusicPlayer: MusicSamplePlayer by lazy {
        SolitaireMusic.createMusicPlayer(audioContext).also { player ->
            solitaireGain.addInput(player)
            isSolitaireMusicPlayerLoaded = true
        }
    }

    init {
        audioContext.out.addInput(titleMusicPlayer)
        
        solitaireMusicVolume.addListener {
            solitaireGain.gain = it.getOrCompute()
        }
        audioContext.out.addInput(solitaireGain)
    }

    fun start() {
        soundSystem.setPaused(false)
        soundSystem.startRealtime()
    }

    fun shutdown() {
        soundSystem.setPaused(true)
        soundSystem.stopRealtime()
    }

    fun fadeToSilent() {
        titleMusicPlayer.fadePlayer(0f, 0.15f)
    }

    fun resetMusic() {
        titleMusicPlayer.gain = 1f
        titleMusicPlayer.position = 0.0
        titleMusicPlayer.fadePlayer(1f, 0f)
    }
    
    fun fadeOutTitleForSolitaire() {
        val fadeOutDuration = 0.5f
        titleMusicPlayer.fadePlayer(0f, fadeOutDuration)
    }
    
    fun playSolitaireMusic() {
        solitaireMusicPlayer.position = -1000.0
        solitaireMusicPlayer.gain = 1f
        solitaireMusicPlayer.pause(false)
    }
    
    fun fadeToTitle() {
        if (isSolitaireMusicPlayerLoaded) {
            solitaireMusicPlayer.fadePlayer(0f, 0.25f, pauseAtEnd = true)
        }

        titleMusicPlayer.fadePlayer(1f, 1f)
    }

    override fun dispose() {
        shutdown()
        soundSystem.dispose()
    }

    private fun MusicSamplePlayer.fadePlayer(targetGain: Float, durationSec: Float, startAtOppositeGain: Boolean = false, pauseAtEnd: Boolean = false) {
        val startGain = (if (startAtOppositeGain) (1f - targetGain) else this.gain).coerceIn(0f, 1f)
        Gdx.app.postRunnable(GdxRunnableTransition(startGain, targetGain, durationSec) { value, progress ->
            this.gain = value
            if (pauseAtEnd && progress >= 1f) {
                this.pause(true)
            }
        })
    }
}