package polyrhythmmania.practice

import net.beadsproject.beads.ugens.SamplePlayer
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockTextbox
import polyrhythmmania.engine.EventCowbellSFX
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.engine.input.EventLockInputs
import polyrhythmmania.engine.input.InputType
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.world.*


class PracticeBasic(main: PRManiaGame) : Practice(main) {
    
    val practiceSection1: PracticeSection = PracticeSection(engine)
    
    init {
        practiceSection1.initBlock = PracticeInitBlock(8f, 4) { engine, startBeat ->
            val musicData = engine.musicData
            musicData.musicSyncPointBeat = startBeat + 4f
            musicData.update()
            musicData.setMusicPlayerPositionToCurrentSec()
            
            for (i in 0 until 4) {
                engine.addEvent(EventCowbellSFX(engine, startBeat + i, false))
            }

            engine.musicData.volumeMap.addMusicVolume(MusicVolume(startBeat, 0f, 100))
            engine.addEvent(EventLockInputs(engine, false, startBeat))
            engine.addEvent(EventDeployRod(engine, engine.world.rowA, startBeat + 4f))
            
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowA, 0, EntityRowBlock.Type.PISTON_A, startBeat + 4f + 0 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowA, 4, EntityRowBlock.Type.PISTON_A, startBeat + 4f + 4 * 0.5f))
            
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowA, 8, EntityRowBlock.Type.PLATFORM, startBeat + 4f + 8 * 0.5f, affectThisIndexAndForward = true))
        }
        practiceSection1.loopBlock = PracticeLoopBlock(4f) { engine, startBeat -> 
            engine.addEvent(EventDeployRod(engine, engine.world.rowA, startBeat))
            engine.addEvent(EventPracticeRetract(engine, engine.world.rowA, 0, startBeat + 3.5f, affectThisIndexAndForward = true))
            listOf(0, 4).map { EngineInputter.RequiredInput(startBeat + it * 0.5f, InputType.A) }
        }
        practiceSection1.endBlock = PracticeEndBlock(4f) { engine, startBeat ->
            engine.addEvent(EventLockInputs(engine, true, startBeat))
            engine.addEvent(EventRowBlockRetract(engine, engine.world.rowA, 0, startBeat + 2f, affectThisIndexAndForward = true))
            engine.addEvent(EventRowBlockDespawn(engine, engine.world.rowA, 0, startBeat + 3f, affectThisIndexAndForward = true))
            
            engine.musicData.volumeMap.addMusicVolume(MusicVolume(startBeat, 4f, 0))
        }
    }
    
    override fun initialize() {
        engine.tempos.addTempoChange(TempoChange(0f, 129f))
        
        val music: BeadsMusic = PracticeAssets.practiceTheme
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 10_000f
        musicData.loopParams = LoopParams(SamplePlayer.LoopType.LOOP_FORWARDS, 0.0, music.musicSample.lengthMs)
        musicData.beadsMusic = music
        musicData.update()
        
        addInitialBlocks()
    }
    
    private fun addInitialBlocks() {
        val blocks = mutableListOf<Block>()

        blocks += BlockTextbox(engine).apply { 
            this.beat = 0f
            this.requireInput.set(true)
            this.text = Localization.getValue("practice.basic.text0")
        }
        blocks += BlockTextbox(engine).apply { 
            this.beat = 2f
            this.requireInput.set(true)
            this.text = Localization.getValue("practice.basic.text1")
        }
        blocks += practiceSection1.apply { 
            this.beat = 4f
        }

        container.addBlocks(blocks)
    }
}