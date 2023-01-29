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
                didLastBounce = true

                // Note that peakHeight is computed via: row.startY + 1f + (endIndex - startIndex) + manualOffset
                bounce(startIndex, 11, MathUtils.random(-0.5f, 0.5f))
                return
            }

            val lookahead = getLookaheadIndex(startIndex)
            bounce(startIndex, lookahead)
        }
    }
}
