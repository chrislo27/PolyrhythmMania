package polyrhythmmania.storymode.gamemode.boss

import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.GlobalContainerSettings
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.gamemodes.endlessmode.Difficulty
import polyrhythmmania.gamemodes.endlessmode.Pattern
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.gamemode.AbstractStoryGameMode
import polyrhythmmania.storymode.gamemode.boss.scripting.BossScriptIntro
import polyrhythmmania.storymode.gamemode.boss.scripting.Script
import polyrhythmmania.storymode.gamemode.boss.scripting.ScriptFunction
import polyrhythmmania.storymode.music.StemCache
import polyrhythmmania.storymode.music.StoryMusicAssets
import polyrhythmmania.util.RandomBagIterator
import polyrhythmmania.world.World
import polyrhythmmania.world.WorldMode
import polyrhythmmania.world.WorldType
import polyrhythmmania.world.entity.Entity
import polyrhythmmania.world.entity.EntityCube
import polyrhythmmania.world.render.ForceTexturePack
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

    private val checkForRodsThatCollidedWithBossRunnable = CheckForRodsThatCollidedWithBossRunnable()
    
    val stems: StemCache = StoryMusicAssets.bossStems
    val modifierModule: BossModifierModule

    val random: Random = Random()
    val difficultyBags: Map<Difficulty, RandomBagIterator<Pattern>> = BossInputPatterns.patternsByDifficulty.entries.associate {
        it.key to RandomBagIterator(it.value, random, RandomBagIterator.ExhaustionBehaviour.SHUFFLE_EXCLUDE_LAST)
    }

    init {
        world.worldMode = WorldMode(WorldType.Polyrhythm())
        world.showInputFeedback = true
        world.worldResetListeners += this as World.WorldResetListener
        
        modifierModule = BossModifierModule(engine.modifiers, this)
        engine.modifiers.addModifierModule(modifierModule)
        
        engine.postRunnable(checkForRodsThatCollidedWithBossRunnable)
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

        difficultyBags.entries.forEach { (difficulty, iter) ->
            iter.resetToOriginalOrder()
            if (difficulty == Difficulty.EASY) {
                iter.shuffleAndExclude(BossInputPatterns.firstPattern)
            } else {
                iter.shuffle()
            }
        }
    }

    private fun addInitialBlocks() {
        val blocks = mutableListOf<Block>()

        blocks += InitScriptBlock()

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

    //region Init blocks
    
    private fun onScriptCreated(script: Script) {
        val func: ScriptFunction = BossScriptIntro(this, script)
        script.addEventsToQueue(func.getEvents())
    }

    private abstract inner class AbstractBlock : Block(engine, EnumSet.allOf(BlockType::class.java)) {
        final override fun copy(): Block = throw NotImplementedError()
    }
    
    private inner class InitScriptBlock : AbstractBlock() {
        override fun compileIntoEvents(): List<Event> {
            val script = Script(0f, this@StoryBossGameMode, 5f) // 1 + 4, 4 for deploy rods

            this@StoryBossGameMode.onScriptCreated(script)
            
            return listOf(script)
        }
    }

    //endregion
}
