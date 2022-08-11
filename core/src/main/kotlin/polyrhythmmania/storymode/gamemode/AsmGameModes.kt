package polyrhythmmania.storymode.gamemode

import com.badlogic.gdx.Gdx
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.BlockEndState
import polyrhythmmania.editor.block.BlockSkillStar
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.GdxAudioReader


class StoryAsmGameModeBouncyRoad(main: PRManiaGame)
    : AbstractStoryAsmGameMode(main) {
    
    override fun initialize() {
        super.initialize()
        
        engine.tempos.addTempoChange(TempoChange(0f, 154f))
        engine.tempos.addTempoChange(TempoChange(36f, 77f))
        engine.tempos.addTempoChange(TempoChange(44f, 154f))
        engine.musicData.volumeMap.addMusicVolume(MusicVolume(0f, 0f, 100))

        val music: BeadsMusic = GdxAudioReader.newMusic(Gdx.files.internal("story/levels/music/bouncy_road.ogg"), null)
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 0f
        musicData.firstBeatSec = 0.800f
        musicData.beadsMusic = music
        musicData.update()
        
        addBouncePatternsToContainer(listOf(
                newBouncePattern(4f, startOnLeft = false, 8),
                newBouncePattern(13f, startOnLeft = false, 6),
                newBouncePattern(20f, startOnLeft = false, 8),
                newBouncePattern(29f, startOnLeft = false, 6),
                newBouncePattern(36f, startOnLeft = false, 8, firstBeatsPerBounce = 2f),
                newBouncePattern(44f, startOnLeft = false, 14, firstBeatsPerBounce = 0.5f),
                newBouncePattern(60f, startOnLeft = false, 8),
                newBouncePattern(68f, startOnLeft = true, 3),
                newBouncePattern(70.5f, startOnLeft = true, 5),
                newBouncePattern(76f, startOnLeft = false, 8),
                newBouncePattern(85f, startOnLeft = false, 6),
                newBouncePattern(92f, startOnLeft = true, 3),
                newBouncePattern(96f, startOnLeft = true, 9),
                newBouncePattern(106f, startOnLeft = true, 1, firstBeatsPerBounce = 2f), // May start from left or right
        ))
        container.addBlock(BlockSkillStar(engine).apply { 
            this.beat = 74.5f
        })
        
        container.addBlock(BlockEndState(engine).apply {
            this.beat = 116f
        })
    }
}
