package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItemState
import polyrhythmmania.storymode.screen.ExitCallback
import polyrhythmmania.storymode.screen.StoryLoadingScreen
import polyrhythmmania.storymode.screen.StoryPlayScreen


interface DesktopController {

    enum class SFXType {
        ENTER_LEVEL, CLICK_INBOX_ITEM, INBOX_ITEM_UNLOCKED, 
    }
    
    
    fun playLevel(contract: Contract, inboxItem: InboxItem?, inboxItemState: InboxItemState?,
                  exitCallback: ExitCallback)
    
    fun playSFX(sfx: SFXType)

}

object NoOpDesktopController : DesktopController {
    override fun playLevel(contract: Contract, inboxItem: InboxItem?, inboxItemState: InboxItemState?,
                           exitCallback: ExitCallback) {
    }

    override fun playSFX(sfx: DesktopController.SFXType) {
    }
}

class DebugDesktopController : DesktopController {
    lateinit var desktopUI: DesktopUI

    override fun playLevel(contract: Contract, inboxItem: InboxItem?, inboxItemState: InboxItemState?,
                           exitCallback: ExitCallback) {
        val main = desktopUI.main
        val loadingScreen = StoryLoadingScreen<StoryPlayScreen>(main, {
            val gameMode = contract.gamemodeFactory(main)
            val playScreen = StoryPlayScreen(main, gameMode.container, Challenges.NO_CHANGES,
                    main.settings.inputCalibration.getOrCompute(), gameMode, contract, desktopUI.rootScreen, exitCallback)

            gameMode.prepareFirstTime()
            playScreen.resetAndUnpause(unpause = false)

            StoryLoadingScreen.LoadResult(playScreen)
        }) { playScreen ->
            playScreen.unpauseGameNoSound()
            playScreen.initializeIntroCard()
            main.screen = TransitionScreen(main, main.screen, playScreen,
                    FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.125f, Color.BLACK))
        }.apply {
            this.minimumShowTime = 0f
            this.minWaitTimeBeforeLoadStart = 0.25f
            this.minWaitTimeAfterLoadFinish = 0f
        }

        main.screen = TransitionScreen(main, main.screen, loadingScreen,
                FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.125f, Color.BLACK))
    }

    override fun playSFX(sfx: DesktopController.SFXType) {
        val main = desktopUI.main
        val sound: Sound = when (sfx) {
            DesktopController.SFXType.ENTER_LEVEL -> AssetRegistry["sfx_menu_enter_game"]
            DesktopController.SFXType.CLICK_INBOX_ITEM -> AssetRegistry["sfx_menu_blip"]
            DesktopController.SFXType.INBOX_ITEM_UNLOCKED -> StoryAssets["sfx_desk_unlocked"]
        } ?: return
        
        main.playMenuSfx(sound)
    }
}
