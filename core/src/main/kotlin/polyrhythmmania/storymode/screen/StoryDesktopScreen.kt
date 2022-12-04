package polyrhythmmania.storymode.screen

import com.badlogic.gdx.Screen
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.StorySession
import polyrhythmmania.storymode.inbox.InboxItemCompletion
import polyrhythmmania.storymode.screen.desktop.AbstractDesktopScreen
import polyrhythmmania.storymode.screen.desktop.DesktopScenario


class StoryDesktopScreen(
        main: PRManiaGame, storySession: StorySession, prevScreen: () -> Screen, scenario: DesktopScenario,
        isBrandNew: Boolean
) : AbstractDesktopScreen(main, storySession, prevScreen, scenario) {
    
    init {
        if (!isBrandNew) {
            scenario.updateProgression()
            scenario.updateInboxItemAvailability(scenario.checkItemsThatWillBecomeAvailable())

            val items = this.scenario.inboxItems.items
            val lastScrollableItem = items.lastOrNull {
                (this.scenario.inboxState.getItemState(it.id)?.completion ?: InboxItemCompletion.UNAVAILABLE) != InboxItemCompletion.UNAVAILABLE
            } ?: items.last()
            this.desktopUI.inboxItemListScrollbar.setValue(this.desktopUI.getTargetVbarValueForInboxItem(lastScrollableItem))
        }
    }
    
}
