package polyrhythmmania.storymode.test.gamemode

import net.beadsproject.beads.ugens.SamplePlayer
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.gamemodes.SidemodeAssets
import polyrhythmmania.gamemodes.practice.AbstractPolyrhythmPractice
import polyrhythmmania.gamemodes.practice.Polyrhythm2Practice
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.storymode.gamemode.AbstractStoryGameMode


abstract class TestStoryGameMode(main: PRManiaGame) : AbstractStoryGameMode(main) {
    
    override fun initialize() {
        engine.tempos.addTempoChange(TempoChange(0f, 148.5f))

        val music: BeadsMusic = SidemodeAssets.polyrhythmTheme
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 0f
        musicData.loopParams = LoopParams(SamplePlayer.LoopType.LOOP_FORWARDS, 0.0, music.musicSample.lengthMs)
        musicData.rate = 148.5f / 129f
        musicData.firstBeatSec = 0f
        musicData.beadsMusic = music
        musicData.volumeMap.addMusicVolume(MusicVolume(0f, 0f, 100))
        musicData.volumeMap.addMusicVolume(MusicVolume(168f, 4f, 0))
        musicData.update()

        container.addBlocks(AbstractPolyrhythmPractice.parseBlocksJson(Polyrhythm2Practice.BLOCKS_JSON, engine))
    }
}