package polyrhythmmania.storymode.screen.desktop

import polyrhythmmania.storymode.inbox.InboxItems
import polyrhythmmania.storymode.inbox.InboxState
import polyrhythmmania.storymode.inbox.progression.Progression


data class DesktopScenario(
        val inboxItems: InboxItems,
        val progression: Progression,
        val inboxState: InboxState,
)
