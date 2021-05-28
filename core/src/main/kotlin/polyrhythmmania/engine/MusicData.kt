package polyrhythmmania.engine

import io.github.chrislo27.paintbox.binding.Var
import net.beadsproject.beads.ugens.SamplePlayer
import polyrhythmmania.engine.music.MusicVolMap
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.soundsystem.sample.MusicSamplePlayer


class MusicData(val engine: Engine) {

    var musicDelaySec: Float = 0f
    val volumeMap: MusicVolMap = MusicVolMap()
    var loopParams: LoopParams = LoopParams.NO_LOOP_FORWARDS

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
    
    fun setPlayerPositionToCurrentSec() {
        val music = this.beadsMusic
        val player = engine.soundInterface.getCurrentMusicPlayer(music)
        if (player != null) {
            val newSeconds = engine.seconds
            val delaySec = engine.musicData.musicDelaySec
            if (newSeconds < delaySec) {
                // Set player position to be negative
                player.position = (newSeconds - delaySec).toDouble() * 1000.0
            } else {
                if (player.loopType == SamplePlayer.LoopType.LOOP_FORWARDS && !player.isLoopInvalid()) {
                    player.prepareStartBuffer()
                    val adjustedSecs = newSeconds - delaySec
                    val loopStart = player.loopStartMs
                    val loopEnd = player.loopEndMs
                    val loopDuration = (player.loopEndMs - player.loopStartMs)

                    if (adjustedSecs < loopEnd / 1000) {
                        player.position = (adjustedSecs * 1000) % player.musicSample.lengthMs
                    } else {
                        player.position = (((adjustedSecs * 1000 - loopStart) % loopDuration + loopStart)) % player.musicSample.lengthMs
                    }
                } else {
                    player.position = ((newSeconds - delaySec) * 1000) % player.musicSample.lengthMs
                }
            }
        }
    }

}