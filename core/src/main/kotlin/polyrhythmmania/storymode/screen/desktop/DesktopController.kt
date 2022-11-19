package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.graphics.Color
import paintbox.Paintbox
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItemCompletion
import polyrhythmmania.storymode.inbox.InboxItemState
import polyrhythmmania.storymode.inbox.StageCompletionData
import polyrhythmmania.storymode.screen.ExitCallback
import polyrhythmmania.storymode.screen.ExitReason
import polyrhythmmania.storymode.screen.StoryLoadingScreen
import polyrhythmmania.storymode.screen.StoryPlayScreen
import java.time.LocalDateTime
import java.time.ZoneOffset


interface DesktopController {

    fun playLevel(contract: Contract, inboxItem: InboxItem?, inboxItemState: InboxItemState?,
                  exitCallback: ExitCallback?)

}

object NoOpDesktopController : DesktopController {
    override fun playLevel(contract: Contract, inboxItem: InboxItem?, inboxItemState: InboxItemState?,
                           exitCallback: ExitCallback?) {
    }
}

class DebugDesktopController : DesktopController {
    lateinit var desktopUI: DesktopUI

    override fun playLevel(contract: Contract, inboxItem: InboxItem?, inboxItemState: InboxItemState?,
                           exitCallback: ExitCallback?) {
        val main = desktopUI.main
        val inboxState = desktopUI.scenario.inboxState
        val combinedExitReason = ExitCallback { exitReason ->
            Paintbox.LOGGER.debug("ExitReason: $exitReason")
            if (inboxItem != null && inboxItemState != null) {
                var newState = inboxItemState
                newState = newState.copy(playedBefore = true)

                when (exitReason) {
                    is ExitReason.Passed -> {
                        val now = LocalDateTime.now(ZoneOffset.UTC)
                        if (newState.completion != InboxItemCompletion.COMPLETED) {
                            newState = newState.copy(completion = InboxItemCompletion.COMPLETED,
                                    stageCompletionData = StageCompletionData(now, now, exitReason.score,
                                            exitReason.skillStar ?: false, exitReason.noMiss))
                        } else {
                            val oldCompletion = newState.stageCompletionData
                            if (oldCompletion != null) {
                                if (exitReason.isBetterThan(ExitReason.Passed(oldCompletion.score, oldCompletion.skillStar, oldCompletion.noMiss))) {
                                    val scd = StageCompletionData(oldCompletion.firstClearTime, now,
                                            exitReason.score, exitReason.skillStar ?: false, exitReason.noMiss)
                                    newState = newState.copy(stageCompletionData = scd)
                                }
                            }
                        }
                    }
                    ExitReason.Skipped -> {
                        if (!newState.completion.shouldCountAsCompleted()) {
                            newState = newState.copy(completion = InboxItemCompletion.SKIPPED)
                        }
                    }
                    ExitReason.Quit -> {}
                }

                inboxState.putItemState(inboxItem, newState)
            }

            exitCallback?.onExit(exitReason)
        }
        val loadingScreen = StoryLoadingScreen<StoryPlayScreen>(main, {
            val gameMode = contract.gamemodeFactory(main)
            val playScreen = StoryPlayScreen(main, gameMode.container, Challenges.NO_CHANGES,
                    main.settings.inputCalibration.getOrCompute(), gameMode, contract, desktopUI.rootScreen, combinedExitReason)

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
}
