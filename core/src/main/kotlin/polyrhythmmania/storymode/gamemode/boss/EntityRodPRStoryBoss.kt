package polyrhythmmania.storymode.gamemode.boss

import polyrhythmmania.world.EntityRodPR
import polyrhythmmania.world.Row
import polyrhythmmania.world.World

class EntityRodPRStoryBoss(
    world: World, deployBeat: Float, row: Row,
    val lastPistonIndex: Int,
    val playerDamageTaken: PlayerDamageTaken, val bossDamageMultiplier: Int,
) : EntityRodPR(world, deployBeat, row, isDefective = true) {

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
            val rowBlocks = row.rowBlocks
            val rowBlock = rowBlocks[startIndex]
            if (rowBlock.type.isPiston && startIndex == lastPistonIndex) {
                // Note that peakHeight is computed via: row.startY + 1f + (endIndex - startIndex) + manualOffset
                bounce(startIndex, 11, 1f)
                return
            }

            val lookahead = getLookaheadIndex(startIndex)
            bounce(startIndex, lookahead)
        }
    }
}
