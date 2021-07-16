package polyrhythmmania.sidemodes

import net.beadsproject.beads.ugens.SamplePlayer
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.Block
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.world.DunkWorldBackground
import polyrhythmmania.world.EntityRodDunk
import polyrhythmmania.world.WorldMode


class DunkMode(main: PRManiaGame, prevHighScore: EndlessModeScore)
    : EndlessMode(main, prevHighScore) {
    
    init {
        container.world.worldMode = WorldMode.DUNK
        container.engine.inputter.endlessScore.maxLives.set(5)
        container.renderer.worldBackground = DunkWorldBackground
    }
    
    override fun initialize() {
        engine.tempos.addTempoChange(TempoChange(0f, 129f))

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
        val b = LoopingEventBlock(engine, 4f, { engine ->
            engine.inputter.endlessScore.lives.getOrCompute() > 0
        }) { engine, startBeat ->
            engine.addEvent(EventDeployRodDunk(engine, startBeat))
        }


        blocks += b.apply {
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
        engine.world.addEntity(EntityRodDunk(engine.world, this.beat))
    }
}
