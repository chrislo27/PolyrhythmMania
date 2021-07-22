package polyrhythmmania.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import polyrhythmmania.PRManiaGame
import polyrhythmmania.soundsystem.BeadsAudio
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.sample.MusicSamplePlayer
import polyrhythmmania.soundsystem.sample.PlayerLike


sealed class SoundInterface {
    companion object {
        fun createFromSoundSystem(soundSystem: SoundSystem?): SoundInterface =
                if (soundSystem == null) NoOp else Impl(soundSystem)
    }
    
    class Impl(val soundSystem: SoundSystem) : SoundInterface() {
        private var currentAudio: BeadsMusic? = null
        private var currentMusicPlayer: MusicSamplePlayer? = null

        @Synchronized override fun getCurrentMusicPlayer(audio: BeadsMusic?): MusicSamplePlayer? {
            if (audio == currentAudio) return currentMusicPlayer
            
            // Dispose current player
            val currentPlayer = this.currentMusicPlayer
            if (currentPlayer != null) {
                currentPlayer.pause(true)
                val out = soundSystem.audioContext.out
                for (i in 0 until out.ins) {
                    out.removeConnection(i, currentPlayer, i % currentPlayer.outs)
                }
                this.currentMusicPlayer = null
            }
            
            this.currentAudio = audio
            if (audio != null) {
                val out = soundSystem.audioContext.out
                val newPlayer = audio.createPlayer(soundSystem.audioContext)
                newPlayer.pause(true)
                out.addInput(newPlayer)
                this.currentMusicPlayer = newPlayer
            }
            
            return this.currentMusicPlayer
        }

        override fun playAudio(audio: BeadsAudio, callback: (player: PlayerLike) -> Unit): Long {
            if (disableSounds) {
                return -1L
            }
            return soundSystem.playAudio(audio, callback)
        }

        override fun playMenuSfx(sound: Sound, volume: Float, pitch: Float, pan: Float) {
            Gdx.app.postRunnable {
                PRManiaGame.instance.playMenuSfx(sound, volume, pitch, pan)
            }
        }
        
        override fun setPaused(paused: Boolean) {
            soundSystem.setPaused(paused)
        }

        override fun isPaused(): Boolean = soundSystem.isPaused
    }

    object NoOp : SoundInterface() {
        private var pausedState: Boolean = false

        override fun setPaused(paused: Boolean) {
            pausedState = paused
        }

        override fun isPaused(): Boolean = pausedState

        override fun getCurrentMusicPlayer(audio: BeadsMusic?): MusicSamplePlayer? {
            return null
        }
        
        override fun playAudio(audio: BeadsAudio, callback: (player: PlayerLike) -> Unit): Long {
            return -1L
        }

        override fun playMenuSfx(sound: Sound, volume: Float, pitch: Float, pan: Float) {
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
    
    abstract fun playAudio(audio: BeadsAudio, callback: (player: PlayerLike) -> Unit = {}): Long
    
    abstract fun playMenuSfx(sound: Sound, volume: Float, pitch: Float, pan: Float)
    
    fun playMenuSfx(sound: Sound, volume: Float) = playMenuSfx(sound, volume, 1f, 0f)
    fun playMenuSfx(sound: Sound) = playMenuSfx(sound, 1f, 1f, 0f)

    fun playAudioNoOverlap(audio: BeadsAudio, callback: (player: PlayerLike) -> Unit = {}): Long {
        if (audio in audioPlayedLastFrame) return -1L
        audioPlayedLastFrame += audio
        return playAudio(audio, callback)
    }
    
    open fun update(delta: Float) {
        if (audioPlayedLastFrame.isNotEmpty()) {
            audioPlayedLastFrame.clear()
        }
    }
    
    abstract fun setPaused(paused: Boolean)
    abstract fun isPaused(): Boolean
    
}