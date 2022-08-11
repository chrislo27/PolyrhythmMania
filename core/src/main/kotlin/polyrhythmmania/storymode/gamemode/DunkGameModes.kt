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
        super.initialize()
        
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

class StoryDunkGameModeHoleInOne2(main: PRManiaGame)
    : AbstractStoryDunkGameMode(main) {

    override fun initialize() {
        super.initialize()
        
        val globalBeatOffset = 2f
        
        engine.tempos.addTempoChange(TempoChange(0f, 122f))
        engine.musicData.volumeMap.addMusicVolume(MusicVolume(0f, 0f, 85))

        val music: BeadsMusic = GdxAudioReader.newMusic(Gdx.files.internal("story/levels/music/hole_in_one_2.ogg"), null)
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 0f + globalBeatOffset
        musicData.firstBeatSec = 0.220f
        musicData.beadsMusic = music
        musicData.update()

        addDunkPatternBlocks(listOf(
                newDunkPattern(1.0f),
                newDunkPattern(6.0f),
                newDunkPattern(10.0f),
                newDunkPattern(14.0f),
                newDunkPattern(16.0f),
                newDunkPattern(17.0f),
                newDunkPattern(18.0f),
                newDunkPattern(22.0f),
                newDunkPattern(26.0f),
                newDunkPattern(30.0f),
                newDunkPattern(32.0f),
                newDunkPattern(36.0f),
                newDunkPattern(40.0f),
                newDunkPattern(44.0f),
                newDunkPattern(46.0f),
                newDunkPattern(48.0f),
                newDunkPattern(52.0f),
                newDunkPattern(56.0f),
                newDunkPattern(60.0f),
                newDunkPattern(62.0f),
                newDunkPattern(64.0f),
                newDunkPattern(68.0f),
                newDunkPattern(72.0f),
                newDunkPattern(76.0f),
                newDunkPattern(77.0f),
                newDunkPattern(78.0f),
                newDunkPattern(80.0f),
                newDunkPattern(84.0f),
                newDunkPattern(86.0f),
                newDunkPattern(88.0f),
                newDunkPattern(92.0f),
                newDunkPattern(94.0f),
                newDunkPattern(101.0f),
                newDunkPattern(104.0f),
                newDunkPattern(106.0f),
                newDunkPattern(108.0f),
                newDunkPattern(110.0f),
                newDunkPattern(112.0f),
                newDunkPattern(113.0f),
                newDunkPattern(114.0f),
                newDunkPattern(115.0f),
                newDunkPattern(117.0f),
                newDunkPattern(120.0f),
                newDunkPattern(122.0f),
                newDunkPattern(124.0f),
                newDunkPattern(126.0f),
                newDunkPattern(128.0f),
                newDunkPattern(129.0f),
                newDunkPattern(130.0f),
                newDunkPattern(131.0f),
                newDunkPattern(133.0f),
                newDunkPattern(136.0f),
                newDunkPattern(138.0f),
                newDunkPattern(140.0f),
                newDunkPattern(142.0f),
                newDunkPattern(144.0f),
                newDunkPattern(145.0f),
                newDunkPattern(146.0f),
                newDunkPattern(147.0f),
                newDunkPattern(149.0f),
                newDunkPattern(152.0f),
                newDunkPattern(154.0f),
                newDunkPattern(156.0f),
                newDunkPattern(158.0f),
                newDunkPattern(160.0f),
                newDunkPattern(161.0f),
                newDunkPattern(162.0f),
                newDunkPattern(163.0f),
                newDunkPattern(165.0f),
                newDunkPattern(168.0f),
                newDunkPattern(170.0f),
                newDunkPattern(173.5f),
                newDunkPattern(176.0f),
                newDunkPattern(178.0f),
                newDunkPattern(181.0f),
                newDunkPattern(186.0f),
                newDunkPattern(190.0f),
                newDunkPattern(194.0f),
                newDunkPattern(196.0f),
                newDunkPattern(202.0f),
                newDunkPattern(206.0f),
                newDunkPattern(210.0f),
                newDunkPattern(212.0f),
                newDunkPattern(216.5f),
        ).onEach { block ->
            block.beat += globalBeatOffset
        })
        container.addBlock(BlockEndState(engine).apply {
            this.beat = 228f + globalBeatOffset
        })
    }
}
