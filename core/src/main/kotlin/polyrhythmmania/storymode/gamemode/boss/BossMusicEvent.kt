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
            val id = engine.soundInterface.playAudio(audio, SoundInterface.SFXType.DYNAMIC_MUSIC)
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
