package polyrhythmmania.gamemodes.practice

import com.badlogic.gdx.Input
import net.beadsproject.beads.ugens.SamplePlayer
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockTextbox
import polyrhythmmania.engine.EventCowbellSFX
import polyrhythmmania.engine.input.*
import polyrhythmmania.engine.input.practice.RequiredInput
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.gamemodes.SidemodeAssets
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.statistics.PlayTimeType
import polyrhythmmania.world.*
import polyrhythmmania.world.entity.EntityPiston


class PracticeTutorial1(main: PRManiaGame, val keyboardKeymap: InputKeymapKeyboard)
    : AbstractPracticeTutorial(main, PlayTimeType.REGULAR, 0b0001) {
    
    val practiceSection1: PracticeSection = PracticeSection(engine)
    val practiceSection2: PracticeSection = PracticeSection(engine)
    val practiceSection3: PracticeSection = PracticeSection(engine)
    val practiceSection4: PracticeSection = PracticeSection(engine)
    
    init {
        practiceSection1.initBlock = PracticeInitBlock(8f + 2, 3) { engine, startBeat ->
            val musicData = engine.musicData
            musicData.musicSyncPointBeat = startBeat + 2 + 4f
            musicData.update()
            musicData.setMusicPlayerPositionToCurrentSec()
            
            for (i in 0 until 4) {
                engine.addEvent(EventCowbellSFX(engine, startBeat + 2 + i, false))
            }

            val volumeMap = engine.musicData.volumeMap
            volumeMap.removeMusicVolumesBulk(volumeMap.getAllMusicVolumes().toList())
            volumeMap.addMusicVolume(MusicVolume(startBeat + 2, 0f, 100))
            
            engine.addEvent(EventLockInputs(engine, false, startBeat + 2))
            engine.addEvent(EventDeployRod(engine, engine.world.rowA, startBeat + 2 + 4f))
            
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowA, 0, EntityPiston.Type.PISTON_A, startBeat + 2 + 4f + 0 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowA, 4, EntityPiston.Type.PISTON_A, startBeat + 2 + 4f + 4 * 0.5f))
            
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowA, 8, EntityPiston.Type.PLATFORM, startBeat + 2 + 4f + 8 * 0.5f, affectThisIndexAndForward = true))
        }
        practiceSection1.loopBlock = PracticeLoopBlock(4f) { engine, startBeat -> 
            engine.addEvent(EventDeployRod(engine, engine.world.rowA, startBeat))
            engine.addEvent(EventPracticeRetract(engine, engine.world.rowA, 0, startBeat + 3.5f, affectThisIndexAndForward = true))
            listOf(0, 4).map { RequiredInput(startBeat + it * 0.5f, InputType.A) }
        }
        practiceSection1.endBlock = PracticeEndBlock(12f) { engine, startBeat ->
            engine.addEvent(EventLockInputs(engine, true, startBeat))
            engine.addEvent(EventRowBlockRetract(engine, engine.world.rowA, 0, startBeat + 2f, affectThisIndexAndForward = true))
            engine.addEvent(EventRowBlockDespawn(engine, engine.world.rowA, 0, startBeat + 3f, affectThisIndexAndForward = true))
            
            engine.musicData.volumeMap.addMusicVolume(MusicVolume(startBeat, 4f, 0))
            
            engine.addEvents(BlockTextbox(engine).apply {
                this.beat = startBeat + 4f + 0f
                this.requireInput.set(true)
                this.text = Localization.getValue("practice.tutorial1.text1a")
            }.compileIntoEvents())
            engine.addEvents(BlockTextbox(engine).apply {
                this.beat = startBeat + 4f + 1f
                this.requireInput.set(true)
                this.text = Localization.getValue("practice.tutorial1.text1b")
            }.compileIntoEvents())
            engine.addEvents(BlockTextbox(engine).apply {
                this.beat = startBeat + 4f + 2f
                this.requireInput.set(true)
                this.text = Localization.getValue("practice.tutorial1.text1c")
            }.compileIntoEvents())
            engine.addEvents(BlockTextbox(engine).apply {
                this.beat = startBeat + 4f + 3f
                this.requireInput.set(true)
                this.text = Localization.getValue("practice.tutorial1.text1d")
            }.compileIntoEvents())
            engine.addEvents(BlockTextbox(engine).apply {
                this.beat = startBeat + 4f + 4f
                this.requireInput.set(true)
                this.text = Localization.getValue("practice.tutorial1.text1e")
            }.compileIntoEvents())
            engine.addEvents(BlockTextbox(engine).apply {
                this.beat = startBeat + 4f + 5f
                this.requireInput.set(true)
                this.text = Localization.getValue("practice.tutorial1.text1f")
            }.compileIntoEvents())
            engine.addEvents(BlockTextbox(engine).apply {
                this.beat = startBeat + 4f + 6f
                this.requireInput.set(true)
                this.text = Localization.getValue("practice.tutorial1.text2", keyboardKeymap.toDpadString())
            }.compileIntoEvents())
            engine.addEvents(practiceSection2.apply {
                this.beat = startBeat + 4f + 8f
            }.compileIntoEvents())
        }

        
        practiceSection2.initBlock = PracticeInitBlock(8f + 2, 3) { engine, startBeat ->
            val musicData = engine.musicData
            musicData.musicSyncPointBeat = startBeat + 2 + 4f
            musicData.update()
            musicData.setMusicPlayerPositionToCurrentSec()

            for (i in 0 until 4) {
                engine.addEvent(EventCowbellSFX(engine, startBeat + 2 + i, false))
            }

            engine.musicData.volumeMap.addMusicVolume(MusicVolume(startBeat + 2, 0f, 100))
            engine.addEvent(EventLockInputs(engine, false, startBeat + 2))
            engine.addEvent(EventDeployRod(engine, engine.world.rowDpad, startBeat + 2 + 4f))

            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowDpad, 0, EntityPiston.Type.PISTON_DPAD, startBeat + 2 + 4f + 0 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowDpad, 4, EntityPiston.Type.PISTON_DPAD, startBeat + 2 + 4f + 4 * 0.5f))

            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowDpad, 8, EntityPiston.Type.PLATFORM, startBeat + 2 + 4f + 8 * 0.5f, affectThisIndexAndForward = true))
        }
        practiceSection2.loopBlock = PracticeLoopBlock(4f) { engine, startBeat ->
            engine.addEvent(EventDeployRod(engine, engine.world.rowDpad, startBeat))
            engine.addEvent(EventPracticeRetract(engine, engine.world.rowDpad, 0, startBeat + 3.5f, affectThisIndexAndForward = true))
            listOf(0, 4).map { RequiredInput(startBeat + it * 0.5f, InputType.DPAD_ANY) }
        }
        practiceSection2.endBlock = PracticeEndBlock(7f) { engine, startBeat ->
            engine.addEvent(EventLockInputs(engine, true, startBeat))
            engine.addEvent(EventRowBlockRetract(engine, engine.world.rowDpad, 0, startBeat + 2f, affectThisIndexAndForward = true))
            engine.addEvent(EventRowBlockDespawn(engine, engine.world.rowDpad, 0, startBeat + 3f, affectThisIndexAndForward = true))

            engine.musicData.volumeMap.addMusicVolume(MusicVolume(startBeat, 4f, 0))

            engine.addEvents(BlockTextbox(engine).apply {
                this.beat = startBeat + 4f + 0
                this.requireInput.set(true)
                this.text = Localization.getValue("practice.tutorial1.text3")
            }.compileIntoEvents())
            engine.addEvents(BlockTextbox(engine).apply {
                this.beat = startBeat + 4f + 1
                this.requireInput.set(true)
                this.text = Localization.getValue("practice.tutorial1.text4")
            }.compileIntoEvents())
            engine.addEvents(practiceSection3.apply {
                this.beat = startBeat + 4f + 3
            }.compileIntoEvents())
        }
        
        
        practiceSection3.initBlock = PracticeInitBlock(8f + 2, 3) { engine, startBeat ->
            val musicData = engine.musicData
            musicData.musicSyncPointBeat = startBeat + 2 + 4f
            musicData.update()
            musicData.setMusicPlayerPositionToCurrentSec()

            for (i in 0 until 4) {
                engine.addEvent(EventCowbellSFX(engine, startBeat + 2 + i, false))
            }

            engine.musicData.volumeMap.addMusicVolume(MusicVolume(startBeat + 2, 0f, 100))
            engine.addEvent(EventLockInputs(engine, false, startBeat + 2))
            engine.addEvent(EventDeployRod(engine, engine.world.rowA, startBeat + 2 + 4f))
            engine.addEvent(EventDeployRod(engine, engine.world.rowDpad, startBeat + 2 + 4f))

            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowA, 0, EntityPiston.Type.PISTON_A, startBeat + 2 + 4f + 0 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowA, 4, EntityPiston.Type.PISTON_A, startBeat + 2 + 4f + 4 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowA, 8, EntityPiston.Type.PLATFORM, startBeat + 2 + 4f + 8 * 0.5f, affectThisIndexAndForward = true))
            
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowDpad, 0, EntityPiston.Type.PISTON_DPAD, startBeat + 2 + 4f + 0 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowDpad, 4, EntityPiston.Type.PISTON_DPAD, startBeat + 2 + 4f + 4 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowDpad, 8, EntityPiston.Type.PLATFORM, startBeat + 2 + 4f + 8 * 0.5f, affectThisIndexAndForward = true))
        }
        practiceSection3.loopBlock = PracticeLoopBlock(4f) { engine, startBeat ->
            engine.addEvent(EventDeployRod(engine, engine.world.rowA, startBeat))
            engine.addEvent(EventPracticeRetract(engine, engine.world.rowA, 0, startBeat + 3.5f, affectThisIndexAndForward = true))
            engine.addEvent(EventDeployRod(engine, engine.world.rowDpad, startBeat))
            engine.addEvent(EventPracticeRetract(engine, engine.world.rowDpad, 0, startBeat + 3.5f, affectThisIndexAndForward = true))
            listOf(0, 4).flatMap { 
                listOf(
                        RequiredInput(startBeat + it * 0.5f, InputType.A),
                        RequiredInput(startBeat + it * 0.5f, InputType.DPAD_ANY)
                )
            }
        }
        practiceSection3.endBlock = PracticeEndBlock(7f) { engine, startBeat ->
            engine.addEvent(EventLockInputs(engine, true, startBeat))
            engine.addEvent(EventRowBlockRetract(engine, engine.world.rowA, 0, startBeat + 2f, affectThisIndexAndForward = true))
            engine.addEvent(EventRowBlockDespawn(engine, engine.world.rowA, 0, startBeat + 3f, affectThisIndexAndForward = true))
            engine.addEvent(EventRowBlockRetract(engine, engine.world.rowDpad, 0, startBeat + 2f, affectThisIndexAndForward = true))
            engine.addEvent(EventRowBlockDespawn(engine, engine.world.rowDpad, 0, startBeat + 3f, affectThisIndexAndForward = true))

            engine.musicData.volumeMap.addMusicVolume(MusicVolume(startBeat, 4f, 0))

            engine.addEvents(BlockTextbox(engine).apply {
                this.beat = startBeat + 4f + 0
                this.requireInput.set(true)
                this.text = Localization.getValue("practice.tutorial1.text5")
            }.compileIntoEvents())
            engine.addEvents(BlockTextbox(engine).apply {
                this.beat = startBeat + 4f + 1
                this.requireInput.set(true)
                this.text = Localization.getValue("practice.tutorial1.text6")
            }.compileIntoEvents())
            engine.addEvents(practiceSection4.apply {
                this.beat = startBeat + 4f + 3
            }.compileIntoEvents())
        }



        practiceSection4.initBlock = PracticeInitBlock(8f + 2, 3) { engine, startBeat ->
            val musicData = engine.musicData
            musicData.musicSyncPointBeat = startBeat + 2 + 4f
            musicData.update()
            musicData.setMusicPlayerPositionToCurrentSec()

            for (i in 0 until 4) {
                engine.addEvent(EventCowbellSFX(engine, startBeat + 2 + i, false))
            }

            engine.musicData.volumeMap.addMusicVolume(MusicVolume(startBeat + 2, 0f, 100))
            engine.addEvent(EventLockInputs(engine, false, startBeat + 2))
            engine.addEvent(EventDeployRod(engine, engine.world.rowA, startBeat + 2 + 4f))
            engine.addEvent(EventDeployRod(engine, engine.world.rowDpad, startBeat + 2 + 4f))

            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowA, 0, EntityPiston.Type.PISTON_A, startBeat + 2 + 4f + 0 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowA, 2, EntityPiston.Type.PISTON_A, startBeat + 2 + 4f + 2 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowA, 4, EntityPiston.Type.PISTON_A, startBeat + 2 + 4f + 4 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowA, 6, EntityPiston.Type.PISTON_A, startBeat + 2 + 4f + 6 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowA, 8, EntityPiston.Type.PLATFORM, startBeat + 2 + 4f + 8 * 0.5f, affectThisIndexAndForward = true))

            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowDpad, 0, EntityPiston.Type.PISTON_DPAD, startBeat + 2 + 4f + 0 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowDpad, 2, EntityPiston.Type.PISTON_DPAD, startBeat + 2 + 4f + 2 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowDpad, 4, EntityPiston.Type.PISTON_DPAD, startBeat + 2 + 4f + 4 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowDpad, 6, EntityPiston.Type.PISTON_DPAD, startBeat + 2 + 4f + 6 * 0.5f))
            engine.addEvent(EventRowBlockSpawn(engine, engine.world.rowDpad, 8, EntityPiston.Type.PLATFORM, startBeat + 2 + 4f + 8 * 0.5f, affectThisIndexAndForward = true))
        }
        practiceSection4.loopBlock = PracticeLoopBlock(4f) { engine, startBeat ->
            engine.addEvent(EventDeployRod(engine, engine.world.rowA, startBeat))
            engine.addEvent(EventPracticeRetract(engine, engine.world.rowA, 0, startBeat + 3.5f, affectThisIndexAndForward = true))
            engine.addEvent(EventDeployRod(engine, engine.world.rowDpad, startBeat))
            engine.addEvent(EventPracticeRetract(engine, engine.world.rowDpad, 0, startBeat + 3.5f, affectThisIndexAndForward = true))
            listOf(0, 2, 6, 4).flatMap {
                listOf(
                        RequiredInput(startBeat + it * 0.5f, InputType.A),
                        RequiredInput(startBeat + it * 0.5f, InputType.DPAD_ANY)
                )
            }
        }
        practiceSection4.endBlock = PracticeEndBlock(7f) { engine, startBeat ->
            engine.addEvent(EventLockInputs(engine, true, startBeat))
            engine.addEvent(EventRowBlockRetract(engine, engine.world.rowA, 0, startBeat + 2f, affectThisIndexAndForward = true))
            engine.addEvent(EventRowBlockDespawn(engine, engine.world.rowA, 0, startBeat + 3f, affectThisIndexAndForward = true))
            engine.addEvent(EventRowBlockRetract(engine, engine.world.rowDpad, 0, startBeat + 2f, affectThisIndexAndForward = true))
            engine.addEvent(EventRowBlockDespawn(engine, engine.world.rowDpad, 0, startBeat + 3f, affectThisIndexAndForward = true))

            engine.musicData.volumeMap.addMusicVolume(MusicVolume(startBeat, 4f, 0))

            engine.addEvents(BlockTextbox(engine).apply {
                this.beat = startBeat + 4f + 0
                this.requireInput.set(true)
                this.text = Localization.getValue("practice.tutorial1.text7")
            }.compileIntoEvents())
            engine.addEvents(BlockTextbox(engine).apply {
                this.beat = startBeat + 4f + 1
                this.requireInput.set(true)
                this.text = Localization.getValue("practice.tutorial1.text8")
            }.compileIntoEvents())
            engine.addEvent(EventEndState(engine, startBeat + 4f + 3))
        }
    }
    
    override fun initialize() {
        engine.tempos.addTempoChange(TempoChange(0f, 129f))
        
        val music: BeadsMusic = SidemodeAssets.practiceTheme
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 10_000f
        musicData.loopParams = LoopParams(SamplePlayer.LoopType.LOOP_FORWARDS, 0.0, music.musicSample.lengthMs)
        musicData.beadsMusic = music
        musicData.update()
        
        addInitialBlocks()
    }
    
    private fun addInitialBlocks() {
        val blocks = mutableListOf<Block>()

        blocks += ResetMusicVolumeBlock(engine, startingVolume = 0).apply {
            this.beat = 0f
        }
        blocks += BlockTextbox(engine).apply { 
            this.beat = 0f
            this.requireInput.set(true)
            this.text = Localization.getValue("practice.tutorial1.text0", Input.Keys.toString(keyboardKeymap.buttonA))
        }
        blocks += BlockTextbox(engine).apply { 
            this.beat = 1f
            this.requireInput.set(true)
            this.text = Localization.getValue("practice.tutorial1.text1", Input.Keys.toString(keyboardKeymap.buttonA))
        }
        blocks += practiceSection1.apply { 
            this.beat = 3f
        }

        container.addBlocks(blocks)
    }
}