package polyrhythmmania.engine

import com.badlogic.gdx.math.MathUtils
import net.beadsproject.beads.ugens.SamplePlayer
import polyrhythmmania.engine.music.MusicVolMap
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.LoopParams


class MusicData(val engine: Engine) {

    val volumeMap: MusicVolMap = MusicVolMap()
    var loopParams: LoopParams = LoopParams.NO_LOOP_FORWARDS
    var firstBeatSec: Float = 0f
    var musicSyncPointBeat: Float = 0f
    var rate: Float = 1f

    var beadsMusic: BeadsMusic? = null

    fun update() {
        val currentBeat = engine.beat
        val music = this.beadsMusic
        val player = engine.soundInterface.getCurrentMusicPlayer(music)
        if (player != null) {
            val volume = volumeMap.volumeAtBeat(currentBeat)
            player.gain = volume / 100f
            player.pitch = rate * engine.playbackSpeed

            if (!player.isPaused && !player.context.out.isPaused) {
                // Player desync correction
                val currentSeconds = engine.seconds
                val correctPosition = getCorrectMusicPlayerPositionAt(currentSeconds)
                val currentPosition = player.position
                if (!MathUtils.isEqual(correctPosition.toFloat(), currentPosition.toFloat(), 50f /* ms */)) {
                    player.position = correctPosition
//                    Paintbox.LOGGER.debug("[MusicData] Manually adjusted player due to desync: was $currentPosition, now $correctPosition")
                }
            }
        }
    }
    
    fun setMusicPlayerPositionToCurrentSec() {
        val music = this.beadsMusic
        val player = engine.soundInterface.getCurrentMusicPlayer(music)
        if (player != null) {
            player.useLoopParams(this.loopParams)
            player.position = getCorrectMusicPlayerPositionAt(engine.seconds)
        }
    }

    fun getCorrectMusicPlayerPositionAt(atSeconds: Float,
                                        delaySec: Float = engine.musicData.computeMusicDelaySec()
    ): Double {
        val music = this.beadsMusic
        val player = engine.soundInterface.getCurrentMusicPlayer(music)
        if (player != null) {
            if (atSeconds < delaySec) {
                // Set player position to be negative
                return (atSeconds - delaySec).toDouble() * 1000.0 * rate
            } else {
                if (player.loopType == SamplePlayer.LoopType.LOOP_FORWARDS && !player.isLoopInvalid()) {
                    player.prepareStartBuffer()
                    val adjustedSecs = atSeconds - delaySec
                    val loopStart = player.loopStartMs
                    val loopEnd = player.loopEndMs
                    val loopDuration = (loopEnd - loopStart)

                    return if (adjustedSecs < loopEnd / 1000) {
                        (adjustedSecs * 1000 * rate) % player.musicSample.lengthMs
                    } else {
                        ((((adjustedSecs * 1000 - loopStart) * rate) % loopDuration + loopStart * rate)) % player.musicSample.lengthMs
                    }
                } else {
                    return ((atSeconds - delaySec) * 1000.0) * rate
                }
            }
        }

        return 0.0
    }
    
    fun computeMusicDelaySec(): Float {
        return ((engine.tempos.beatsToSeconds(this.musicSyncPointBeat) - this.firstBeatSec) * 1000 + engine.musicOffsetMs) / 1000
    }

}