package polyrhythmmania.engine.modifiers

import paintbox.registry.AssetRegistry
import polyrhythmmania.engine.SoundInterface
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.engine.input.InputResult
import polyrhythmmania.engine.input.InputScore
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.statistics.GlobalStats

class PerfectChallengeData : ModifierModule() {
    
    // Data
    var hit: Float = 0f
    var failed: Boolean = false
    
    
    override fun resetState() {
        hit = 0f
        failed = false
    }


    override fun onMissed(inputter: EngineInputter, firstMiss: Boolean) {
        if (this.enabled.get() && !this.failed) {
            this.failed = true
            this.hit = 1f

            val engine = inputter.engine
            engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_perfect_fail"), SoundInterface.SFXType.NORMAL) { player ->
                player.gain = 0.45f
            }
            if (engine.areStatisticsEnabled) {
                GlobalStats.perfectsLost.increment()
            }
        }
    }

    override fun onInputResultHit(inputter: EngineInputter, result: InputResult, countsAsMiss: Boolean) {
        if (this.enabled.get() && !this.failed && !countsAsMiss) {
            this.hit = 1f
        }
    }
}
