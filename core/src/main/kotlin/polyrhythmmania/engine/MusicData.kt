package polyrhythmmania.engine

import com.badlogic.gdx.math.MathUtils
import net.beadsproject.beads.ugens.SamplePlayer
import polyrhythmmania.engine.music.MusicVolMap
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.LoopParams


class MusicData(val engine: Engine) {

    val volumeMap: MusicVolMap = MusicVolMap()
    var loopParams: LoopParams = LoopParams.NO_LOOP_FORWARDS

    /**
     * The point in the MUSIC where it should sync up with the Music Sync marker.
     */
    var firstBeatSec: Float = 0f

    /**
     * Where the Music Sync marker actually is in beats.
     */
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
            if (atSeconds >= delaySec && player.loopType == SamplePlayer.LoopType.LOOP_FORWARDS && !player.isLoopInvalid()) {
                player.prepareStartBuffer()
                val adjustedSecs = (atSeconds - delaySec) * rate // Note that rate is multipled at this point
                val loopStart = player.loopStartMs
                val loopEnd = player.loopEndMs
                val loopDuration = (loopEnd - loopStart)

                return if (adjustedSecs < loopEnd / 1000) {
                    (adjustedSecs * 1000.0) % (player.musicSample.lengthMs)
                } else {
                    ((((adjustedSecs * 1000 - loopStart)) % loopDuration + loopStart)) % player.musicSample.lengthMs
                }
            } else {
                // When atSeconds == delaySec, time should be 0.
                return ((atSeconds - delaySec) * 1000.0) * rate
            }
        }

        return 0.0
    }
    
    fun computeMusicDelaySec(): Float {
        val rate = this.rate
        return ((engine.tempos.beatsToSeconds(this.musicSyncPointBeat) * rate - this.firstBeatSec) / rate * 1000 + (engine.musicOffsetMs * rate) /* <- this is a user calibration setting */) / 1000
    }

}