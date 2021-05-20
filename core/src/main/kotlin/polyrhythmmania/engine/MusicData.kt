package polyrhythmmania.engine

import polyrhythmmania.engine.music.MusicVolMap
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.MusicSamplePlayer


class MusicData(val engine: Engine) {

    var musicDelaySec: Float = 0f
    val volumeMap: MusicVolMap = MusicVolMap()

    var beadsMusic: BeadsMusic? = null

    fun update() {
        val currentBeat = engine.beat
        val music = this.beadsMusic
        val player = engine.soundInterface.getCurrentMusicPlayer(music)
        if (player != null) {
            val volume = volumeMap.volumeAtBeat(currentBeat)
            player.gain = volume / 100f
        }
    }

}