package polyrhythmmania.storymode.gamemode.boss

import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.gamemodes.LoopingEvent
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.gamemode.AbstractStoryGameMode
import polyrhythmmania.storymode.music.StemCache
import polyrhythmmania.storymode.music.StoryMusicAssets
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

    override fun initialize() {
        engine.tempos.addTempoChange(TempoChange(0f, BPM))
        addInitialBlocks()
    }

    private fun addInitialBlocks() {
        val blocks = mutableListOf<Block>()
        
        blocks += MusicInitializationBlock().apply {
            this.beat = 0f
        }

        container.addBlocks(blocks)
    }

    override fun getIntroCardTimeOverride(): Float {
        return INTRO_CARD_TIME_SEC
    }

    override fun getSecondsToDelayAtStartOverride(): Float {
        return 0f
    }

    override fun shouldPauseWhileInIntroCardOverride(): Boolean {
        return false
    }

    // Blocks and events below

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

    private abstract inner class AbstractBlock : Block(engine, EnumSet.allOf(BlockType::class.java)) {
        final override fun copy(): Block = throw NotImplementedError()
    }

    private inner class MusicInitializationBlock : AbstractBlock() {
        override fun compileIntoEvents(): List<Event> {
            var startBeat = 0f
            return listOf(
                    createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_INTRO, beat = 0, measures = 6),
                    createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_A1, beat = 0, measures = SEGMENT_DURATION_MEASURES),

                    createMusicLoopingEvent(-(SEGMENT_BEATS_PER_MEASURE).toFloat())
            ).onEach { evt ->
                evt.beat += startBeat
                startBeat += evt.width
            }
        }
    }
}
