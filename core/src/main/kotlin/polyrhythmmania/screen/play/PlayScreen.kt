package polyrhythmmania.screen.play

import com.badlogic.gdx.Gdx
import polyrhythmmania.PRManiaGame
import polyrhythmmania.achievements.Achievements
import polyrhythmmania.container.Container
import polyrhythmmania.engine.InputCalibration
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.engine.input.Score
import polyrhythmmania.sidemodes.SideMode
import polyrhythmmania.sidemodes.endlessmode.EndlessPolyrhythm
import polyrhythmmania.statistics.PlayTimeType
import polyrhythmmania.world.EndlessType
import polyrhythmmania.world.WorldType


class PlayScreen private constructor(
        main: PRManiaGame, sideMode: SideMode?, playTimeType: PlayTimeType,
        container: Container, challenges: Challenges, inputCalibration: InputCalibration,
        resultsBehaviour: ResultsBehaviour
) : AbstractPlayScreen(main, sideMode, playTimeType, container, challenges, inputCalibration, resultsBehaviour) {

    companion object; // Used for early init

    private var pauseTime: Float = 0f
    
    constructor(
            main: PRManiaGame, sideMode: SideMode,
            challenges: Challenges,
            inputCalibration: InputCalibration,
            resultsBehaviour: ResultsBehaviour
    ) : this(main, sideMode, sideMode.playTimeType, sideMode.container, challenges, inputCalibration, resultsBehaviour)

    constructor(
            main: PRManiaGame, playTimeType: PlayTimeType,
            container: Container, challenges: Challenges,
            inputCalibration: InputCalibration,
            resultsBehaviour: ResultsBehaviour
    ) : this(main, null, playTimeType, container, challenges, inputCalibration, resultsBehaviour)

    init {
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
                                    if (engine.inputter.endlessScore.maxLives.get() == 1) { // Daredevil
                                        Achievements.attemptAwardScoreAchievement(Achievements.endlessDaredevil100, newScore)
                                    }
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

    override fun render(delta: Float) {
        super.render(delta)
        
        if (isPaused.get()) {
            pauseTime += Gdx.graphics.deltaTime
        }
    }

    override fun copyScreenForResults(scoreObj: Score, resultsBehaviour: ResultsBehaviour): PlayScreen {
        return PlayScreen(
                main, sideMode, playTimeType, container, challenges, inputCalibration,
                if (resultsBehaviour is ResultsBehaviour.ShowResults) resultsBehaviour.copy(
                        previousHighScore = if (scoreObj.newHighScore) scoreObj.scoreInt else resultsBehaviour.previousHighScore
                ) else resultsBehaviour
        )
    }

    override fun pauseGame(playSound: Boolean) {
        super.pauseGame(playSound)

        pauseTime = 0f
    }

    override fun unpauseGame(playSound: Boolean) {
        super.unpauseGame(playSound)

        if (sideMode != null && sideMode is EndlessPolyrhythm) {
            sideMode.submitPauseTime(this.pauseTime)
        }
    }
}