package polyrhythmmania.engine

import polyrhythmmania.soundsystem.BeadsAudio
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.sample.PlayerLike


sealed class SoundInterface {
    companion object {
        fun createFromSoundSystem(soundSystem: SoundSystem?): SoundInterface =
                if (soundSystem == null) NoOp else Impl(soundSystem)
    }
    
    class Impl(val soundSystem: SoundSystem) : SoundInterface() {
        override fun playAudio(audio: BeadsAudio, callback: (player: PlayerLike) -> Unit): Long {
            return soundSystem.playAudio(audio, callback)
        }
    }

    object NoOp : SoundInterface() {
        override fun playAudio(audio: BeadsAudio, callback: (player: PlayerLike) -> Unit): Long {
            return -1L
        }
    }

    abstract fun playAudio(audio: BeadsAudio, callback: (player: PlayerLike) -> Unit = {}): Long
}