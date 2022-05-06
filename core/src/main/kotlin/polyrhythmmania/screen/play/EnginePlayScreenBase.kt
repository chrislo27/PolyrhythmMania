package polyrhythmmania.screen.play

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Align
import paintbox.binding.FloatVar
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.transition.*
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.animation.Animation
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.fillRect
import paintbox.util.gdxutils.prepareStencilMask
import paintbox.util.gdxutils.useStencilMask
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.achievements.Achievements
import polyrhythmmania.container.Container
import polyrhythmmania.engine.InputCalibration
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.engine.input.Score
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.gamemodes.endlessmode.EndlessPolyrhythm
import polyrhythmmania.screen.play.pause.*
import polyrhythmmania.statistics.PlayTimeType
import polyrhythmmania.world.EndlessType
import polyrhythmmania.world.WorldType


class EnginePlayScreenBase(
        main: PRManiaGame, playTimeType: PlayTimeType?,
        container: Container,
        challenges: Challenges, inputCalibration: InputCalibration,
        gameMode: GameMode?, resultsBehaviour: ResultsBehaviour
) : AbstractEnginePlayScreen(main, playTimeType, container, challenges, inputCalibration, gameMode, resultsBehaviour) {
    
    override val pauseMenuHandler: PauseMenuHandler = TengokuBgPauseMenuHandler(this)
    
    private var endlessPrPauseTime: Float = 0f
    
    private var disableCatchingCursorOnHide: Boolean = false
    
    init {
        if (engine.world.worldMode.endlessType == EndlessType.REGULAR_ENDLESS
                && engine.world.worldMode.type == WorldType.POLYRHYTHM
                && engine.inputter.endlessScore.maxLives.get() == 1) { // Daredevil mode in endless
            // 232CDD
            (pauseMenuHandler as? TengokuBgPauseMenuHandler)?.also { handler ->
                val hex = "DB2323"
                val bg = handler.pauseBg
                bg.cycleSpeed = 0f
                bg.topColor.set(Color.valueOf(hex))
                bg.bottomColor.set(Color.valueOf(hex))
            }
        }
    }
    
    init {
        // Score achievements for endless-type modes
        engine.inputter.endlessScore.score.addListener { scoreVar ->
            if (engine.world.worldMode.endlessType == EndlessType.REGULAR_ENDLESS && engine.areStatisticsEnabled) {
                val newScore = scoreVar.getOrCompute()
                when (engine.world.worldMode.type) {
                    WorldType.POLYRHYTHM -> {
                        if (gameMode is EndlessPolyrhythm) {
                            if (gameMode.dailyChallenge != null) {
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

                                if (gameMode.disableLifeRegen) {
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
    
    init {
        val optionList = mutableListOf<PauseOption>()
        optionList += PauseOption(if (engine.autoInputs) "play.pause.resume.robotMode" else "play.pause.resume", true) {
            unpauseGame(true)
        }
        optionList += PauseOption("play.pause.startOver", !(gameMode is EndlessPolyrhythm && gameMode.dailyChallenge != null)) {
            playMenuSound("sfx_menu_enter_game")

            val thisScreen: EnginePlayScreenBase = this
            val resetAction: () -> Unit = {
                resetAndUnpause()
                disableCatchingCursorOnHide = false
            }
            if (shouldCatchCursor()) {
                disableCatchingCursorOnHide = true
                Gdx.input.isCursorCatched = true
            }
            main.screen = TransitionScreen(main, thisScreen, thisScreen,
                    WipeTransitionHead(Color.BLACK.cpy(), 0.4f), WipeTransitionTail(Color.BLACK.cpy(), 0.4f)).apply {
                onEntryEnd = resetAction
            }
        }
        optionList += PauseOption("play.pause.quitToMainMenu", true) {
            quitToMainMenu()
            Gdx.app.postRunnable {
                playMenuSound("sfx_pause_exit")
            }
        }
        this.pauseOptions.set(optionList)
    }


    override fun uncatchCursorOnHide(): Boolean {
        return super.uncatchCursorOnHide() && !disableCatchingCursorOnHide
    }

    override fun copyThisScreenForResultsStartOver(scoreObj: Score, resultsBehaviour: ResultsBehaviour): EnginePlayScreenBase {
        return EnginePlayScreenBase(main, playTimeType, container, challenges, inputCalibration, gameMode,
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

        if (gameMode != null && gameMode is EndlessPolyrhythm) {
            gameMode.submitPauseTime(this.endlessPrPauseTime)
        }
    }
}