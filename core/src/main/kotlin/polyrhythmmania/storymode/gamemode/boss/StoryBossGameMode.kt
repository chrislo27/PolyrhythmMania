package polyrhythmmania.storymode.gamemode.boss

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.GlobalContainerSettings
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.gamemode.AbstractStoryGameMode
import polyrhythmmania.storymode.gamemode.boss.pattern.BossPatternPools
import polyrhythmmania.storymode.gamemode.boss.scripting.*
import polyrhythmmania.storymode.music.StemCache
import polyrhythmmania.storymode.music.StoryMusicAssets
import polyrhythmmania.storymode.screen.StoryPlayScreen
import polyrhythmmania.world.World
import polyrhythmmania.world.WorldMode
import polyrhythmmania.world.WorldType
import polyrhythmmania.world.entity.Entity
import polyrhythmmania.world.entity.EntityCube
import polyrhythmmania.world.render.ForceTexturePack
import java.util.*


class StoryBossGameMode(main: PRManiaGame, val debugPhase: DebugPhase = DebugPhase.NONE) :
    AbstractStoryGameMode(main), World.WorldResetListener {

    companion object {

        private const val INTRO_CARD_TIME_SEC: Float = 2.5f // Duration of intro segment
        const val BPM: Float = 186f

        private const val SEGMENT_BEATS_PER_MEASURE: Int = 4
        private const val SEGMENT_DURATION_MEASURES: Int = 8

        fun getFactory(debugPhase: DebugPhase = DebugPhase.NONE): Contract.GamemodeFactory = object : Contract.GamemodeFactory {
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

                    if (ready) StoryBossGameMode(main, debugPhase) else null
                }
            }
        }
    }
    
    enum class DebugPhase {
        NONE,
        A1,
        A2,
        B1,
        B2,
        C,
        C_VAR1,
        C_VAR2,
        C_VAR3,
        D,
        D_VAR1,
        D_VAR2,
        D_VAR3,
        E1,
        E2,
        F,
        F_VAR1,
        F_VAR2,
        F_VAR3,
    }

    private val checkForRodsThatCollidedWithBossRunnable = CheckForRodsThatCollidedWithBossRunnable()

    val stems: StemCache = StoryMusicAssets.bossStems
    val modifierModule: BossModifierModule

    val random: Random = Random()
    val patternPools: BossPatternPools = BossPatternPools(random)

    init {
        world.worldMode = WorldMode(WorldType.Polyrhythm(showRaisedPlatformsRepeated = false))
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

    override fun prepareFirstTimeWithStoryPlayScreen(playScreen: StoryPlayScreen) {
        super.prepareFirstTimeWithStoryPlayScreen(playScreen)
    }

    override fun onWorldReset(world: World) {
        val list = mutableListOf<Entity>()

        // Extra blocks due to more extreme zoom-out
        list.addAll(createExtraBlockEntities())

        val bossPosition = Vector3(5 + 11f, 1f + (14 / 32f), -3f)
        list += EntityBossRobotUpside(world, this, bossPosition)
        list += EntityBossRobotMiddle(world, this, bossPosition)
        list += EntityBossRobotDownside(world, this, bossPosition)

        list.forEach(world::addEntity)

        patternPools.allPools.forEach { pool ->
            pool.resetAndShuffle()
        }

        val ambientLight = world.spotlights.ambientLight
        ambientLight.color.set(Color.BLACK)
        ambientLight.strength = 0f
    }

    private fun createExtraBlockEntities(): List<Entity> {
        val list = mutableListOf<Entity>()
        
        fun addCube(x: Int, y: Int, z: Int) {
            list += EntityCube(world, false).apply {
                this.position.set(x.toFloat(), y.toFloat(), z.toFloat())
            }
        }

        for (x in 8..15) addCube(x, 3, -11)
        for (x in 8..15) addCube(x, 3, -12)
        for (x in 9..14) addCube(x, 3, -13)
        for (x in 10..13) addCube(x, 3, -14)
        for (x in 11..12) addCube(x, 3, -15)
        
        for (x in 11..14) addCube(x, 0, 10)
        for (x in 12..13) addCube(x, 0, 11)
        
//        for (x in 8..13) {
//            for (z in -12 downTo -13) {
//                list += EntityCube(world, false).apply {
//                    this.position.set(x.toFloat(), 3f, z.toFloat())
//                }
//            }
//        }

        return list
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
        return INTRO_CARD_TIME_SEC * 0
    }

    override fun getSecondsToDelayAtStartOverride(): Float {
        return 0f
    }

    override fun shouldPauseWhileInIntroCardOverride(): Boolean {
        return false
    }

    override fun createGlobalContainerSettings(): GlobalContainerSettings {
        return super.createGlobalContainerSettings().copy(
            forceTexturePack = ForceTexturePack.FORCE_GBA, 
            reducedMotion = false,
            numberOfSpotlightsOverride = 16
        )
    }

    //endregion

    //region Init blocks
    
    private fun onScriptCreated(script: Script) {
        val phase1Factory =
            when (this.debugPhase) {
                DebugPhase.NONE -> null
                DebugPhase.A1 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(BossScriptPhase1A1(it))
                    }
                DebugPhase.A2 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(BossScriptPhase1A2(it))
                    }
                DebugPhase.B1 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(BossScriptPhase1B1(it))
                    }
                DebugPhase.B2 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(BossScriptPhase1B2(it))
                    }
                DebugPhase.C -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(
                            BossScriptPhase1C(it, variantIndex = 0),
                            BossScriptPhase1C(it, variantIndex = 1),
                            BossScriptPhase1C(it, variantIndex = 2),
                        )
                    }
                DebugPhase.C_VAR1 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(
                            BossScriptPhase1C(it, variantIndex = 0),
                        )
                    }
                DebugPhase.C_VAR2 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(
                            BossScriptPhase1C(it, variantIndex = 1),
                        )
                    }
                DebugPhase.C_VAR3 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(
                            BossScriptPhase1C(it, variantIndex = 2),
                        )
                    }
                DebugPhase.D -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(
                            BossScriptPhase1D(it, variantIndex = 0),
                            BossScriptPhase1D(it, variantIndex = 1),
                            BossScriptPhase1D(it, variantIndex = 2),
                        )
                    }
                DebugPhase.D_VAR1 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(
                            BossScriptPhase1D(it, variantIndex = 0),
                        )
                    }
                DebugPhase.D_VAR2 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(
                            BossScriptPhase1D(it, variantIndex = 1),
                        )
                    }
                DebugPhase.D_VAR3 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(
                            BossScriptPhase1D(it, variantIndex = 2),
                        )
                    }
                DebugPhase.E1 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(
                            BossScriptPhase1E1(it)
                        )
                    }
                DebugPhase.E2 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(
                            BossScriptPhase1E2(it)
                        )
                    }
                DebugPhase.F -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(
                            BossScriptPhase1F(it, variantIndex = 0),
                            BossScriptPhase1F(it, variantIndex = 1),
                            BossScriptPhase1F(it, variantIndex = 2),
                        )
                    }
                DebugPhase.F_VAR1 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(
                            BossScriptPhase1F(it, variantIndex = 0),
                        )
                    }
                DebugPhase.F_VAR2 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(
                            BossScriptPhase1F(it, variantIndex = 1),
                        )
                    }
                DebugPhase.F_VAR3 -> fun(intro: BossScriptIntro) =
                    BossScriptPhase1DebugLoop(intro.gamemode, intro.script) {
                        listOf(
                            BossScriptPhase1F(it, variantIndex = 2),
                        )
                    }
            }
        val func: ScriptFunction = BossScriptIntro(
            this, 
            script,
            phase1Factory
        )
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
