package polyrhythmmania.gamemodes.endlessmode

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import net.beadsproject.beads.ugens.SamplePlayer
import paintbox.binding.*
import polyrhythmmania.PRManiaGame
import polyrhythmmania.achievements.Achievements
import polyrhythmmania.world.texturepack.TexturePackSource
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockType
import polyrhythmmania.editor.block.RowSetting
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.EventConditionalOnRods
import polyrhythmmania.engine.EventPlaySFX
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.gamemodes.*
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.statistics.PlayTimeType
import polyrhythmmania.util.RandomBagIterator
import polyrhythmmania.util.Semitones
import polyrhythmmania.world.*
import polyrhythmmania.world.entity.EntityPiston
import polyrhythmmania.world.tileset.PaletteTransition
import polyrhythmmania.world.tileset.TilesetPalette
import java.time.LocalDate
import java.time.Year
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class EndlessPolyrhythm(main: PRManiaGame, playTimeType: PlayTimeType, prevHighScore: EndlessModeScore,
                        /** A 48-bit seed. */ val seed: Long,
                        val dailyChallenge: LocalDate?, val disableLifeRegen: Boolean, maxLives: Int = -1)
    : AbstractEndlessMode(main, prevHighScore, playTimeType) {
    
    companion object {
        private const val COLOR_CHANGE_LIMIT: Int = 18
        private val VALID_COLOR_CHANGE_MULTIPLIERS: List<Int> = listOf(5, 7, 11, 13)

        private fun findPeriods(limit: Int) {
            for (i in 3 until limit) {
                if (limit % i == 0) continue
                print("Testing $i\n  0  ")
                val gotten = mutableSetOf<Int>(0)
                var current = 0
                var iter = 0
                while (true) {
                    current = (current + i) % limit
                    print("$current  ")
                    if (current in gotten) {
                        println()
                        if (gotten.size < limit) {
                            println("   Failed at iteration $iter, ran into $current again.")
                        } else {
                            println("   SUCCESS!")
                        }
                        break
                    }
                    gotten += current
                    iter++
                }
            }
        }
        
        fun getCurrentDailyChallengeDate(): LocalDate {
            return ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()
        }
        
        fun getSeedFromHexString(hex: String): Long {
            if (hex.isEmpty()) return 0L
            return hex.takeLast(8).toUInt(16).toLong() and 0x0_FFFF_FFFFL
        }
        
        fun getSeedString(seed: UInt): String {
            return seed.toString(16).uppercase().padStart(8, '0')
        }
        
        fun getSeedFromLocalDate(date: LocalDate): Long {
            return (date.dayOfYear and 0b111111111 /* 9 bits */).toLong() or (((date.year.toLong() % Year.MAX_VALUE) and 0xFFFF_FFFF) shl 9) or (1L shl 42) 
        }
    }
    
    private enum class RecoveryAchievementState {
        NOT_READY, READY, AWARDED;
    }
    
    val dailyChallengeUUIDNonce: Var<UUID?> = Var(null)
    
    private val colorChangeMultiplier: Int = VALID_COLOR_CHANGE_MULTIPLIERS[seed.toInt().absoluteValue % VALID_COLOR_CHANGE_MULTIPLIERS.size]
    val random: Random = Random(seed)
    val difficultyBags: Map<Difficulty, RandomBagIterator<Pattern>> = EndlessPatterns.patternsByDifficulty.entries.associate { 
        it.key to RandomBagIterator(it.value, random, RandomBagIterator.ExhaustionBehaviour.SHUFFLE_EXCLUDE_LAST)
    }
    val difficultyFactor: FloatVar = FloatVar(0f)
    val loopsCompleted: IntVar = IntVar(0)
    val speedIncreaseLevel: IntVar = IntVar(0)
    val speedIncreaseSemitones: ReadOnlyIntVar = IntVar(eager = true) { EndlessSpeedup.map(speedIncreaseLevel.use()) }
    
    // For pausing in between pattern achievement.
    // pauseTime is sent by PlayScreen, and accepted only if currently in pattern
    private var currentlyInPattern: Boolean = false
    private var pauseTime: Float = 0f
    
    // For daily recovery achievement.
    private var hasGottenDownToOneLifeAfterTwoSpeedups: RecoveryAchievementState = RecoveryAchievementState.NOT_READY

    init {
        container.texturePackSource.set(TexturePackSource.StockHD)
        TilesetPalette.createGBA1TilesetPalette().applyTo(container.renderer.tileset)
        container.world.tilesetPalette.copyFrom(container.renderer.tileset)
        
        container.world.worldMode = WorldMode(WorldType.Polyrhythm())
        val endlessScore = container.engine.modifiers.endlessScore
        endlessScore.enabled.set(true)
        endlessScore.flashHudRedWhenLifeLost = true
        endlessScore.maxLives.set(if (maxLives <= 0) 3 else maxLives)
        container.renderer.endlessModeRendering.endlessModeSeed.set(getSeedString(seed.toUInt()))
        container.renderer.endlessModeRendering.dailyChallengeDate.set(dailyChallenge)
    }
    
    fun submitPauseTime(pauseTime: Float) {
        if (currentlyInPattern) {
            this.pauseTime = pauseTime
        }
    }

    override fun renderUpdate() {
        // Recovery achievement
        if (dailyChallenge != null && loopsCompleted.get() >= 2) {
            val endlessScore = container.engine.modifiers.endlessScore
            when (hasGottenDownToOneLifeAfterTwoSpeedups) {
                RecoveryAchievementState.NOT_READY -> {
                    if (endlessScore.lives.get() == 1) {
                        hasGottenDownToOneLifeAfterTwoSpeedups = RecoveryAchievementState.READY
                    }
                }
                RecoveryAchievementState.READY -> {
                    if (endlessScore.lives.get() >= endlessScore.maxLives.get()) {
                        hasGottenDownToOneLifeAfterTwoSpeedups = RecoveryAchievementState.AWARDED
                        Achievements.awardAchievement(Achievements.dailyRecovery)
                    }
                }
                RecoveryAchievementState.AWARDED -> {}
            }
        }
    }

    override fun initialize() {
        engine.tempos.addTempoChange(TempoChange(0f, 129f))

        val music: BeadsMusic = SidemodeAssets.polyrhythmTheme // Music loop (Polyrhythm) is 88 beats long
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 0f
        musicData.loopParams = LoopParams(SamplePlayer.LoopType.LOOP_FORWARDS, 0.0, music.musicSample.lengthMs)
        musicData.rate = 1f
        musicData.firstBeatSec = 0f
        musicData.beadsMusic = music
        musicData.volumeMap.addMusicVolume(MusicVolume(0f, 0f, 100))
        musicData.update()
        
        addInitialBlocks()
    }
    
    private fun addInitialBlocks() {
        val blocks = mutableListOf<Block>()
        blocks += InitializationBlock().apply {
            this.beat = 0f
        }

        container.addBlocks(blocks)
    }
    
    fun nextGaussianAbs(stdDev: Float, mean: Float): Float {
        return (random.nextGaussian().absoluteValue * stdDev + mean).toFloat()
    }
    
    fun getMeanFromDifficulty(): Float = (difficultyFactor.get() / 2.5f).coerceIn(0.25f, 2.25f)
    fun getStdDevFromDifficulty(): Float = MathUtils.lerp(0.5f, 0.85f, (difficultyFactor.get() / 2.5f).coerceIn(0f, 1f))

    override fun getDebugString(): String {
        return """seed: ${if (dailyChallenge != null) "daily challenge" else seed.toString(16).uppercase()}
difficultyFactor: ${difficultyFactor.get()}
distribution: mean = ${getMeanFromDifficulty()}, stddev = ${getStdDevFromDifficulty()}
loops: ${loopsCompleted.get()} / speed: Lvl ${speedIncreaseLevel.get()} (${speedIncreaseSemitones.get()} semitones)
currentlyInPattern: $currentlyInPattern | pauseTime: $pauseTime
""".dropLast(1)
    }
    
    private fun createTilesetPaletteIterated(iteration: Int, multiplier: Int, limit: Int): TilesetPalette {
        val hueChange = (((iteration * multiplier) % limit) / limit.toFloat()) * 360f
        val loops = iteration / limit
        return TilesetPalette.createGBA1TilesetPalette().also { p ->
            val hsv = FloatArray(3)
            listOf(p.cubeBorder, p.cubeBorderZ, p.cubeFaceX, p.cubeFaceY, p.cubeFaceZ, p.pistonAFaceX, p.pistonAFaceZ,
                    p.pistonDpadFaceX, p.pistonDpadFaceZ, p.signShadow).forEach { colorMapping ->
                val color = colorMapping.color.getOrCompute()
                color.toHsv(hsv)
                hsv[0] = (hsv[0] + hueChange) % 360f
                hsv[0] += loops * 10f
                colorMapping.color.set(Color(1f, 1f, 1f, 1f).fromHsv(hsv))
            }
        }
    }
    
    inner class ChangeInPatternFlagEvent(startBeat: Float, val newValue: Boolean) : Event(engine) {
        init {
            this.beat = startBeat
        }
        
        override fun onStart(currentBeat: Float) {
            super.onStart(currentBeat)
            currentlyInPattern = newValue
            if (newValue) {
                pauseTime = 0f
            }
        }
    }

    /**
     * Generates a pattern when started.
     */
    inner class PatternGeneratorEvent(startBeat: Float, val delay: Float) : Event(engine) {
        
        init {
            this.beat = startBeat
        }
        
        override fun onStart(currentBeat: Float) {
            super.onStart(currentBeat)

            val gaussian = nextGaussianAbs(getStdDevFromDifficulty(), getMeanFromDifficulty()).coerceAtLeast(0f)
            val difficultyInt = gaussian.roundToInt()
            val diff = Difficulty.VALUES[difficultyInt % Difficulty.VALUES.size]
            
            var pattern = difficultyBags.getValue(diff).next()
            if (pattern.flippable) {
                if (random.nextBoolean()) {
                    pattern = pattern.flip()
                }
            }
            
            val patternDuration: Int = 4 + 4 /* 4 beats setup, 4 beats pattern and teardown in one */
            val patternStart: Float = this.beat + delay

            val patternEvents = pattern.toEvents(engine, patternStart)
            engine.addEvents(patternEvents)

            // For pausing in middle achievement
            if (patternEvents.isNotEmpty()) {
                var earliest = Float.POSITIVE_INFINITY
                var latest = Float.NEGATIVE_INFINITY
                for (evt in patternEvents) {
                    if (evt is EventRowBlockSpawn && evt.type != EntityPiston.Type.PLATFORM) {
                        val b = evt.beat
                        if (b < earliest) {
                            earliest = b
                        }
                        if (b > latest) {
                            latest = b
                        }
                    }
                }
                if (earliest.isFinite() && latest.isFinite()) {
                    engine.addEvent(ChangeInPatternFlagEvent(4f + earliest, true))
                    engine.addEvent(ChangeInPatternFlagEvent(4f + latest, false))
                }
            }
            
            val anyA = pattern.rowA.row.isNotEmpty()
            val anyDpad = pattern.rowDpad.row.isNotEmpty()
            val lifeLostVar = BooleanVar(false)
            if (anyA) {
                engine.addEvent(EventDeployRodEndless(engine, world.rowA, patternStart, lifeLostVar))
                engine.addEvent(EventRowBlockDespawn(engine, world.rowA, 0, patternStart + patternDuration - 0.25f, affectThisIndexAndForward = true))
            }
            if (anyDpad) {
                engine.addEvent(EventDeployRodEndless(engine, world.rowDpad, patternStart, lifeLostVar))
                engine.addEvent(EventRowBlockDespawn(engine, world.rowDpad, 0, patternStart + patternDuration - 0.25f, affectThisIndexAndForward = true))
            }
            
            if (anyA || anyDpad) {
                val awardScoreBeat = patternStart + patternDuration + 0.01f
                val onFailure: () -> Unit = {
                    container.renderer.endlessModeRendering.triggerScoreNGInput()
                }
                engine.addEvent(EventConditionalOnRods(engine, awardScoreBeat,
                        if (anyA && anyDpad) RowSetting.BOTH else if (anyA) RowSetting.ONLY_A else RowSetting.ONLY_DPAD, true, onFailure) {
                    engine.addEvent(EventIncrementEndlessScore(engine) { newScore ->
                        val endlessScore = engine.modifiers.endlessScore
                        val currentLives = endlessScore.lives.get()
                        val maxLives = endlessScore.maxLives.get()
                        if (!disableLifeRegen && newScore >= 10 && newScore % 10 == 0 && currentLives > 0 && currentLives < maxLives) {
                            engine.addEvent(EventPlaySFX(engine, awardScoreBeat, "sfx_practice_moretimes_2"))
                            endlessScore.lives.set(currentLives + 1)
                            if (engine.areStatisticsEnabled) {
                                GlobalStats.livesGainedEndless.increment()
                            }
                        } else {
                            engine.addEvent(EventPlaySFX(engine, awardScoreBeat, "sfx_practice_moretimes_1"))
                        }

                        // For pausing in middle achievement
                        if (pauseTime >= 0.5f) {
                            Achievements.attemptAwardScoreAchievement(Achievements.endlessPauseBetweenInputs, newScore)
                        }
                        pauseTime = 0f
                        currentlyInPattern = false
                    }.also {
                        it.beat = awardScoreBeat
                    })
                })
            }
            
            // Loop
            engine.addEvent(PatternGeneratorEvent(patternStart + 4, 4f))
        }

        override fun onEnd(currentBeat: Float) {
            super.onEnd(currentBeat)
            difficultyFactor.set(difficultyFactor.get() + 0.1f)
        }
    }
    
    inner class InitializationBlock : Block(engine, EnumSet.allOf(BlockType::class.java)) {
        override fun compileIntoEvents(): List<Event> {
            return listOf(
                    object : Event(engine) {
                        override fun onStart(currentBeat: Float) {
                            random.setSeed(this@EndlessPolyrhythm.seed)
                            difficultyBags.values.forEach {
                                it.resetToOriginalOrder()
                                it.shuffle()
                            }
                            difficultyFactor.set(0f)
                            loopsCompleted.set(0)
                            speedIncreaseLevel.set(0)
                            engine.playbackSpeed = 1f
                            currentlyInPattern = false
                            pauseTime = 0f
                            hasGottenDownToOneLifeAfterTwoSpeedups = RecoveryAchievementState.NOT_READY
                        }
                    }.also { e ->
//                        e.beat = this.beat
                        e.beat = -10000f
                    },
                    
                    EventRowBlockRetract(engine, world.rowA, 0, 6f, affectThisIndexAndForward = true),
                    EventRowBlockRetract(engine, world.rowDpad, 0, 6f, affectThisIndexAndForward = true),
                    EventRowBlockDespawn(engine, world.rowA, 0, 7f, affectThisIndexAndForward = true),
                    EventRowBlockDespawn(engine, world.rowDpad, 0, 7f, affectThisIndexAndForward = true),
                    
                    LoopingEvent(engine, 88f, { true }) { engine, startBeat ->
                        loopsCompleted.set(loopsCompleted.get() + 1)
                        val currentSpeedIncrease = speedIncreaseLevel.get()
                        val maxSpeedIncrease = EndlessSpeedup.MAX_LEVEL
                        val newSpeedLvl = (currentSpeedIncrease + 1).coerceAtMost(maxSpeedIncrease)
                        speedIncreaseLevel.set(newSpeedLvl)
                        engine.playbackSpeed = Semitones.getALPitch(speedIncreaseSemitones.get())
                        
                        engine.addEvent(EventPaletteChange(engine, startBeat,
                                PaletteTransition(duration = 1f, pulseMode = false, reverse = false),
                                createTilesetPaletteIterated(loopsCompleted.get(), colorChangeMultiplier, COLOR_CHANGE_LIMIT)))
                        
                        if (newSpeedLvl > currentSpeedIncrease) {
                            container.renderer.endlessModeRendering.triggerSpeedUpText()
                        }
                        
                        if (engine.areStatisticsEnabled && newSpeedLvl >= maxSpeedIncrease && currentSpeedIncrease < newSpeedLvl) {
                            Achievements.awardAchievement(Achievements.endlessReachMaxSpeed)
                        }
                    }.also { e ->
                        e.beat = 88f
                    },
                    
                    PatternGeneratorEvent(this.beat + 1, delay = 8f - 1),
            )
        }

        override fun copy(): Block = throw NotImplementedError()
    }

}