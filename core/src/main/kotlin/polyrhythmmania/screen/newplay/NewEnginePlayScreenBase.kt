package polyrhythmmania.screen.newplay

import com.badlogic.gdx.Gdx
import polyrhythmmania.PRManiaGame
import polyrhythmmania.achievements.Achievements
import polyrhythmmania.container.Container
import polyrhythmmania.engine.InputCalibration
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.engine.input.Score
import polyrhythmmania.screen.play.ResultsBehaviour
import polyrhythmmania.sidemodes.SideMode
import polyrhythmmania.sidemodes.endlessmode.EndlessPolyrhythm
import polyrhythmmania.statistics.PlayTimeType
import polyrhythmmania.world.EndlessType
import polyrhythmmania.world.WorldType


class NewEnginePlayScreenBase(
        main: PRManiaGame, playTimeType: PlayTimeType?,
        container: Container,
        challenges: Challenges, inputCalibration: InputCalibration,
        sideMode: SideMode?, resultsBehaviour: ResultsBehaviour
) : NewAbstractEnginePlayScreen(main, playTimeType, container, challenges, inputCalibration, sideMode, resultsBehaviour) {

    companion object; // Used for early init

    private var endlessPrPauseTime: Float = 0f
    
    init {
        // TODO move me
        // Score achievements for endless-type modes
        engine.inputter.endlessScore.score.addListener { scoreVar ->
            if (engine.world.worldMode.endlessType == EndlessType.REGULAR_ENDLESS && engine.areStatisticsEnabled) {
                val newScore = scoreVar.getOrCompute()
                when (engine.world.worldMode.type) {
                    WorldType.POLYRHYTHM -> {
                        if (sideMode is EndlessPolyrhythm) {
                            if (sideMode.dailyChallenge != null) {
                                listOf(Achievements.dailyScore25, Achievements.dailyScore50,
                                        Achievements.dailyScore75, Achievements.dailyScore100,
                                        Achievements.dailyScore125).forEach {
                                    Achievements.attemptAwardScoreAchievement(it, newScore)
                                }
                            } else {
                                listOf(Achievements.endlessScore25, Achievements.endlessScore50,
                                        Achievements.endlessScore75, Achievements.endlessScore100,
                                        Achievements.endlessScore125).forEach {
                                    Achievements.attemptAwardScoreAchievement(it, newScore)
                                }

                                if (sideMode.disableLifeRegen) {
                                    Achievements.attemptAwardScoreAchievement(Achievements.endlessNoLifeRegen100, newScore)
                                }
                                if (engine.inputter.endlessScore.maxLives.get() == 1) { // Daredevil
                                    Achievements.attemptAwardScoreAchievement(Achievements.endlessDaredevil100, newScore)
                                }
                                if (main.settings.masterVolumeSetting.getOrCompute() == 0) {
                                    Achievements.attemptAwardScoreAchievement(Achievements.endlessSilent50, newScore)
                                }
                            }
                        }

                    }
                    WorldType.DUNK -> {
                        listOf(Achievements.dunkScore10, Achievements.dunkScore20, Achievements.dunkScore30,
                                Achievements.dunkScore50).forEach {
                            Achievements.attemptAwardScoreAchievement(it, newScore)
                        }
                    }
                    WorldType.ASSEMBLE -> {
                        // NO-OP
                    }
                }
            }
        }
    }
    
    override fun copyThisScreenForResults(scoreObj: Score, resultsBehaviour: ResultsBehaviour): NewAbstractEnginePlayScreen {
        return NewEnginePlayScreenBase(main, playTimeType, container, challenges, inputCalibration, sideMode,
                if (resultsBehaviour is ResultsBehaviour.ShowResults)
                    resultsBehaviour.copy(previousHighScore = if (scoreObj.newHighScore) 
                        scoreObj.scoreInt 
                    else resultsBehaviour.previousHighScore)
                else resultsBehaviour)
    }

    override fun renderGameplay(delta: Float) {
        super.renderGameplay(delta)

        if (isPaused.get()) {
            endlessPrPauseTime += Gdx.graphics.deltaTime
        }
    }

    override fun pauseGame(playSound: Boolean) {
        super.pauseGame(playSound)

        endlessPrPauseTime = 0f
    }

    override fun unpauseGame(playSound: Boolean) {
        super.unpauseGame(playSound)

        if (sideMode != null && sideMode is EndlessPolyrhythm) {
            sideMode.submitPauseTime(this.endlessPrPauseTime)
        }
    }
}