package polyrhythmmania.storymode.gamemode.boss

import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.world.Row


class EventDeployRodBoss(
        engine: Engine,
        val row: Row, startBeat: Float,
        val damageTakenVar: EntityRodPRStoryBoss.PlayerDamageTaken, val bossDamageMultiplier: Int
) : Event(engine) {

    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        val rod = EntityRodPRStoryBoss(engine.world, this.beat, row, damageTakenVar, bossDamageMultiplier)
        engine.world.addEntity(rod)

        if (engine.areStatisticsEnabled) {
            GlobalStats.rodsDeployed.increment()
            GlobalStats.rodsDeployedPolyrhythm.increment()
        }
    }
}
