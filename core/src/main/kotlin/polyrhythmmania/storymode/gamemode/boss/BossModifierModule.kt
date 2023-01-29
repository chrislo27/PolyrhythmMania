package polyrhythmmania.storymode.gamemode.boss

import paintbox.binding.FloatVar
import paintbox.binding.IntVar
import paintbox.binding.ReadOnlyFloatVar
import paintbox.util.filterAndIsInstance
import polyrhythmmania.engine.ResultFlag
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.engine.modifiers.EngineModifiers
import polyrhythmmania.engine.modifiers.ModifierModule
import polyrhythmmania.world.EntityRodPR


class BossModifierModule(parent: EngineModifiers, val gamemode: StoryBossGameMode) : ModifierModule(parent) {

    companion object {
        const val BLOCKS_AHEAD_OF_START_COUNTS_FOR_DAMAGE: Float = 11.125f
        private const val PLAYER_HEALTH: Int = 10
        private const val BOSS_HEALTH: Int = 50
    }
    
    class HealthBar(initialMaxHP: Int) {
        // Settings
        val maxHP: IntVar = IntVar(initialMaxHP)
        val startingHP: IntVar = IntVar { maxHP.use() }
        
        // Data
        val currentHP: IntVar = IntVar(startingHP.get())
        val hurtFlash: FloatVar = FloatVar(0f)
        
        // Readonly
        val hpPercentage: ReadOnlyFloatVar = FloatVar { currentHP.use().toFloat() / (maxHP.use()).coerceAtLeast(1) }
        
        fun resetState() {
            currentHP.set(startingHP.get())
            hurtFlash.set(0f)
        }
        
        fun triggerHurtFlash() {
            hurtFlash.set(1f)
        }

        fun engineUpdate(beat: Float, seconds: Float, deltaSec: Float) {
            val currentHurtFlash = hurtFlash.get()
            if (currentHurtFlash > 0f) {
                val transitionDuration = 0.333f
                hurtFlash.set((currentHurtFlash - deltaSec / transitionDuration).coerceIn(0f, 1f))
            }
        }
    }
    
    // Settings
    
    // Data
    val uiOpacity: FloatVar = FloatVar(0f)
    
    // Health
    val playerHealth: HealthBar = HealthBar(PLAYER_HEALTH)
    val bossHealth: HealthBar = HealthBar(BOSS_HEALTH)

    override fun resetState() {
        playerHealth.resetState()
        bossHealth.resetState()
        uiOpacity.set(0f)
    }
    
    fun triggerUIShow() {
        if (uiOpacity.get() <= 0f) {
            uiOpacity.set(0.001f)
        }
    }

    override fun engineUpdate(beat: Float, seconds: Float, deltaSec: Float) {
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
        val rods = gamemode.world.entities.filterAndIsInstance<EntityRodPRStoryBoss> { rod ->
            !rod.exploded && rod.position.x > (rod.row.startX + blocksAheadOfStart) && rod.didLastBounce
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
        if (rod.position.x < BLOCKS_AHEAD_OF_START_COUNTS_FOR_DAMAGE) {
            if (!dmgTaken.damageTaken && countedAsMiss) {
                dmgTaken.markDamageTaken()
                triggerPlayerHPDown(inputter)
            }
        } else {
            triggerBossHPDown(rod)
        }
    }
    
    private fun triggerPlayerHPDown(inputter: EngineInputter) {
        val playerHP = playerHealth.currentHP
        if (playerHP.get() <= 0) return
        
        playerHP.decrementAndGet()
        playerHealth.triggerHurtFlash()
        if (playerHP.get() == 0) {
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
        engine.resultFlag.set(ResultFlag.Fail.Generic)
    }
}
