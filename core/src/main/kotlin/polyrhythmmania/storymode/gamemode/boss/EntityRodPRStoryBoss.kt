package polyrhythmmania.storymode.gamemode.boss

import polyrhythmmania.world.EntityRodPR
import polyrhythmmania.world.Row
import polyrhythmmania.world.World

class EntityRodPRStoryBoss(
        world: World, deployBeat: Float, row: Row,
        val playerDamageTaken: PlayerDamageTaken, val bossDamageMultiplier: Int
) : EntityRodPR(world, deployBeat, row, false) {

    class PlayerDamageTaken {
        var damageTaken: Boolean = false
            private set

        fun markDamageTaken() {
            damageTaken = true
        }
    }
}
