package polyrhythmmania.storymode.gamemode.boss

import com.badlogic.gdx.math.Vector3
import paintbox.util.filterAndIsInstance
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
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.gamemode.AbstractStoryGameMode
import polyrhythmmania.storymode.music.StemCache
import polyrhythmmania.storymode.music.StoryMusicAssets
import polyrhythmmania.world.EntityRodPR
import polyrhythmmania.world.EventDeployRod
import polyrhythmmania.world.EventMoveCameraRelative
import polyrhythmmania.world.EventRowBlockDespawn
import polyrhythmmania.world.tileset.PaletteTransition
import polyrhythmmania.world.tileset.TransitionCurve
import java.util.*


class StoryBossGameMode(main: PRManiaGame)
    : AbstractStoryGameMode(main) {

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
    
    init {
        engine.postRunnable(checkForRodsThatCollidedWithBossRunnable)
    }

    override fun initialize() {
        engine.tempos.addTempoChange(TempoChange(0f, BPM))
        addInitialBlocks()
    }

    private fun addInitialBlocks() {
        val blocks = mutableListOf<Block>()

        val zoomTransition = PaletteTransition.DEFAULT.copy(duration = 4f, transitionCurve = TransitionCurve.SMOOTHER)
        blocks += BlockZoom(engine).apply {
            this.startZoom.set(1f)    
            this.endZoom.set(1.3f * 3 / 3)
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
    
    private fun checkForRodsThatCollidedWithBoss() {
        val blocksAheadOfStart = 11.125f
        val rods = world.entities.filterAndIsInstance<EntityRodPR> { 
            it.position.x > (it.row.startX + blocksAheadOfStart)
        }
        rods.forEach { rod ->
            rod.explode(engine, shouldCountAsMiss = false)
        }
    }
    
    private inner class CheckForRodsThatCollidedWithBossRunnable : Runnable {
        var cancel: Boolean = false
        
        override fun run() {
            checkForRodsThatCollidedWithBoss()
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
        return super.createGlobalContainerSettings().copy(reducedMotion = false)
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
            if (anyA) {
                engine.addEvent(EventDeployRod(engine, world.rowA, patternStart))
                engine.addEvent(EventRowBlockDespawn(engine, world.rowA, 0, patternStart + patternDuration - 0.25f, affectThisIndexAndForward = true))
            }
            if (anyDpad) {
                engine.addEvent(EventDeployRod(engine, world.rowDpad, patternStart))
                engine.addEvent(EventRowBlockDespawn(engine, world.rowDpad, 0, patternStart + patternDuration - 0.25f, affectThisIndexAndForward = true))
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

    //endregion
}
