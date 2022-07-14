package polyrhythmmania.storymode.gamemode

import com.badlogic.gdx.Gdx
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.BlockEndState
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.GdxAudioReader


class StoryDunkGameModeFruitBasket(main: PRManiaGame)
    : AbstractStoryDunkGameMode(main) {
    
    override fun initialize() {
        engine.tempos.addTempoChange(TempoChange(0f, 151f))
        engine.musicData.volumeMap.addMusicVolume(MusicVolume(0f, 0f, 100))

        val music: BeadsMusic = GdxAudioReader.newMusic(Gdx.files.internal("story/levels/music/fruit_basket.ogg"), null)
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 0f
        musicData.firstBeatSec = 0.026f
        musicData.beadsMusic = music
        musicData.update()
        
        addDunkPatternBlocks(listOf(
                newDunkPattern(4f),
                newDunkPattern(10f),
                newDunkPattern(16f),
                newDunkPattern(20f),
                newDunkPattern(26f),
                newDunkPattern(32f),
                newDunkPattern(36f),
                newDunkPattern(40f),
                newDunkPattern(42f),
                newDunkPattern(44f),
                newDunkPattern(51f),
                newDunkPattern(56f),
                newDunkPattern(58f),
                newDunkPattern(60f),
                newDunkPattern(64f),
                newDunkPattern(68f),
                newDunkPattern(72f),
                newDunkPattern(77f),
                newDunkPattern(83f),
                newDunkPattern(88f),
                newDunkPattern(92f),
                newDunkPattern(96f),
                newDunkPattern(98f),
                newDunkPattern(103f),
                newDunkPattern(108f),
                newDunkPattern(115f),
        ))
        container.addBlock(BlockEndState(engine).apply {
            this.beat = 126f
        })
    }
}