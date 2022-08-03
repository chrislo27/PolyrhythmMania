package polyrhythmmania.engine.modifiers

import paintbox.binding.FloatVar
import paintbox.binding.IntVar
import paintbox.registry.AssetRegistry
import polyrhythmmania.engine.ResultFlag
import polyrhythmmania.engine.SoundInterface
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.soundsystem.BeadsSound

/**
 * Lives are separate from [EndlessScore] lives, and only flag the engine result to be failed.
 * 
 * A life is lost when a miss occurs. There is a cooldown before more lives can be lost.
 * 
 * Not compatible with [EndlessScore] mode.
 */
class LivesMode(parent: EngineModifiers) : ModifierModule(parent) {
    
    companion object {
        const val DEFAULT_COOLDOWN: Float = 1.75f
    }

    // Settings
    val maxLives: IntVar = IntVar(3)
    val cooldownBaseAmount: FloatVar = FloatVar(DEFAULT_COOLDOWN)


    // Data
    val lives: IntVar = IntVar(maxLives.get())
    val currentCooldown: FloatVar = FloatVar(0f)

    override fun resetState() {
        lives.set(maxLives.get())
        currentCooldown.set(0f)
    }

    override fun engineUpdate(beat: Float, seconds: Float, deltaSec: Float) {
        if (currentCooldown.get() > 0f && deltaSec > 0f) {
            currentCooldown.set((currentCooldown.get() - deltaSec).coerceAtLeast(0f))
        }
    }
    
    fun loseALife(inputter: EngineInputter) {
        if (!this.enabled.get()) {
            return
        }
        if (currentCooldown.get() > 0 || lives.get() <= 0 || maxLives.get() <= 0) {
            return
        }

        val oldLives = this.lives.get()
        val newLives = (oldLives - 1).coerceIn(0, this.maxLives.get())
        this.lives.set(newLives)
        this.currentCooldown.set(cooldownBaseAmount.get())
        
        inputter.engine.soundInterface.playAudio(AssetRegistry.get<BeadsSound>("sfx_perfect_fail"), SoundInterface.SFXType.NORMAL) { player ->
            player.gain = 0.4f
        }

        if (oldLives > 0 && newLives == 0) {
            onAllLivesLost(inputter)
        }
    }

    private fun onAllLivesLost(inputter: EngineInputter) {
        val engine = inputter.engine
        
        engine.resultFlag.set(ResultFlag.FAIL)
    }


    // InputterListener overrides

    override fun onMissed(inputter: EngineInputter, firstMiss: Boolean) {
        loseALife(inputter)
    }
}
