package polyrhythmmania.sidemodes

import paintbox.binding.IntVar
import paintbox.binding.Var
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.statistics.PlayTimeType


abstract class AbstractEndlessMode(main: PRManiaGame, val prevHighScore: EndlessModeScore, playTimeType: PlayTimeType)
    : SideMode(main, playTimeType) {

    init {
        val renderer = container.renderer
        renderer.showEndlessModeScore.set(true)
        renderer.prevHighScore.set(prevHighScore.highScore.getOrCompute())
        
        engine.inputter.endlessScore.highScore = prevHighScore.highScore
        engine.inputter.endlessScore.showHighScoreAtEnd = prevHighScore.showHighScore
        engine.inputter.endlessScore.showPrevHighScore = prevHighScore.showHighScore
    }
}

data class EndlessModeScore(val highScore: Var<Int> /* Should not be specialized */, val showHighScore: Boolean = true)

class EventIncrementEndlessScore(engine: Engine, val callback: (newScore: Int) -> Unit = {})
    : Event(engine) {
    
    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        val endlessScore = engine.inputter.endlessScore
        if (endlessScore.lives.get() > 0) {
            val scoreVar = endlessScore.score
            val oldScore = scoreVar.get()
            val newScore = oldScore + 1
            scoreVar.set(newScore)
            callback.invoke(newScore)
        }
    }
}