package polyrhythmmania.gamemodes

import net.beadsproject.beads.ugens.SamplePlayer
import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.GlobalContainerSettings
import polyrhythmmania.editor.block.Block
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.input.InputTimingRestriction
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.statistics.PlayTimeType
import polyrhythmmania.world.EntityRodDunk
import polyrhythmmania.world.WorldMode
import polyrhythmmania.world.WorldType
import polyrhythmmania.world.render.ForceTexturePack
import polyrhythmmania.world.render.ForceTilesetPalette
import polyrhythmmania.world.texturepack.TexturePackSource
import polyrhythmmania.world.tileset.TilesetPalette


class DunkMode(main: PRManiaGame, prevHighScore: EndlessModeScore)
    : AbstractEndlessMode(main, prevHighScore, PlayTimeType.DUNK) {
    
    init {
        container.world.worldMode = WorldMode(WorldType.Dunk)
        val modifiers = engine.modifiers
        modifiers.endlessScore.enabled.set(true)
        modifiers.endlessScore.maxLives.set(5)
        val inputter = container.engine.inputter
        inputter.inputChallenge.restriction = InputTimingRestriction.ACES_ONLY
        container.texturePackSource.set(TexturePackSource.StockGBA)
        TilesetPalette.createGBA1TilesetPalette().applyTo(container.renderer.tileset)
    }
    
    override fun createGlobalContainerSettings(): GlobalContainerSettings {
        return super.createGlobalContainerSettings().copy(forceTexturePack = ForceTexturePack.FORCE_GBA, forceTilesetPalette = ForceTilesetPalette.NO_FORCE)
    }
    
    override fun initialize() {
        engine.tempos.addTempoChange(TempoChange(0f, 129f))
        engine.musicData.volumeMap.addMusicVolume(MusicVolume(0f, 0f, 100))

        val music: BeadsMusic = SidemodeAssets.practiceTheme
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 0f
        musicData.loopParams = LoopParams(SamplePlayer.LoopType.LOOP_FORWARDS, 0.0, music.musicSample.lengthMs)
        musicData.beadsMusic = music
        musicData.update()
        
        addInitialBlocks()
    }
    
    private fun addInitialBlocks() {
        val blocks = mutableListOf<Block>()
        val loop = LoopingEventBlock(engine, 4f, { engine ->
            engine.modifiers.endlessScore.lives.get() > 0
        }) { engine, startBeat ->
            engine.addEvent(EventDeployRodDunk(engine, startBeat))
        }


        blocks += loop.apply {
            this.beat = 0f
        }

        container.addBlocks(blocks)
    }
}

class EventDeployRodDunk(engine: Engine, startBeat: Float) : Event(engine) {
    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        
        val endless = engine.modifiers.endlessScore
        if (!endless.enabled.get() || endless.lives.get() > 0) {
            engine.world.addEntity(EntityRodDunk(engine.world, this.beat))

            if (engine.areStatisticsEnabled) {
                GlobalStats.rodsDeployed.increment()
                GlobalStats.rodsDeployedDunk.increment()
            }
        }
    }
}
