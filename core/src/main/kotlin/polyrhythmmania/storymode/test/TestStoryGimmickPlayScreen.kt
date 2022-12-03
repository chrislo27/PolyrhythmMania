package polyrhythmmania.storymode.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import paintbox.binding.ReadOnlyVar
import paintbox.transition.*
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.InputCalibration
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.screen.play.AbstractEnginePlayScreen
import polyrhythmmania.screen.play.pause.PauseOption
import polyrhythmmania.screen.play.pause.TengokuBgPauseMenuHandler
import polyrhythmmania.screen.play.pause.TengokuPauseBackground
import polyrhythmmania.storymode.gamemode.AbstractStoryGameMode


open class TestStoryGimmickPlayScreen(
        main: PRManiaGame,
        challenges: Challenges, inputCalibration: InputCalibration,
        gameMode: AbstractStoryGameMode
) : AbstractEnginePlayScreen(main, null, gameMode.container, challenges, inputCalibration, gameMode)  {

    override val pauseMenuHandler: TengokuBgPauseMenuHandler = TengokuBgPauseMenuHandler(this)

    private var disableCatchingCursorOnHide: Boolean = false
    
    init {
        pauseMenuHandler.pauseBg.gradientRenderer = TengokuPauseBackground.DebugColorGradientRenderer
    }
    
    init {
        val optionList = mutableListOf<PauseOption>()
        optionList += PauseOption("play.pause.resume", true) {
            unpauseGame(true)
        }
        optionList += PauseOption("play.pause.startOver", true) {
            playMenuSound("sfx_menu_enter_game")

            val thisScreen: TestStoryGimmickPlayScreen = this
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
        optionList += PauseOption(ReadOnlyVar.const("Quit to Debug Menu"), true) {
            playMenuSound("sfx_pause_exit")
            
            quitToDebugMenu()
        }
        this.pauseOptions.set(optionList)
    }
    
    private fun quitToDebugMenu() {
        val currentScreen = main.screen
        Gdx.app.postRunnable {
            val mainMenu = TestStoryGimmickDebugScreen(main)
            main.screen = TransitionScreen(main, currentScreen, mainMenu,
                    FadeToOpaque(0.125f, Color(0f, 0f, 0f, 1f)), FadeToTransparent(0.125f, Color(0f, 0f, 0f, 1f))).apply {
                this.onEntryEnd = {
                    currentScreen.dispose()
                    gameMode?.disposeQuietly()
                }
            }
        }
    }

    override fun onEndSignalFired() {
        super.onEndSignalFired()
        
        quitToDebugMenu()
    }

    override fun uncatchCursorOnHide(): Boolean {
        return super.uncatchCursorOnHide() && !disableCatchingCursorOnHide
    }
}