package polyrhythmmania.storymode.gamemode

import com.badlogic.gdx.Gdx
import paintbox.util.gdxutils.set
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.BlockEndState
import polyrhythmmania.editor.block.BlockSkillStar
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.GdxAudioReader
import polyrhythmmania.world.tileset.TilesetPalette


class StoryAsmGameModeBouncyRoad(main: PRManiaGame)
    : AbstractStoryAsmGameMode(main) {
    
    init {
        TilesetPalette.createAssembleTilesetPalette().also { palette ->
            palette.cubeBorder.color.getOrCompute().set(0x00, 0x6B, 0xD3).mul(0.9f)
            palette.cubeBorderZ.color.getOrCompute().set(0x00, 0x58, 0xB2).mul(0.9f)
            palette.cubeFaceY.color.getOrCompute().set(0x00, 0x94, 0xFF).mul(0.9f)
            palette.cubeFaceZ.color.getOrCompute().set(0x00, 0x7E, 0xE5).mul(0.9f)
            palette.cubeFaceX.color.getOrCompute().set(0x00, 0x77, 0xDD).mul(0.9f)
            
            palette.aliasAsmLaneBorder.color.getOrCompute().set(0x00, 0x56, 0xA8).mul(0.975f)
            palette.aliasAsmLaneTop.color.getOrCompute().set(0x7F, 0xFC, 0xFF).mul(0.975f)
        }.applyTo(container.renderer.tileset)
        container.world.tilesetPalette.copyFrom(container.renderer.tileset)
    }
    
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
