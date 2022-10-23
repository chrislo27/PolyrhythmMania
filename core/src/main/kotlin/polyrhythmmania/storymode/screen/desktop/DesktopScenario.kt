package polyrhythmmania.storymode.screen.desktop

import polyrhythmmania.storymode.inbox.InboxItems
import polyrhythmmania.storymode.inbox.state.InboxState
import polyrhythmmania.storymode.inbox.unlock.Progression


data class DesktopScenario(
        val inboxItems: InboxItems,
        val progression: Progression,
        val inboxState: InboxState,
)
