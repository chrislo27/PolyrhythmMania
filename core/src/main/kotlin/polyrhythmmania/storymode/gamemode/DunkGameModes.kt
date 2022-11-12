package polyrhythmmania.storymode.gamemode

import com.badlogic.gdx.Gdx
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.BlockEndState
import polyrhythmmania.editor.block.BlockSkillStar
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.GdxAudioReader
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.world.DunkWorldBackground


class StoryDunkGameModeFruitBasket(main: PRManiaGame)
    : AbstractStoryDunkGameMode(main) {
    
    init {
        this.container.renderer.worldBackground = DunkWorldBackground { StoryAssets["dunk_background_fruit_basket_1"] }
    }
    
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
        container.addBlock(BlockSkillStar(engine).apply {
            this.beat = 104f
        })
        container.addBlock(BlockEndState(engine).apply {
            this.beat = 126f
        })
    }
}

class StoryDunkGameModeHoleInOne2(main: PRManiaGame)
    : AbstractStoryDunkGameMode(main) {

    init {
        this.container.renderer.worldBackground = DunkWorldBackground { StoryAssets["dunk_background_hole_in_one_2"] }
    }

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
        container.addBlock(BlockSkillStar(engine).apply {
            this.beat = 174.5f + globalBeatOffset
        })
        container.addBlock(BlockEndState(engine).apply {
            this.beat = 228f + globalBeatOffset
        })
    }
}

class StoryDunkGameModeHoleInOne(main: PRManiaGame)
    : AbstractStoryDunkGameMode(main) {

    init {
        this.container.renderer.worldBackground = DunkWorldBackground { StoryAssets["dunk_background_hole_in_one_1"] }
    }

    override fun initialize() {
        super.initialize()

        val globalBeatOffset = 0f

        engine.tempos.addTempoChange(TempoChange(0f, 115f))
        engine.musicData.volumeMap.addMusicVolume(MusicVolume(0f, 0f, 85))

        val music: BeadsMusic = GdxAudioReader.newMusic(Gdx.files.internal("story/levels/music/hole_in_one.ogg"), null)
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 0f + globalBeatOffset
        musicData.firstBeatSec = 0.010f
        musicData.beadsMusic = music
        musicData.update()

        addDunkPatternBlocks(listOf(
                newDunkPattern(0.0f),
                newDunkPattern(7.0f),
                newDunkPattern(11.0f),
                newDunkPattern(15.0f),
                newDunkPattern(20.0f),
                newDunkPattern(26.0f),
                newDunkPattern(30.0f),
                newDunkPattern(35.0f),
                newDunkPattern(37.0f),
                newDunkPattern(39.0f),
                newDunkPattern(42.0f),
                newDunkPattern(46.0f),
                newDunkPattern(51.0f),
                newDunkPattern(53.0f),
                newDunkPattern(59.0f),
                newDunkPattern(62.0f),
                newDunkPattern(68.0f),
                newDunkPattern(72.0f),
                newDunkPattern(74.0f),
                newDunkPattern(76.0f),
                newDunkPattern(78.0f),
                newDunkPattern(83.0f),
                newDunkPattern(87.0f),
                newDunkPattern(89.0f),
                newDunkPattern(93.0f),
                newDunkPattern(95.0f),
                newDunkPattern(100.0f),
                newDunkPattern(104.0f),
                newDunkPattern(106.0f),
                newDunkPattern(108.0f),
                newDunkPattern(110.0f),
                newDunkPattern(112.0f),
                newDunkPattern(114.0f),
                newDunkPattern(116.0f),
                newDunkPattern(118.0f),
                newDunkPattern(120.0f),
                newDunkPattern(122.0f),
                newDunkPattern(124.0f),
                newDunkPattern(127.0f),
                newDunkPattern(132.0f),
                newDunkPattern(135.5f),
                newDunkPattern(138.0f),
                newDunkPattern(142.0f),
                newDunkPattern(143.0f),
                newDunkPattern(144.0f),
                newDunkPattern(148.0f),
                newDunkPattern(155.0f),
                newDunkPattern(160.5f),
                newDunkPattern(163.0f),
                newDunkPattern(169.0f),
                newDunkPattern(172.0f),
                newDunkPattern(179.0f),
                newDunkPattern(184.0f),
                newDunkPattern(187.5f),
        ).onEach { block ->
            block.beat += 5f
            block.beat += globalBeatOffset
        })
        container.addBlock(BlockSkillStar(engine).apply {
            this.beat = 154f + globalBeatOffset
        })
        container.addBlock(BlockEndState(engine).apply {
            this.beat = 200f + globalBeatOffset
        })
    }
}

class StoryDunkGameModeFruitBasket2(main: PRManiaGame)
    : AbstractStoryDunkGameMode(main) {

    init {
        this.container.renderer.worldBackground = DunkWorldBackground { StoryAssets["dunk_background_fruit_basket_2"] }
    }

    override fun initialize() {
        super.initialize()

        val globalBeatOffset = 0f

        engine.tempos.addTempoChange(TempoChange(0f, 163f))
        engine.musicData.volumeMap.addMusicVolume(MusicVolume(0f, 0f, 65))

        val music: BeadsMusic = GdxAudioReader.newMusic(Gdx.files.internal("story/levels/music/fruit_basket_2.ogg"), null)
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 0f + globalBeatOffset
        musicData.firstBeatSec = 0.031f
        musicData.beadsMusic = music
        musicData.update()

        addDunkPatternBlocks(listOf(
                newDunkPattern(0.0f),
                newDunkPattern(1.0f),
                newDunkPattern(2.0f),
                newDunkPattern(8.0f),
                newDunkPattern(14.0f),
                newDunkPattern(16.0f),
                newDunkPattern(20.0f),
                newDunkPattern(22.0f),
                newDunkPattern(28.0f),
                newDunkPattern(30.0f),
                newDunkPattern(31.5f),
                newDunkPattern(36.0f),
                newDunkPattern(38.0f),
                newDunkPattern(40.0f),
                newDunkPattern(46.0f),
                newDunkPattern(47.5f),
                newDunkPattern(52.0f),
                newDunkPattern(53.5f),
                newDunkPattern(56.0f),
                newDunkPattern(57.0f),
                newDunkPattern(58.0f),
                newDunkPattern(63.0f),
                newDunkPattern(68.0f),
                newDunkPattern(70.0f),
                newDunkPattern(71.5f),
                newDunkPattern(76.0f),
                newDunkPattern(81.0f),
                newDunkPattern(85.0f),
                newDunkPattern(87.0f),
                newDunkPattern(88.5f),
                newDunkPattern(92.0f),
                newDunkPattern(96.0f),
                newDunkPattern(100.0f),
                newDunkPattern(102.0f),
                newDunkPattern(110.0f),
                newDunkPattern(112.0f),
                newDunkPattern(113.5f),
                newDunkPattern(115.0f),
                newDunkPattern(120.0f),
                newDunkPattern(121.0f),
                newDunkPattern(122.0f),
                newDunkPattern(126.0f),
                newDunkPattern(132.0f),
                newDunkPattern(134.0f),
                newDunkPattern(135.5f),
                newDunkPattern(141.0f),
                newDunkPattern(143.0f),
                newDunkPattern(145.0f),
                newDunkPattern(148.0f),
                newDunkPattern(152.0f),
                newDunkPattern(153.0f),
                newDunkPattern(154.0f),
                newDunkPattern(157.0f),
                newDunkPattern(159.0f),
                newDunkPattern(164.0f),
                newDunkPattern(166.0f),
                newDunkPattern(167.0f),
                newDunkPattern(168.0f),
                newDunkPattern(172.0f),
                newDunkPattern(174.0f),
                newDunkPattern(180.0f),
                newDunkPattern(181.0f),
                newDunkPattern(182.0f),
                newDunkPattern(183.0f),
                newDunkPattern(185.0f),
                newDunkPattern(186.0f),
                newDunkPattern(190.0f),
                newDunkPattern(196.0f),
                newDunkPattern(198.0f),
                newDunkPattern(199.5f),
                newDunkPattern(204.0f),
                newDunkPattern(206.0f),
                newDunkPattern(207.5f),
                newDunkPattern(212.0f),
                newDunkPattern(214.0f),
                newDunkPattern(216.0f),
                newDunkPattern(217.5f),
                newDunkPattern(222.0f),
                newDunkPattern(224.0f),
                newDunkPattern(226.0f),
                newDunkPattern(232.0f),
                newDunkPattern(233.5f),
                newDunkPattern(234.5f),
                newDunkPattern(239.0f),
                ).onEach { block ->
            block.beat += 4f
            block.beat += globalBeatOffset
        })
        container.addBlock(BlockSkillStar(engine).apply { 
            this.beat = 190f + globalBeatOffset
        })
        container.addBlock(BlockEndState(engine).apply {
            this.beat = 252f + globalBeatOffset
        })
    }
}
