package polyrhythmmania.gamemodes

import paintbox.binding.Var
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.statistics.PlayTimeType


abstract class AbstractEndlessMode(main: PRManiaGame, val prevHighScore: EndlessModeScore, playTimeType: PlayTimeType)
    : GameMode(main, playTimeType) {

    init {
        val renderer = container.renderer
        renderer.endlessModeRendering.prevHighScore.set(prevHighScore.highScore.getOrCompute())

        val endlessScore = engine.modifiers.endlessScore
        endlessScore.highScore = prevHighScore.highScore
        endlessScore.showNewHighScoreAtEnd = prevHighScore.showNewHighScoreAtEnd
        endlessScore.hideHighScoreText = prevHighScore.hideHighScoreText
    }
}

data class EndlessModeScore(
        /** Should not be specialized */ val highScore: Var<Int>, 
        val showNewHighScoreAtEnd: Boolean = true,
        val hideHighScoreText: Boolean = false,
)

class EventIncrementEndlessScore(engine: Engine, val callback: (newScore: Int) -> Unit = {})
    : Event(engine) {
    
    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        val endlessScore = engine.modifiers.endlessScore
        if (endlessScore.lives.get() > 0) {
            val scoreVar = endlessScore.score
            val oldScore = scoreVar.get()
            val newScore = oldScore + 1
            scoreVar.set(newScore)
            callback.invoke(newScore)
        }
    }
}