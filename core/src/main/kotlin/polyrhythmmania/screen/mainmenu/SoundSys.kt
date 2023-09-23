package polyrhythmmania.screen.mainmenu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Disposable
import net.beadsproject.beads.ugens.CrossFade
import net.beadsproject.beads.ugens.Gain
import net.beadsproject.beads.ugens.SamplePlayer
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyFloatVar
import paintbox.util.gdxutils.GdxRunnableTransition
import polyrhythmmania.PRManiaGame
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.beads.ugen.Bandpass
import polyrhythmmania.soundsystem.sample.MusicSamplePlayer


class SoundSys(private val mainMenuScreen: MainMenuScreen) : Disposable {

    private val main: PRManiaGame = mainMenuScreen.main
    private val menuMusicVolume: ReadOnlyFloatVar = mainMenuScreen.menuMusicVolume
    private var shouldBeBandpass: Boolean = false
    
    val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem(settings = SoundSystem.SoundSystemSettings()).apply {
        this.setPaused(true)
        this.audioContext.out.gain = menuMusicVolume.get()
    }
    val musicPlayer: MusicSamplePlayer = mainMenuScreen.beadsMusic.createPlayer(soundSystem.audioContext).also { player ->
        val sample = mainMenuScreen.musicSample
        player.loopStartMs = sample.samplesToMs(123_381.0).toFloat()
        player.loopEndMs = sample.samplesToMs(8_061_382.0).toFloat()
        player.loopType = SamplePlayer.LoopType.LOOP_FORWARDS
    }
    val bandpassVolume: FloatVar = FloatVar {
        if (use(main.settings.solitaireMusic)) 1f else 0f
    }
    val bandpass: Bandpass = Bandpass(soundSystem.audioContext, musicPlayer.outs)
    val bandpassGain: Gain = Gain(soundSystem.audioContext, musicPlayer.outs, bandpassVolume.get())
    val crossFade: CrossFade = CrossFade(soundSystem.audioContext, if (shouldBeBandpass) bandpassGain else musicPlayer)

    init {
        bandpass.addInput(musicPlayer)
        bandpassGain.addInput(bandpass)

        soundSystem.audioContext.out.addInput(crossFade)

        bandpassVolume.addListener {
            bandpassGain.gain = it.getOrCompute()
        }
    }

    fun start() {
        soundSystem.setPaused(false)
        soundSystem.startRealtime()
    }

    fun shutdown() {
        soundSystem.setPaused(true)
        soundSystem.stopRealtime()
    }

    fun fadeToBandpass(durationMs: Float = 1000f) {
        if (shouldBeBandpass) return
        shouldBeBandpass = true
        crossFade.fadeTo(bandpassGain, durationMs)
    }

    fun fadeToNormal(durationMs: Float = 1000f) {
        if (!shouldBeBandpass) return
        shouldBeBandpass = false
        crossFade.fadeTo(musicPlayer, durationMs)
    }

    fun resetMusic() {
        musicPlayer.gain = 1f
        musicPlayer.position = 0.0
        fadeToNormal(1f)
    }

    fun fadeMusicToSilent() {
        Gdx.app.postRunnable(GdxRunnableTransition(musicPlayer.gain.coerceIn(0f, 1f), 0f, 0.15f) { value, _ ->
            musicPlayer.gain = value
        })
    }

    override fun dispose() {
        shutdown()
        soundSystem.dispose()
    }
}