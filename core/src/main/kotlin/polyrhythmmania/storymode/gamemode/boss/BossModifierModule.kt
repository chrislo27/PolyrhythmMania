package polyrhythmmania.storymode.gamemode.boss

import com.badlogic.gdx.math.Interpolation
import paintbox.binding.BooleanVar
import paintbox.binding.FloatVar
import paintbox.binding.IntVar
import paintbox.binding.ReadOnlyFloatVar
import paintbox.ui.animation.Animation
import paintbox.ui.animation.AnimationHandler
import paintbox.util.filterAndIsInstance
import polyrhythmmania.engine.ResultFlag
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.engine.modifiers.EngineModifiers
import polyrhythmmania.engine.modifiers.ModifierModule
import polyrhythmmania.world.EntityRodPR


class BossModifierModule(parent: EngineModifiers) : ModifierModule(parent) {

    companion object {

        const val BLOCKS_AHEAD_OF_START_COUNTS_FOR_DAMAGE: Float = 11.2f
        private const val PLAYER_HEALTH: Int = 10
        private const val BOSS_HEALTH: Int = 72
        
        private val DEFAULT_SCORE_OVERRIDE: EngineInputter.ScoreComputationOverride =
            EngineInputter.ScoreComputationOverride(noMiss = true, skillStar = true)
    }

    inner class HealthBar(initialMaxHP: Int) {

        // Settings
        val maxHP: IntVar = IntVar(initialMaxHP)
        val startingHP: IntVar = IntVar { maxHP.use() }

        var visualHPDuration: Float = 0.2f
        var hpDrainDelay: Float = 0.5f
        var hpDrainDuration: Float = 0.5f

        // Data
        val currentHP: IntVar = IntVar(startingHP.get())

        // Readonly
        val hurtFlash: ReadOnlyFloatVar = FloatVar(0f)
        val visualHP: ReadOnlyFloatVar = FloatVar(currentHP.get().toFloat())
        val hpDrainEffect: ReadOnlyFloatVar = FloatVar(visualHP.get())


        fun resetState() {
            visualHP as FloatVar
            hpDrainEffect as FloatVar

            animations.cancelAnimationFor(visualHP)
            animations.cancelAnimationFor(hpDrainEffect)

            currentHP.set(startingHP.get())

            visualHP.set(currentHP.get().toFloat())
            hpDrainEffect.set(visualHP.get())

            (hurtFlash as FloatVar).set(0f)
        }

        fun triggerHurtFlash() {
            (hurtFlash as FloatVar).set(1f)
            
            val interpolation: Interpolation = Interpolation.pow4Out

            animations.enqueueAnimation(
                Animation(
                    interpolation,
                    visualHPDuration,
                    visualHP.get(),
                    currentHP.get().toFloat()
                ),
                visualHP as FloatVar
            )

            val currentHPDrainEffect = hpDrainEffect.get()
            animations.enqueueAnimation(
                Animation(
                    interpolation,
                    hpDrainDuration,
                    currentHPDrainEffect,
                    currentHP.get().toFloat(),
                    delay = hpDrainDelay
                ),
                hpDrainEffect as FloatVar
            )
            hpDrainEffect.set(currentHPDrainEffect) // Has to be set because enqueuing a new animation will complete the previous one, and this animation has a delay
        }

        fun engineUpdate(beat: Float, seconds: Float, deltaSec: Float) {
            val currentHurtFlash = hurtFlash.get()
            if (currentHurtFlash > 0f) {
                val transitionDuration = 0.333f
                (hurtFlash as FloatVar).set((currentHurtFlash - deltaSec / transitionDuration).coerceIn(0f, 1f))
            }
        }
    }

    private val animations: AnimationHandler = AnimationHandler()

    // Settings

    // Data
    val uiOpacity: FloatVar = FloatVar(0f)
    val invertUIOpacity: BooleanVar = BooleanVar(false)
    val effectiveUIOpacity: ReadOnlyFloatVar = FloatVar { if (invertUIOpacity.use()) (1f - uiOpacity.use()) else uiOpacity.use() }
    val missed: BooleanVar = BooleanVar(false)

    // Health
    val playerHealth: HealthBar = HealthBar(PLAYER_HEALTH)
    val bossHealth: HealthBar = HealthBar(BOSS_HEALTH)

    private var scoreOverride: EngineInputter.ScoreComputationOverride = DEFAULT_SCORE_OVERRIDE
    
    override fun resetState() {
        playerHealth.resetState()
        bossHealth.resetState()
        uiOpacity.set(0f)
        invertUIOpacity.set(false)
        missed.set(false)
        scoreOverride = DEFAULT_SCORE_OVERRIDE
        updateInputterScoreOverride()
    }

    fun triggerUIShow() {
        if (uiOpacity.get() <= 0f) {
            invertUIOpacity.set(false)
            uiOpacity.set(0.001f)
        }
    }

    fun triggerUIHide() {
        if (uiOpacity.get() >= 1) {
            invertUIOpacity.set(true)
            uiOpacity.set(0.001f)
        }
    }
    
    fun miss() {
        if (bossHealth.currentHP.get() <= 0 || missed.get()) return
        
        missed.set(true)
        this.scoreOverride = this.scoreOverride.copy(noMiss = false)
        updateInputterScoreOverride()
    }
    
    private fun updateInputterScoreOverride() {
        parent.engine.inputter.scoreComputationOverride = this.scoreOverride
    }

    override fun engineUpdate(beat: Float, seconds: Float, deltaSec: Float) {
        animations.frameUpdate(deltaSec)

        val currentOpacity = uiOpacity.get()
        if (currentOpacity > 0f && currentOpacity < 1f) {
            val transitionDuration = 0.5f
            uiOpacity.set((currentOpacity + deltaSec / transitionDuration).coerceAtMost(1f))
        }

        playerHealth.engineUpdate(beat, seconds, deltaSec)
        bossHealth.engineUpdate(beat, seconds, deltaSec)
    }

    fun checkForRodsThatCollidedWithBoss() {
        val blocksAheadOfStart = BLOCKS_AHEAD_OF_START_COUNTS_FOR_DAMAGE
        val rods = engine.world.entities.filterAndIsInstance<EntityRodPRStoryBoss> { rod ->
            !rod.exploded && !rod.registeredMiss && rod.position.x > (rod.row.startX + blocksAheadOfStart) && rod.didLastBounce
        }
        rods.forEach { rod ->
            rod.explode(engine, shouldCountAsMiss = false)
        }
    }

    override fun onRodPRExploded(rod: EntityRodPR, inputter: EngineInputter, countedAsMiss: Boolean) {
        if (rod !is EntityRodPRStoryBoss) return

        val playerHP = playerHealth.currentHP
        if (playerHP.get() <= 0) return

        val dmgTaken = rod.playerDamageTaken
        if (rod.position.x >= BLOCKS_AHEAD_OF_START_COUNTS_FOR_DAMAGE && rod.didLastBounce) {
            triggerBossHPDown(rod)
        } else {
            if (!dmgTaken.damageTaken && countedAsMiss) {
                dmgTaken.markDamageTaken()
                triggerPlayerHPDown(inputter)
            }
        }
    }

    private fun triggerPlayerHPDown(inputter: EngineInputter) {
        val playerHP = playerHealth.currentHP
        if (playerHP.get() <= 0) return

        playerHP.decrementAndGet()
        playerHealth.triggerHurtFlash()

        this.scoreOverride = this.scoreOverride.copy(skillStar = false)
        updateInputterScoreOverride()

        val currentBossHP = bossHealth.currentHP.get()
        if (playerHP.get() == 0 && currentBossHP > 0) {
            onGameOver(inputter)
        }
    }

    private fun triggerBossHPDown(rod: EntityRodPRStoryBoss) {
        val currentBossHP = bossHealth.currentHP.get()
        if (currentBossHP > 0) {
            val baseDamage = 1
            bossHealth.currentHP.set((currentBossHP - (baseDamage * rod.bossDamageMultiplier)).coerceAtLeast(0))
            bossHealth.triggerHurtFlash()
        }
    }

    private fun onGameOver(inputter: EngineInputter) {
        val engine = inputter.engine

        engine.playbackSpeed = 1f
        engine.resultFlag.set(ResultFlag.Fail.LostToBoss)
    }
}
