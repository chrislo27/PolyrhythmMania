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
            palette.cubeBorder.color.getOrCompute().set(0x00, 0x6B, 0xD3).mul(0.9f, 0.9f, 0.9f, 1f)
            palette.cubeBorderZ.color.getOrCompute().set(0x00, 0x58, 0xB2).mul(0.9f, 0.9f, 0.9f, 1f)
            palette.cubeFaceY.color.getOrCompute().set(0x00, 0x94, 0xFF).mul(0.9f, 0.9f, 0.9f, 1f)
            palette.cubeFaceZ.color.getOrCompute().set(0x00, 0x7E, 0xE5).mul(0.9f, 0.9f, 0.9f, 1f)
            palette.cubeFaceX.color.getOrCompute().set(0x00, 0x77, 0xDD).mul(0.9f, 0.9f, 0.9f, 1f)
            
            palette.aliasAsmLaneBorder.color.getOrCompute().set(0x00, 0x56, 0xA8).mul(0.975f, 0.975f, 0.975f, 1f)
            palette.aliasAsmLaneTop.color.getOrCompute().set(0x7F, 0xFC, 0xFF).mul(0.975f, 0.975f, 0.975f, 1f)
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

class StoryAsmGameModeBouncyRoad2(main: PRManiaGame)
    : AbstractStoryAsmGameMode(main) {

    init {
        TilesetPalette.createAssembleTilesetPalette().also { palette ->
            palette.cubeBorder.color.getOrCompute().set(0x55, 0x00, 0x9B).mul(0.9f, 0.9f, 0.9f, 1f)
            palette.cubeBorderZ.color.getOrCompute().set(0x41, 0x00, 0x7A).mul(0.9f, 0.9f, 0.9f, 1f)
            palette.cubeFaceY.color.getOrCompute().set(0x7A, 0x00, 0xC6).mul(0.9f, 0.9f, 0.9f, 1f)
            palette.cubeFaceZ.color.getOrCompute().set(0x65, 0x00, 0xAD).mul(0.9f, 0.9f, 0.9f, 1f)
            palette.cubeFaceX.color.getOrCompute().set(0x5F, 0x00, 0xA5).mul(0.9f, 0.9f, 0.9f, 1f)

            palette.aliasAsmLaneBorder.color.getOrCompute().set(0x33, 0x00, 0x60).mul(0.975f, 0.975f, 0.975f, 1f)
            palette.aliasAsmLaneTop.color.getOrCompute().set(0xBA, 0x7C, 0xE2).mul(0.975f, 0.975f, 0.975f, 1f)
        }.applyTo(container.renderer.tileset)
        container.world.tilesetPalette.copyFrom(container.renderer.tileset)
    }

    private fun newBouncePatternSkips(startBeat: Float, startOnLeft: Boolean, numBouncesInclFire: Int,
                                      beatsPerBounce: Float = 1f, firstBeatsPerBounce: Float = beatsPerBounce,
                                      rodID: Int = -1): BouncePattern {
        val indices: List<Int> = buildList {
            var goingRight = startOnLeft
            var next: Int = if (numBouncesInclFire == 1) 2 else (if (startOnLeft) 0 else 3)

            for (i in 0 until numBouncesInclFire - 1) {
                this += next
                if (goingRight) {
                    next += 1
                    if (next == 2) {
                        next += 1
                    }
                    if (next == 3) {
                        goingRight = false
                    }
                } else {
                    next -= 1
                    if (next == 2) {
                        next -= 1
                    }
                    if (next == 0) {
                        goingRight = true
                    }
                }
            }
            this += 2
        }
        val block = BlockAsmBouncePattern(startBeat, if (startOnLeft) -1 else 999, indices, beatsPerBounce, firstBeatsPerBounce, rodID)
        return BouncePattern(block, block.getNumInputs())
    }

    override fun initialize() {
        super.initialize()

        engine.tempos.addTempoChange(TempoChange(0f, 154f))
        engine.musicData.volumeMap.addMusicVolume(MusicVolume(0f, 0f, 100))

        val music: BeadsMusic = GdxAudioReader.newMusic(Gdx.files.internal("story/levels/music/bouncy_road.ogg"), null)
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 4f
        musicData.firstBeatSec = 0.800f
        musicData.beadsMusic = music
        musicData.update()

        var currentRodID: Int = 1000
        @Suppress("UNUSED_CHANGED_VALUE")
        addBouncePatternsToContainer(listOf(
                newBouncePattern(0f, startOnLeft = true, 15, beatsPerBounce = 1f, firstBeatsPerBounce = 60f / 154f, rodID = currentRodID++),
                newBouncePattern(8f, startOnLeft = true, 15, beatsPerBounce = 1f, rodID = currentRodID++),
                newBouncePattern(24f, startOnLeft = true, 15, beatsPerBounce = 1f, rodID = currentRodID++),
                newBouncePattern(40f, startOnLeft = true, 15, beatsPerBounce = 2f, rodID = currentRodID++),
                newBouncePattern(42f + 2/3f, startOnLeft = true, 15, beatsPerBounce = 2/3f, rodID = currentRodID++),
                newBouncePattern(64f, startOnLeft = false, 2, beatsPerBounce = 1f, rodID = currentRodID++),
                newBouncePattern(72.5f, startOnLeft = true, 15, beatsPerBounce = 1f, rodID = currentRodID++),
                newBouncePattern(74f, startOnLeft = true, 15, beatsPerBounce = 0.5f, rodID = currentRodID++),
                newBouncePattern(88f, startOnLeft = true, 15, beatsPerBounce = 2f, rodID = currentRodID++),
                newBouncePattern(90.5f, startOnLeft = true, 3, beatsPerBounce = 0.5f, rodID = currentRodID++),
                newBouncePattern(95f, startOnLeft = false, 2, beatsPerBounce = 1f, rodID = currentRodID++),
                newBouncePattern(100f, startOnLeft = true, 3, beatsPerBounce = 1f, rodID = currentRodID++),
                BlockAsmBouncePattern(108f, 999, listOf(3, 0, 2), beatsPerBounce = 1f, rodID = currentRodID++).let { BouncePattern(it, it.getNumInputs()) },
                newBouncePattern(119f, startOnLeft = true, 1, firstBeatsPerBounce = 2f, rodID = currentRodID++),
        ))
        container.addBlock(BlockSkillStar(engine).apply {
            this.beat = 86.5f
        })

        container.addBlock(BlockEndState(engine).apply {
            this.beat = 128f
        })
    }
}