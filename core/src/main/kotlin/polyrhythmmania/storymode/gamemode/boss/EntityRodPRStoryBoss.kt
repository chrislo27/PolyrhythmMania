package polyrhythmmania.storymode.gamemode.boss

import com.badlogic.gdx.math.MathUtils
import polyrhythmmania.world.EntityRodPR
import polyrhythmmania.world.Row
import polyrhythmmania.world.World

class EntityRodPRStoryBoss(
    world: World, deployBeat: Float, row: Row,
    val lastPistonIndex: Int,
    val playerDamageTaken: PlayerDamageTaken, val bossDamageMultiplier: Int,
) : EntityRodPR(world, deployBeat, row, isDefective = true) {
    
    companion object {
        private const val INDEX_OF_BOSS: Int = 11
    }

    class PlayerDamageTaken {

        var damageTaken: Boolean = false
            private set

        fun markDamageTaken() {
            damageTaken = true
        }
    }

    override val killAfterBeats: Float
        get() = 4f + row.length / xUnitsPerBeat + 5 // 4 prior to first index 0 + rowLength/xUnitsPerBeat + 5 buffer

    var didLastBounce: Boolean = false
        private set
    
    init {
        this.defectiveRodEscaped = true // Set to true so it doesn't count as escaped -- should be ignored
    }

    override fun bounce(startIndex: Int) {
        // Special behaviour: Last piston bounces the rod up to the boss
        if (startIndex in 0 until row.length) {
            val rowBlocks = row.rowBlocks
            val rowBlock = rowBlocks[startIndex]
            if (rowBlock.type.isPiston && startIndex == lastPistonIndex) {
                bounceToBoss(startIndex)
                return
            }

            val lookahead = getLookaheadIndex(startIndex)
            if (lastPistonIndex == -1 && lookahead > INDEX_OF_BOSS) {
                // Happens in cases where the patterns are disjoint, so information is lost about the other side.
                // We assume if the lookahead is ahead of the boss that it should hit the boss in this case
                bounceToBoss(startIndex)
            } else {
                bounce(startIndex, lookahead)
            }
        }
    }
    
    private fun bounceToBoss(startIndex: Int) {
        didLastBounce = true
        // Note that peakHeight is computed via: row.startY + 1f + (endIndex - startIndex) + manualOffset
        bounce(startIndex, INDEX_OF_BOSS, MathUtils.random(-0.5f, 0.5f))
    }
}
