package polyrhythmmania.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import net.beadsproject.beads.core.UGen
import net.beadsproject.beads.ugens.Gain
import polyrhythmmania.PRManiaGame
import polyrhythmmania.soundsystem.BeadsAudio
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.sample.MusicSamplePlayer
import polyrhythmmania.soundsystem.sample.PlayerLike


sealed class SoundInterface {
    companion object {
        fun createFromSoundSystem(soundSystem: SoundSystem?, engine: Engine): SoundInterface =
                if (soundSystem == null) NoOp else Impl(soundSystem, engine)
    }
    
    enum class SFXType {
        NORMAL, PLAYER_INPUT, DYNAMIC_MUSIC
    }
    
    class Impl(val soundSystem: SoundSystem, val engine: Engine) : SoundInterface() {
        private val globalGain: Gain = Gain(soundSystem.audioContext, soundSystem.audioContext.out.outs)
        
        private var currentAudio: BeadsMusic? = null
        private var currentMusicPlayer: MusicSamplePlayer? = null
        
        init {
            globalGain.gain = 1f
            soundSystem.audioContext.out.addInput(globalGain)
        }

        @Synchronized override fun getCurrentMusicPlayer(audio: BeadsMusic?): MusicSamplePlayer? {
            if (audio == currentAudio) return currentMusicPlayer

            val ugenToAddTo: UGen = globalGain
            
            // Dispose current player
            val currentPlayer = this.currentMusicPlayer
            if (currentPlayer != null) {
                currentPlayer.pause(true)
                for (i in 0..<ugenToAddTo.ins) {
                    ugenToAddTo.removeConnection(i, currentPlayer, i % currentPlayer.outs)
                }
                this.currentMusicPlayer = null
            }
            
            this.currentAudio = audio
            if (audio != null) {
                val newPlayer = audio.createPlayer(soundSystem.audioContext)
                newPlayer.pause(true)
                ugenToAddTo.addInput(newPlayer)
                this.currentMusicPlayer = newPlayer
            }
            
            return this.currentMusicPlayer
        }

        override fun playAudio(audio: BeadsAudio, type: SFXType, callback: (player: PlayerLike) -> Unit): Long {
            if (disableSounds) {
                return -1L
            }
            if (type == SFXType.PLAYER_INPUT && engine.inputCalibration.disableInputSounds) {
                return -1L
            }
            
            return soundSystem.playAudio(audio, globalGain, callback)
        }

        override fun playMenuSfx(sound: Sound, volume: Float, pitch: Float, pan: Float) {
            Gdx.app.postRunnable {
                PRManiaGame.instance.playMenuSfx(sound, volume, pitch, pan)
            }
        }

        override fun clearAllNonMusicAudio() {
            val currentMusic = currentMusicPlayer
            val out = globalGain
            out.clearInputConnections()
            if (currentMusic != null) {
                out.addInput(currentMusic)
            }
        }

        override fun setGlobalGain(gain: Float) {
            globalGain.gain = gain
        }

        override fun getPlayer(id: Long): PlayerLike? = soundSystem.getPlayer(id)

        override fun setPaused(paused: Boolean) {
            soundSystem.setPaused(paused)
        }

        override fun isPaused(): Boolean = soundSystem.isPaused
    }

    data object NoOp : SoundInterface() {
        private var pausedState: Boolean = false

        override fun setPaused(paused: Boolean) {
            pausedState = paused
        }

        override fun isPaused(): Boolean = pausedState

        override fun getCurrentMusicPlayer(audio: BeadsMusic?): MusicSamplePlayer? {
            return null
        }
        
        override fun playAudio(audio: BeadsAudio, type: SFXType, callback: (player: PlayerLike) -> Unit): Long {
            return -1L
        }

        override fun playMenuSfx(sound: Sound, volume: Float, pitch: Float, pan: Float) {
        }

        override fun clearAllNonMusicAudio() {
        }

        override fun getPlayer(id: Long): PlayerLike? = null
        
        override fun setGlobalGain(gain: Float) {
        }
    }
    
    open var disableSounds: Boolean = false
    
    private val audioPlayedLastFrame: MutableSet<BeadsAudio> = mutableSetOf()

    /**
     * Gets the current [MusicSamplePlayer] for the given music [audio]. It is up to each implementation
     * to handle adding/removing/disposing of internal players.
     * 
     * A null [audio] should force an implementor to stop and remove the current music player, if any.
     * 
     * A newly created [MusicSamplePlayer] will always be paused at the start.
     */
    abstract fun getCurrentMusicPlayer(audio: BeadsMusic?): MusicSamplePlayer?
    
    abstract fun playAudio(audio: BeadsAudio, type: SFXType, callback: (player: PlayerLike) -> Unit = {}): Long
    
    abstract fun playMenuSfx(sound: Sound, volume: Float, pitch: Float, pan: Float)
    
    abstract fun clearAllNonMusicAudio()
    
    abstract fun getPlayer(id: Long): PlayerLike?
    
    abstract fun setGlobalGain(gain: Float)
    
    fun playMenuSfx(sound: Sound, volume: Float) = playMenuSfx(sound, volume, 1f, 0f)
    fun playMenuSfx(sound: Sound) = playMenuSfx(sound, 1f, 1f, 0f)

    fun playAudioNoOverlap(audio: BeadsAudio, type: SFXType, callback: (player: PlayerLike) -> Unit = {}): Long {
        if (audio in audioPlayedLastFrame) return -1L
        audioPlayedLastFrame += audio
        return playAudio(audio, type, callback)
    }
    
    open fun update(delta: Float) {
        if (audioPlayedLastFrame.isNotEmpty()) {
            audioPlayedLastFrame.clear()
        }
    }
    
    open fun resetMutableState() {
        this.clearAllNonMusicAudio()
        this.setGlobalGain(1f)
    }
    
    abstract fun setPaused(paused: Boolean)
    abstract fun isPaused(): Boolean
    
}