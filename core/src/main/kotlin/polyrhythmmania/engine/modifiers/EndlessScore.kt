package polyrhythmmania.engine.modifiers

import paintbox.binding.*
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.engine.input.InputResult

class EndlessScore : ModifierModule {
    
    // Settings
    var enabled: Boolean = false
    
    var showNewHighScoreAtEnd: Boolean = true
    var hideHighScoreText: Boolean = false
    val maxLives: IntVar = IntVar(0)
    val startingLives: IntVar = IntVar { maxLives.use() }
    /**
     * Set when the [score] is higher than this [highScore].
     */
    var highScore: Var<Int> = GenericVar(0) // Intentionally a Var<Int> and not a specialized IntVar.
    
    // Data
    val score: IntVar = IntVar(0)
    val lives: IntVar = IntVar(startingLives.get())
    val gameOverSeconds: FloatVar = FloatVar(Float.MAX_VALUE)
    val gameOverUIShown: BooleanVar = BooleanVar(false)
    
    override fun resetState() {
        score.set(0)
        lives.set(startingLives.get())
        gameOverSeconds.set(Float.MAX_VALUE)
        gameOverUIShown.set(false)
    }

    override fun onMissed(inputter: EngineInputter, firstMiss: Boolean) {
        // TODO
    }

    override fun onInputResultHit(inputter: EngineInputter, result: InputResult, countsAsMiss: Boolean) {
        // TODO
    }
}