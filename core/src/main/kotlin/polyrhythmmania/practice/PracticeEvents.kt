package polyrhythmmania.practice

import paintbox.registry.AssetRegistry
import polyrhythmmania.engine.Engine
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.world.EntityRowBlock
import polyrhythmmania.world.EventRowBlock
import polyrhythmmania.world.EventRowBlockRetract
import polyrhythmmania.world.Row


/**
 * Same as [EventRowBlockRetract] but will NOT fire if the "moreTimes" counter is already zero.
 */
class EventPracticeRetract(engine: Engine, row: Row, index: Int, startBeat: Float,
                           affectThisIndexAndForward: Boolean = false)
    : EventRowBlock(engine, row, index, startBeat, affectThisIndexAndForward) {

    /**
     * Holds the result of [EntityRowBlock.retract] from [entityOnUpdate] for checking if the sound should play.
     */
    private var anyBlocksAffected: Boolean = false
    

    override fun onStart(currentBeat: Float) {
        this.anyBlocksAffected = false
        val practice = engine.inputter.practice
        if (practice.practiceModeEnabled && practice.moreTimes <= 0) return
        
        super.onStart(currentBeat)

        if (anyBlocksAffected && currentBeat < this.beat + 0.125f) {
            engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_retract"))
        }
    }

    override fun entityOnStart(entity: EntityRowBlock, currentBeat: Float) {
        val result = entity.retract()
        if (result) {
            anyBlocksAffected = true
        }
    }
}