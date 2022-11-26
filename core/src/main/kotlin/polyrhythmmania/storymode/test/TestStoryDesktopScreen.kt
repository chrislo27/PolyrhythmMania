package polyrhythmmania.storymode.test

import com.badlogic.gdx.Screen
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.inbox.InboxItems
import polyrhythmmania.storymode.inbox.InboxState
import polyrhythmmania.storymode.inbox.progression.Progression
import polyrhythmmania.storymode.screen.EarlyAccessMsgOnBottom
import polyrhythmmania.storymode.screen.desktop.AbstractDesktopScreen
import polyrhythmmania.storymode.screen.desktop.DesktopScenario


class TestStoryDesktopScreen(
        main: PRManiaGame, prevScreen: Screen,
        private val inboxItems: InboxItems,
        private val progression: Progression
) : AbstractDesktopScreen(main, prevScreen, DesktopScenario(
        inboxItems,
        progression,
        InboxState() // Blank state intentionally
)), EarlyAccessMsgOnBottom
