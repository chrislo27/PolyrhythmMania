package polyrhythmmania.engine

import com.badlogic.gdx.math.MathUtils
import net.beadsproject.beads.ugens.SamplePlayer
import polyrhythmmania.engine.music.MusicVolMap
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.soundsystem.sample.PlayerLike


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

    
    // Mutable state that is not file-persisted
    
    /**
     * Music volume multiplier for temporary effects, like the music fade out when Endless Mode ends. 
     * This is reset to 1.0 in [resetState].
     */
    var musicVolumeMultiplier: Float = 1f
    
    fun resetState() {
        this.musicVolumeMultiplier = 1f
    }

    fun update() {
        val currentBeat = engine.beat
        val music = this.beadsMusic
        val player = engine.soundInterface.getCurrentMusicPlayer(music)
        if (player != null) {
            val volume: Int = volumeMap.volumeAtBeat(currentBeat)
            updatePlayerWithVolumeAndRate(player, volume, this.rate)

            if (!player.isPaused && !player.context.out.isPaused) {
                // Player desync correction
                val currentSeconds = engine.seconds
                val currentPosition = player.position
                val correctPosition = getCorrectMusicPlayerPositionAt(currentSeconds)
                if (!MathUtils.isEqual(correctPosition.toFloat(), currentPosition.toFloat(), 50f /* ms */)) {
                    player.position = correctPosition
//                    Paintbox.LOGGER.debug("[MusicData] Manually adjusted player due to desync: was $currentPosition, should be $correctPosition (delta ${correctPosition - currentPosition} ms)")
                }
            }
        }
    }
    
    fun updatePlayerWithVolumeAndRate(player: PlayerLike, volume: Int = volumeMap.volumeAtBeat(engine.beat), rate: Float = this.rate) {
        player.gain = (volume / 100f) * this.musicVolumeMultiplier
        player.pitch = rate * engine.playbackSpeed
    }
    
    fun getCurrentDesyncMs(): Double {
        val music = this.beadsMusic
        val player = engine.soundInterface.getCurrentMusicPlayer(music)
        return if (player != null && !player.isPaused && !player.context.out.isPaused) {
            val currentPosition = player.position
            val correctPosition = getCorrectMusicPlayerPositionAt(engine.seconds)
            correctPosition - currentPosition
        } else 0.0
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
                val adjustedSecs = (atSeconds - delaySec) * rate
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
        return ((engine.tempos.beatsToSeconds(this.musicSyncPointBeat) * rate - this.firstBeatSec) / rate * 1000 + (engine.inputCalibration.audioOffsetMs * rate) /* <- this is a user calibration setting */) / 1000
    }
    
    fun computeMusicDelaySecNoCalibration(): Float {
        val rate = this.rate
        return ((engine.tempos.beatsToSeconds(this.musicSyncPointBeat) * rate - this.firstBeatSec) / rate * 1000) / 1000
    }

}