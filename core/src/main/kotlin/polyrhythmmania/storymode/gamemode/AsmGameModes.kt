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
                newBouncePattern(36f, startOnLeft = false, 8, beatsPerBounce = 2f),
                newBouncePattern(52f, startOnLeft = false, 14),
                newBouncePattern(68f, startOnLeft = false, 8),
                newBouncePattern(76f, startOnLeft = true, 3),
                newBouncePattern(78.5f, startOnLeft = true, 5),
                newBouncePattern(84f, startOnLeft = false, 8),
                newBouncePattern(93f, startOnLeft = false, 6),
                newBouncePattern(100f, startOnLeft = true, 3),
                newBouncePattern(104f, startOnLeft = true, 9),
                newBouncePattern(115f, startOnLeft = true, 1, firstBeatsPerBounce = 2f), // May start from left or right
        ))
        container.addBlock(BlockSkillStar(engine).apply { 
            this.beat = 82.5f
        })
        
        container.addBlock(BlockEndState(engine).apply {
            this.beat = 124f
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
        musicData.musicSyncPointBeat = 1 + 4f
        musicData.firstBeatSec = 0.800f
        musicData.beadsMusic = music
        musicData.update()

        var currentRodID: Int = 1000
        @Suppress("UNUSED_CHANGED_VALUE")
        addBouncePatternsToContainer(listOf(
                newBouncePattern(1 + 0f, startOnLeft = true, 15, beatsPerBounce = 1f, rodID = currentRodID++),
                newBouncePattern(1 + 8f, startOnLeft = true, 15, beatsPerBounce = 1f, rodID = currentRodID++),
                newBouncePattern(1 + 24f, startOnLeft = true, 15, beatsPerBounce = 1f, rodID = currentRodID++),
                newBouncePattern(1 + 41f, startOnLeft = true, 15, beatsPerBounce = 2f, rodID = currentRodID++),
                newBouncePattern(1 + 42f + 1/3f, startOnLeft = true, 15, beatsPerBounce = 2/3f, rodID = currentRodID++),
                newBouncePattern(1 + 64f, startOnLeft = false, 2, beatsPerBounce = 1f, rodID = currentRodID++),
                newBouncePattern(1 + 72.5f, startOnLeft = true, 15, beatsPerBounce = 1f, rodID = currentRodID++),
                newBouncePattern(1 + 73.5f, startOnLeft = true, 15, beatsPerBounce = 0.5f, rodID = currentRodID++),
                newBouncePattern(1 + 89f, startOnLeft = true, 15, beatsPerBounce = 2f, rodID = currentRodID++),
                newBouncePattern(1 + 90f, startOnLeft = true, 3, beatsPerBounce = 0.5f, rodID = currentRodID++),
                newBouncePattern(1 + 95f, startOnLeft = false, 2, beatsPerBounce = 1f, rodID = currentRodID++),
                newBouncePattern(1 + 100f, startOnLeft = true, 3, beatsPerBounce = 1f, rodID = currentRodID++),
                BlockAsmBouncePattern(1 + 108f, 999, listOf(3, 0, 2), beatsPerBounce = 1f, rodID = currentRodID++).let { BouncePattern(it, it.getNumInputs()) },
                newBouncePattern(1 + 120f, startOnLeft = true, 1, firstBeatsPerBounce = 2f, rodID = currentRodID++),
        ))
        container.addBlock(BlockSkillStar(engine).apply {
            this.beat = 1 + 86.5f
        })

        container.addBlock(BlockEndState(engine).apply {
            this.beat = 1 + 128f
        })
    }
}

class StoryAsmGameModeMonkeyWatch(main: PRManiaGame)
    : AbstractStoryAsmGameMode(main) {

    init {
        TilesetPalette.createAssembleTilesetPalette().also { palette ->
            palette.cubeBorder.color.getOrCompute().set(0xEE, 0x64, 0x38).mul(0.9f, 0.9f, 0.9f, 1f)
            palette.cubeBorderZ.color.getOrCompute().set(0xCC, 0x48, 0x2E).mul(0.9f, 0.9f, 0.9f, 1f)
            palette.cubeFaceY.color.getOrCompute().set(0xFE, 0x94, 0x52).mul(0.9f, 0.9f, 0.9f, 1f)
            palette.cubeFaceZ.color.getOrCompute().set(0xFE, 0x7B, 0x2F).mul(0.9f, 0.9f, 0.9f, 1f)
            palette.cubeFaceX.color.getOrCompute().set(0xFE, 0x7B, 0x2F).mul(0.9f, 0.9f, 0.9f, 1f)

            palette.aliasAsmLaneBorder.color.getOrCompute().set(0x53, 0x42, 0x2A).mul(0.975f, 0.975f, 0.975f, 1f)
            palette.aliasAsmLaneTop.color.getOrCompute().set(0xFF, 0xFF, 0xDD).mul(0.975f, 0.975f, 0.975f, 1f)
        }.applyTo(container.renderer.tileset)
        container.world.tilesetPalette.copyFrom(container.renderer.tileset)
    }

    override fun initialize() {
        super.initialize()

        engine.tempos.addTempoChange(TempoChange(0f, 160f))
        engine.musicData.volumeMap.addMusicVolume(MusicVolume(0f, 0f, 50))

        val music: BeadsMusic = GdxAudioReader.newMusic(Gdx.files.internal("story/levels/music/monkey_watch.ogg"), null)
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 0f
        musicData.firstBeatSec = 0.354f
        musicData.beadsMusic = music
        musicData.update()

        var currentRodID: Int = 1000
        @Suppress("UNUSED_CHANGED_VALUE")
        addBouncePatternsToContainer(listOf(
                newBouncePattern(3f, startOnLeft = true, 5, rodID = currentRodID++),
                newBouncePattern(8f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(16f, startOnLeft = true, 5, rodID = currentRodID++),
                newBouncePattern(21f, startOnLeft = true, 3, rodID = currentRodID++),
                newBouncePattern(24f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(32f, startOnLeft = true, 5, rodID = currentRodID++),
                newBouncePattern(35.5f, startOnLeft = true, 5, rodID = currentRodID++),
                newBouncePattern(41f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(49f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(49f, startOnLeft = false, 8, beatsPerBounce = 2f, rodID = currentRodID++),
                newBouncePattern(64f, startOnLeft = true, 5, rodID = currentRodID++),
                newBouncePattern(69f, startOnLeft = true, 3, rodID = currentRodID++),
                newBouncePattern(72f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(80f, startOnLeft = true, 3, rodID = currentRodID++),
                newBouncePattern(83.5f, startOnLeft = true, 5, rodID = currentRodID++),
                newBouncePattern(88f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(96f, startOnLeft = true, 5, rodID = currentRodID++),
                newBouncePattern(105.5f, startOnLeft = true, 3, rodID = currentRodID++),
                newBouncePattern(104f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(105f, startOnLeft = true, 27, beatsPerBounce = 2f, rodID = currentRodID++),
                newBouncePattern(112.5f, startOnLeft = true, 3, rodID = currentRodID++),
                newBouncePattern(115.5f, startOnLeft = true, 5, rodID = currentRodID++),
                newBouncePattern(120f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(128f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(138.5f, startOnLeft = false, 6, rodID = currentRodID++),
                newBouncePattern(145f, startOnLeft = true, 15, rodID = currentRodID++),
                newBouncePattern(160f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(167.5f, startOnLeft = true, 5, rodID = currentRodID++),
                newBouncePattern(172.5f, startOnLeft = true, 3, rodID = currentRodID++),
                newBouncePattern(176f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(184f, startOnLeft = true, 5, rodID = currentRodID++),
                newBouncePattern(189f, startOnLeft = true, 3, rodID = currentRodID++),
                newBouncePattern(192f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(200f, startOnLeft = true, 5, rodID = currentRodID++),
                newBouncePattern(203.5f, startOnLeft = true, 5, rodID = currentRodID++),
                newBouncePattern(209f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(217f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(217f, startOnLeft = false, 8, beatsPerBounce = 2f, rodID = currentRodID++),
                newBouncePattern(232.5f, startOnLeft = false, 8, rodID = currentRodID++),
                newBouncePattern(240f, startOnLeft = true, 13, beatsPerBounce = 2f, indices = listOf(
                        0, 1, 2, 3, 2, 1, 0, 1, 2, 3, 2, 1, 0, -999
                ), rodID = currentRodID++), // Custom, doesn't end with fire
        ))
        container.addBlock(BlockSkillStar(engine).apply {
            this.beat = 157f
        })

        container.addBlock(BlockEndState(engine).apply {
            this.beat = 274f
        })
    }
}
