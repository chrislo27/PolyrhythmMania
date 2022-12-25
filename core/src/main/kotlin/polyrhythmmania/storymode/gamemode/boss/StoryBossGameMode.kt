package polyrhythmmania.storymode.gamemode.boss

import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.gamemodes.GameMode
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
    
    private fun createMusicEvent(stemID: String, beat: Float, measures: Int): BossMusicEvent =
            BossMusicEvent(engine, stems, stemID, beat, (measures * 4).toFloat())
    
    private fun addInitialBlocks() {
        val blocks = mutableListOf<Block>()
//        blocks += InitializationBlock().apply {
//            this.beat = 0f
//        }
        
        // FIXME debug
        blocks += object : Block(engine, EnumSet.noneOf(BlockType::class.java)) {
            override fun compileIntoEvents(): List<Event> {
                return listOf<Event>(
                        createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_INTRO, 0f, 6),
                        createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_A1, 6 * 4f, 8),
                        createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_B2, (6 + 1 * 8) * 4f, 8),
                        createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_C, (6 + 2 * 8) * 4f, 8),
                        createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_B1, (6 + 3 * 8) * 4f, 8),
                        createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_B2, (6 + 4 * 8) * 4f, 8),
                        createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_D, (6 + 5 * 8) * 4f, 8),
                        createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_E1, (6 + 6 * 8) * 4f, 8),
                        createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_E2, (6 + 7 * 8) * 4f, 8),
                        createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_F, (6 + 8 * 8) * 4f, 8),
                        createMusicEvent(StoryMusicAssets.STEM_ID_BOSS_1_A2, (6 + 9 * 8) * 4f, 8),
                ).onEach { 
                    it.beat = this.beat + it.beat
                }
            }

            override fun copy(): Block = throw NotImplementedError()
        }.apply { 
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
}
