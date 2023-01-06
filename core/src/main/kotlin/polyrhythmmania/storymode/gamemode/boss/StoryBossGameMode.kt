package polyrhythmmania.storymode.gamemode.boss

import com.badlogic.gdx.math.Vector3
import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.GlobalContainerSettings
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.editor.block.BlockZoom
import polyrhythmmania.editor.block.GenericBlock
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.gamemodes.LoopingEvent
import polyrhythmmania.gamemodes.endlessmode.EndlessPatterns
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.gamemode.AbstractStoryGameMode
import polyrhythmmania.storymode.music.StemCache
import polyrhythmmania.storymode.music.StoryMusicAssets
import polyrhythmmania.world.*
import polyrhythmmania.world.entity.Entity
import polyrhythmmania.world.entity.EntityCube
import polyrhythmmania.world.render.ForceTexturePack
import polyrhythmmania.world.tileset.PaletteTransition
import polyrhythmmania.world.tileset.TransitionCurve
import java.util.*


class StoryBossGameMode(main: PRManiaGame)
    : AbstractStoryGameMode(main), World.WorldResetListener {

    companion object {
        private const val INTRO_CARD_TIME_SEC: Float = 2.5f // Duration of intro segment
        const val BPM: Float = 186f

        private const val SEGMENT_BEATS_PER_MEASURE: Int = 4
        private const val SEGMENT_DURATION_MEASURES: Int = 8

        fun getFactory(): Contract.GamemodeFactory = object : Contract.GamemodeFactory {
            private var firstCall = true

            override fun load(delta: Float, main: PRManiaGame): GameMode? {
                return if (firstCall) {
                    firstCall = false
                    StoryMusicAssets.initBossStems()
                    null
                } else {
                    val bossStems = StoryMusicAssets.bossStems
                    val keys = bossStems.keys
                    val ready = keys.all { key ->
                        val stem = bossStems.getOrLoad(key)
                        stem?.musicFinishedLoading?.get() ?: false
                    }

                    if (ready) StoryBossGameMode(main) else null
                }
            }
        }
    }

    private val stems: StemCache = StoryMusicAssets.bossStems
    private val checkForRodsThatCollidedWithBossRunnable = CheckForRodsThatCollidedWithBossRunnable()
    
    private val modifierModule: BossModifierModule
    
    init {
        world.worldMode = WorldMode(WorldType.Polyrhythm())
        world.showInputFeedback = true
        world.worldResetListeners += this as World.WorldResetListener
        
        engine.postRunnable(checkForRodsThatCollidedWithBossRunnable)
        
        modifierModule = BossModifierModule(engine.modifiers, this)
        engine.modifiers.addModifierModule(modifierModule)
    }

    override fun initialize() {
        engine.tempos.addTempoChange(TempoChange(0f, BPM))
        addInitialBlocks()
    }

    override fun onWorldReset(world: World) {
        val list = mutableListOf<Entity>()
        // This part is necessary for the story mode boss level (zooms way out) 
        for (x in 9..13) {
            for (z in -11 downTo -13) {
                val ent: Entity = EntityCube(world, false)
                list += ent.apply {
                    this.position.set(x.toFloat(), 3f, z.toFloat())
                }
            }
        }
        
        list.forEach(world::addEntity)
    }

    private fun addInitialBlocks() {
        val blocks = mutableListOf<Block>()

        val zoomTransition = PaletteTransition.DEFAULT.copy(duration = 4f, transitionCurve = TransitionCurve.SMOOTHER)
        blocks += BlockZoom(engine).apply {
            this.startZoom.set(1f)    
            this.endZoom.set(1.3f)
            this.transitionData.paletteTransition.set(zoomTransition)
            this.beat = 0f
        }
        blocks += GenericBlock(engine) {
            listOf(EventMoveCameraRelative(engine, this.beat, zoomTransition, Vector3(1f, 1f, 0f)))
        }.apply { 
            this.beat = 0f
        }
        
        blocks += MusicInitializationBlock().apply {
            this.beat = 0f
        }

        container.addBlocks(blocks)
    }
    
    
    private inner class CheckForRodsThatCollidedWithBossRunnable : Runnable {
        var cancel: Boolean = false
        
        override fun run() {
            modifierModule.checkForRodsThatCollidedWithBoss()
            if (!cancel) {
                engine.postRunnable(this)
            }
        }
    }
    
    //region GameMode overrides

    override fun getIntroCardTimeOverride(): Float {
        return INTRO_CARD_TIME_SEC
    }

    override fun getSecondsToDelayAtStartOverride(): Float {
        return 0f
    }

    override fun shouldPauseWhileInIntroCardOverride(): Boolean {
        return false
    }

    override fun createGlobalContainerSettings(): GlobalContainerSettings {
        return super.createGlobalContainerSettings().copy(forceTexturePack = ForceTexturePack.FORCE_GBA, reducedMotion = false)
    }

    //endregion

    //region Blocks and events

    private fun createMusicEvent(stemID: String, beat: Int, measures: Int): BossMusicEvent =
            BossMusicEvent(engine, stems, stemID, beat.toFloat(), (measures * 4).toFloat())

    private fun createMusicLoopingEvent(offsetBeat: Float): LoopingEvent {
        val stemIDs = listOf(
                StoryMusicAssets.STEM_ID_BOSS_1_C,
                StoryMusicAssets.STEM_ID_BOSS_1_B1,
                StoryMusicAssets.STEM_ID_BOSS_1_B2,
                StoryMusicAssets.STEM_ID_BOSS_1_D,
                StoryMusicAssets.STEM_ID_BOSS_1_E1,
                StoryMusicAssets.STEM_ID_BOSS_1_E2,
                StoryMusicAssets.STEM_ID_BOSS_1_F,
                StoryMusicAssets.STEM_ID_BOSS_1_A2,
        )
        val loopDurationBeats = stemIDs.size * SEGMENT_DURATION_MEASURES * SEGMENT_BEATS_PER_MEASURE
        return LoopingEvent(engine, loopDurationBeats.toFloat(), { engine ->
            true
        }) { engine, startBeat ->
            val events = stemIDs.map { id ->
                createMusicEvent(id, 0, SEGMENT_DURATION_MEASURES)
            }

            events.fold(startBeat - offsetBeat) { acc, evt ->
                evt.beat = acc
                acc + evt.width
            }

            engine.addEvents(events)
        }.apply {
            this.beat = offsetBeat
        }
    }
    
    private fun createRowBlockDespawnEvents(row: Row, startBeat: Float): List<EventRowBlockDespawn> =
            (0 until 10).map { idx -> EventRowBlockDespawn(engine, row, idx, startBeat, affectThisIndexAndForward = false) }

    private fun createRandomPatternLoopingEvent(offsetBeat: Float): LoopingEvent {
        val patternDuration = 8f
        return LoopingEvent(engine, patternDuration, { engine ->
            true
        }) { engine, startBeat ->
            val pattern = EndlessPatterns.allPatterns.random()
            val patternStart = startBeat - offsetBeat
            
            engine.addEvents(pattern.toEvents(engine, patternStart))
            
            val anyA = pattern.rowA.row.isNotEmpty()
            val anyDpad = pattern.rowDpad.row.isNotEmpty()
            val bossDamageMultiplier = if (anyA && anyDpad) 1 else 2
            val damageTakenVar = EntityRodPRStoryBoss.PlayerDamageTaken()
            val despawnStartBeat = patternStart + patternDuration - 0.25f
            if (anyA) {
                engine.addEvent(EventDeployRodBoss(world.rowA, patternStart, damageTakenVar, bossDamageMultiplier))
//                engine.addEvents(createRowBlockDespawnEvents(world.rowA, despawnStartBeat))
                engine.addEvent(EventRowBlockDespawn(engine, world.rowA, 0, despawnStartBeat, affectThisIndexAndForward = true))
            }
            if (anyDpad) {
                engine.addEvent(EventDeployRodBoss(world.rowDpad, patternStart, damageTakenVar, bossDamageMultiplier))
//                engine.addEvents(createRowBlockDespawnEvents(world.rowDpad, despawnStartBeat))
                engine.addEvent(EventRowBlockDespawn(engine, world.rowDpad, 0, despawnStartBeat, affectThisIndexAndForward = true))
            }
            
            engine.addEvent(ClearInputsEvent(engine, patternDuration).apply { 
                this.beat = patternStart
            })
        }.apply {
            this.beat = offsetBeat
        }
    }
    
    private class ClearInputsEvent(engine: Engine, val patternDuration: Float) : Event(engine) {
        override fun onStart(currentBeat: Float) {
            engine.inputter.clearInputs(this.beat - patternDuration)
        }
    }

    private abstract inner class AbstractBlock : Block(engine, EnumSet.allOf(BlockType::class.java)) {
        final override fun copy(): Block = throw NotImplementedError()
    }

    private inner class MusicInitializationBlock : AbstractBlock() {
        override fun compileIntoEvents(): List<Event> {
            var startBeat = 0f
            return listOf(
                    createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_INTRO, beat = 0, measures = 6),
                    createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_A1, beat = 0, measures = SEGMENT_DURATION_MEASURES),

                    createMusicLoopingEvent(-(SEGMENT_BEATS_PER_MEASURE).toFloat()),
            ).onEach { evt ->
                evt.beat += startBeat
                startBeat += evt.width
            } + listOf(
                    createRandomPatternLoopingEvent(-2f).apply {
                        this.beat += 8f
                    }
            )
        }
    }

    private inner class EventDeployRodBoss(
            val row: Row, startBeat: Float,
            val damageTakenVar: EntityRodPRStoryBoss.PlayerDamageTaken, val bossDamageMultiplier: Int
    ) : Event(engine) {
        
        init {
            this.beat = startBeat
        }

        override fun onStart(currentBeat: Float) {
            super.onStart(currentBeat)
            val rod = EntityRodPRStoryBoss(engine.world, this.beat, row, damageTakenVar, bossDamageMultiplier)
            engine.world.addEntity(rod)

            if (engine.areStatisticsEnabled) {
                GlobalStats.rodsDeployed.increment()
                GlobalStats.rodsDeployedPolyrhythm.increment()
            }
        }
    }

    //endregion
}
