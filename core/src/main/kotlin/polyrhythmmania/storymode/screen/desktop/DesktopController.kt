package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.graphics.Color
import paintbox.Paintbox
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.screen.StoryPlayScreen


interface DesktopController {
    
    fun playLevel(contract: Contract)
    
}

object NoOpDesktopController : DesktopController {
    override fun playLevel(contract: Contract) {
    }
}

class DebugDesktopController : DesktopController {
    lateinit var desktopUI: DesktopUI
    
    override fun playLevel(contract: Contract) {
        val main = desktopUI.main
        val gameMode = contract.gamemodeFactory(main)
        val playScreen = StoryPlayScreen(main, gameMode.container, Challenges.NO_CHANGES,
                main.settings.inputCalibration.getOrCompute(), gameMode, contract, desktopUI.rootScreen) {
            Paintbox.LOGGER.debug("ExitReason: $it")
        }
        main.screen = TransitionScreen(main, main.screen, playScreen,
                FadeToOpaque(0.25f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK)).apply {
            this.onEntryEnd = {
                gameMode.prepareFirstTime()
                playScreen.resetAndUnpause()
                playScreen.initializeIntroCard()
            }
        }
    }
}
