package polyrhythmmania.storymode.gamemode.boss

import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.world.Row


class EventDeployRodBoss(
    engine: Engine,
    val row: Row, startBeat: Float,
    val xUnitsPerBeat: Float, val lastPistonIndex: Int,
    val damageTakenVar: EntityRodPRStoryBoss.PlayerDamageTaken, val bossDamageMultiplier: Int,
    val onMissCallback: (() -> Unit)?,
) : Event(engine) {

    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)

        val rod = EntityRodPRStoryBoss(
            engine.world, this.beat, row, lastPistonIndex,
            damageTakenVar, bossDamageMultiplier,
            onMissCallback
        )
        rod.xUnitsPerBeat = this.xUnitsPerBeat

        engine.world.addEntity(rod)

        if (engine.areStatisticsEnabled) {
            GlobalStats.rodsDeployed.increment()
            GlobalStats.rodsDeployedPolyrhythm.increment()
        }
    }
}
