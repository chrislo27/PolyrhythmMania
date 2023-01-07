package polyrhythmmania.storymode.gamemode.boss

import paintbox.binding.FloatVar
import paintbox.binding.IntVar
import paintbox.binding.ReadOnlyFloatVar
import paintbox.util.filterAndIsInstance
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
        
        // Readonly
        val hpPercentage: ReadOnlyFloatVar = FloatVar { currentHP.use().toFloat() / (maxHP.use()).coerceAtLeast(1) }
        
        fun resetState() {
            currentHP.set(startingHP.get())
        }
    }
    
    // Settings
    
    // Data
    
    // Health
    val playerHealth: HealthBar = HealthBar(PLAYER_HEALTH)
    val bossHealth: HealthBar = HealthBar(BOSS_HEALTH)

    override fun resetState() {
        playerHealth.resetState()
        bossHealth.resetState()
    }

    override fun engineUpdate(beat: Float, seconds: Float, deltaSec: Float) {
    }

    fun checkForRodsThatCollidedWithBoss() {
        val blocksAheadOfStart = BLOCKS_AHEAD_OF_START_COUNTS_FOR_DAMAGE
        val rods = gamemode.world.entities.filterAndIsInstance<EntityRodPR> { rod ->
            !rod.exploded && rod.position.x > (rod.row.startX + blocksAheadOfStart)
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
                playerHP.decrementAndGet()
            }
        } else {
            val currentBossHP = bossHealth.currentHP.get()
            if (currentBossHP > 0) {
                val baseDamage = 1
                bossHealth.currentHP.set((currentBossHP - (baseDamage * rod.bossDamageMultiplier)).coerceAtLeast(0))
            }
        }
    }
}
