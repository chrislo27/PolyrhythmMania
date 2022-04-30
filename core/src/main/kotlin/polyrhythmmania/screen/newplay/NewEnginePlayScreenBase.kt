package polyrhythmmania.screen.newplay

import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.Container
import polyrhythmmania.engine.InputCalibration
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.engine.input.Score
import polyrhythmmania.screen.play.ResultsBehaviour
import polyrhythmmania.sidemodes.SideMode
import polyrhythmmania.statistics.PlayTimeType


class NewEnginePlayScreenBase(
        main: PRManiaGame, playTimeType: PlayTimeType?,
        container: Container,
        challenges: Challenges, inputCalibration: InputCalibration,
        sideMode: SideMode?, resultsBehaviour: ResultsBehaviour
) : NewAbstractEnginePlayScreen(main, playTimeType, container, challenges, inputCalibration, sideMode, resultsBehaviour) {

    companion object; // Used for early init
    
    override fun copyThisScreenForResults(scoreObj: Score, resultsBehaviour: ResultsBehaviour): NewAbstractEnginePlayScreen {
        return NewEnginePlayScreenBase(main, playTimeType, container, challenges, inputCalibration, sideMode,
                if (resultsBehaviour is ResultsBehaviour.ShowResults)
                    resultsBehaviour.copy(previousHighScore = if (scoreObj.newHighScore) 
                        scoreObj.scoreInt 
                    else resultsBehaviour.previousHighScore)
                else resultsBehaviour)
    }
    
}