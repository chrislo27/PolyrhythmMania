package polyrhythmmania.gamemodes.endlessmode

import polyrhythmmania.world.EntityRodPR
import polyrhythmmania.world.Row
import polyrhythmmania.world.World


class EntityRodPREndless(world: World, deployBeat: Float, row: Row, val lifeLost: LifeLost) 
    : EntityRodPR(world, deployBeat, row, false) {

    class LifeLost {
        var lifeLost: Boolean = false
            private set

        fun markLifeLost() {
            lifeLost = true
        }
    }
}
