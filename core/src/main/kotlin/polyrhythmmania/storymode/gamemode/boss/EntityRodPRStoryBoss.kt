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

    override fun bounce(startIndex: Int) {
        // Special behaviour: Last piston bounces the rod up to the boss
        if (startIndex in 0 until row.length) {
            val rowBlock = row.rowBlocks[startIndex]
            if (rowBlock.type.isPiston) {
                var isLastPiston = true
                for (i in startIndex + 1 until row.length) {
                    if (row.rowBlocks[i].type.isPiston) {
                        isLastPiston = false
                        break
                    }
                }
                
                if (isLastPiston) {
                    // Note that peakHeight is computed via: row.startY + 1f + (endIndex - startIndex) + manualOffset
                    bounce(startIndex, startIndex + 5, 5f) // TODO
                    return
                }
            }

            val lookahead = getLookaheadIndex(startIndex)
            bounce(startIndex, lookahead)
        }
    }
}
