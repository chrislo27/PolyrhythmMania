package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.audio.Sound
import paintbox.Paintbox
import paintbox.registry.AssetRegistry
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItemCompletion
import polyrhythmmania.storymode.inbox.InboxItemState
import polyrhythmmania.storymode.inbox.StageCompletionData
import polyrhythmmania.storymode.screen.ExitCallback
import polyrhythmmania.storymode.screen.ExitReason
import java.time.LocalDateTime
import java.time.ZoneOffset

class DesktopControllerWithUI(val desktopUI: DesktopUI) : DesktopControllerWithPlayLevel() {

    override fun createExitCallback(contract: Contract, inboxItem: InboxItem?, inboxItemState: InboxItemState?): ExitCallback {
        return ExitCallback { exitReason ->
            Paintbox.LOGGER.debug("ExitReason: $exitReason")

            if (inboxItem != null && inboxItemState != null) {
                val currentInboxItem = desktopUI.currentInboxItem
                val scenario = desktopUI.scenario
                val inboxState = scenario.inboxState
                var newState = inboxItemState
                newState = newState.copy(playedBefore = true)

                when (exitReason) {
                    is ExitReason.Passed -> {
                        val now = LocalDateTime.now(ZoneOffset.UTC)
                        val oldCompletion = newState.stageCompletionData
                        
                        if (newState.completion != InboxItemCompletion.COMPLETED || oldCompletion == null) {
                            // Was not previously completed or old data is missing
                            newState = newState.copy(completion = InboxItemCompletion.COMPLETED,
                                    stageCompletionData = StageCompletionData(now, now, exitReason.score,
                                            exitReason.skillStar ?: false, exitReason.noMiss))
                        } else {
                            val oldScore = ExitReason.Passed(oldCompletion.score, oldCompletion.skillStar, oldCompletion.noMiss)
                            if (exitReason.isBetterThan(oldScore)) {
                                val mergedScore = oldScore.createHighScore(exitReason)
                                val scd = StageCompletionData(oldCompletion.firstClearTime, now,
                                        mergedScore.score, mergedScore.skillStar ?: false, mergedScore.noMiss)
                                newState = newState.copy(stageCompletionData = scd)
                            }
                        }
                    }
                    ExitReason.Skipped -> {
                        if (!newState.completion.shouldCountAsCompleted()) {
                            newState = newState.copy(completion = InboxItemCompletion.SKIPPED)
                        }
                    }
                    is ExitReason.Quit -> {
                        if (exitReason.timesFailedThisTime > 0) {
                            newState = newState.copy(failureCount = newState.failureCount + exitReason.timesFailedThisTime)
                        }
                    }
                }

                inboxState.putItemState(inboxItem, newState)
                currentInboxItem.invalidate()
                currentInboxItem.getOrCompute()
                desktopUI.background.resetAll()
                desktopUI.updateAndShowNewlyAvailableInboxItems(lockInputs = true)
                desktopUI.storySession.attemptSave()
            }
        }
    }

    override fun playLevel(contract: Contract, inboxItem: InboxItem?, inboxItemState: InboxItemState?) {
        playLevel(contract, inboxItem, inboxItemState, desktopUI.main, desktopUI.storySession, desktopUI.rootScreen)
    }

    override fun playSFX(sfx: DesktopController.SFXType) {
        val main = desktopUI.main
        val sound: Sound = when (sfx) {
            DesktopController.SFXType.ENTER_LEVEL -> AssetRegistry["sfx_menu_enter_game"]
            DesktopController.SFXType.CLICK_INBOX_ITEM -> AssetRegistry["sfx_menu_blip"]
            DesktopController.SFXType.INBOX_ITEM_UNLOCKED -> StoryAssets["sfx_desk_unlocked"]
            DesktopController.SFXType.PAUSE_ENTER -> AssetRegistry["sfx_pause_enter"]
            DesktopController.SFXType.PAUSE_EXIT -> AssetRegistry["sfx_pause_exit"]
        } ?: return
        
        main.playMenuSfx(sound)
    }
}