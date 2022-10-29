package polyrhythmmania.storymode.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItem.Debug.DebugSubtype
import polyrhythmmania.storymode.inbox.InboxItems
import polyrhythmmania.storymode.inbox.InboxState
import polyrhythmmania.storymode.inbox.progression.Progression
import polyrhythmmania.storymode.inbox.progression.UnlockStage
import polyrhythmmania.storymode.inbox.progression.UnlockStageChecker
import polyrhythmmania.storymode.screen.desktop.DesktopScenario
import polyrhythmmania.storymode.screen.desktop.DesktopUI


class TestStoryDesktopScreen(main: PRManiaGame, val prevScreen: Screen)
    : PRManiaScreen(main) {

    val batch: SpriteBatch = main.batch

    private val inboxItems: InboxItems = InboxItems(listOf(
            InboxItem.Debug("debug0", "1st item", DebugSubtype.PROGRESSION_ADVANCER, "This item is always unlocked to start. This will unlock the 2nd item once COMPLETED"),
            InboxItem.Debug("debug1", "2nd item", DebugSubtype.PROGRESSION_ADVANCER, "This will unlock both the 3rd and 4th items once COMPLETED"),
            InboxItem.Debug("debug2a", "3rd item", DebugSubtype.PROGRESSION_ADVANCER, "This unlocks the 5th item once this and 4th item are COMPLETED"),
            InboxItem.Debug("debug2b", "4th item", DebugSubtype.PROGRESSION_ADVANCER, "This unlocks the 5th item once this and 3rd item are COMPLETED"),
            InboxItem.Debug("debug3", "5th item", DebugSubtype.PROGRESSION_ADVANCER),
//            InboxItem.Debug("debug4", "item4", DebugSubtype.PROGRESSION_ADVANCER),
    ))
    private val progression: Progression = Progression(listOf(
            UnlockStage.singleItem("debug0", UnlockStageChecker.alwaysUnlocked(), stageID = "stage0"),
            UnlockStage.singleItem("debug1", UnlockStageChecker.stageToBeCompleted("stage0"), stageID = "stage1"),
            UnlockStage("stage2", UnlockStageChecker.stageToBeCompleted("stage1"), listOf("debug2a", "debug2b")),
            UnlockStage.singleItem("debug3", UnlockStageChecker.stageToBeCompleted("stage2"), stageID = "stage3"),
    ))
    val scenario: DesktopScenario = DesktopScenario(
            inboxItems,
            progression,
            InboxState() // Blank state intentionally
    )
    private val desktopUI: DesktopUI = DesktopUI(scenario, this)
    
    init {
//        scenario.inboxState.putItemState(inboxItems.items[0], InboxItemState.Completed(null))
//        scenario.inboxState.putItemState(inboxItems.items[1], InboxItemState.Skipped)
//        scenario.inboxState.putItemState(inboxItems.items[2], InboxItemState.Available(false))
//        scenario.inboxState.putItemState(inboxItems.items[3], InboxItemState.Available(true))
//        scenario.inboxState.putItemState(inboxItems.items[4], InboxItemState.Unavailable)

        scenario.updateProgression()
        scenario.updateInboxItemAvailability()
    }


    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        super.render(delta)

        // Render desktop UI components
        val camera = desktopUI.uiCamera
        batch.projectionMatrix = camera.combined
        batch.begin()
        desktopUI.sceneRoot.renderAsRoot(batch)
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        desktopUI.onResize(width, height)
    }

    override fun show() {
        super.show()
        desktopUI.enableInputs()
    }

    override fun hide() {
        super.hide()
        desktopUI.disableInputs()
    }

    override fun dispose() {
        desktopUI.dispose()
    }
}
