package polyrhythmmania.storymode.gamemode.boss

import paintbox.Paintbox
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.SoundInterface
import polyrhythmmania.storymode.music.Stem
import polyrhythmmania.storymode.music.StemCache


class BossMusicEvent(engine: Engine, val stems: StemCache, val stemID: String, beat: Float, duration: Float) : BossAudioEvent(engine) {

    private val stem: Stem? get() = stems.getOrLoad(stemID)?.takeIf { it.isSampleAccessible.get() }
    private var id: Long = -1L

    init {
        this.beat = beat
        this.width = duration
    }

    override fun onAudioStart(atBeat: Float, actualBeat: Float) {
        this.id = -1L

        val stem = this.stem
        val audio = stem?.beadsMusic
        
        if (stem == null) {
            Paintbox.LOGGER.warn("Stem for stem ID $stemID is null")
        } else if (audio == null) {
            Paintbox.LOGGER.warn("BeadsAudio for stem ID $stemID is null")
        } else {
            val beatOffset = actualBeat - atBeat
            val secOffset = engine.tempos.beatsToSeconds(actualBeat) - engine.tempos.beatsToSeconds(atBeat)
            if (beatOffset > this.width / 2 || secOffset > 2f * engine.playbackSpeed) {
                // Off by too much
                return
            }
            val id = engine.soundInterface.playAudio(audio, SoundInterface.SFXType.DYNAMIC_MUSIC) { player ->
                if (secOffset > 0.25f) {
                    // Adjust player position if off by too much
                    player.position = secOffset * 1000.0
                }
            }
            this.id = id
        }
    }

    override fun onAudioUpdate(atBeat: Float, actualBeat: Float) {
        val musicData = engine.musicData
        engine.soundInterface.getPlayer(this.id)?.apply {
            musicData.updatePlayerWithVolumeAndRate(this, volume = musicData.volumeMap.volumeAtBeat(atBeat))
        }
    }
}
