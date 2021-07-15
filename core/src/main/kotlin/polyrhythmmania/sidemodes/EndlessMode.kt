package polyrhythmmania.sidemodes

import paintbox.binding.Var
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event


abstract class EndlessMode(main: PRManiaGame, val prevHighScore: EndlessModeScore)
    : SideMode(main) {

    init {
        val renderer = container.renderer
        renderer.showEndlessModeScore.set(true)
        renderer.prevHighScore.set(prevHighScore.highScore.getOrCompute())
        
        engine.inputter.endlessScore.highScore = prevHighScore.highScore
    }
}

data class EndlessModeScore(val highScore: Var<Int>)

class EventIncrementEndlessScore(engine: Engine, val callback: (newScore: Int) -> Unit = {})
    : Event(engine) {
    
    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        val scoreVar = engine.inputter.endlessScore.score
        val oldScore = scoreVar.getOrCompute()
        val newScore = oldScore + 1
        scoreVar.set(newScore)
        callback.invoke(newScore)
    }
}