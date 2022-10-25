package polyrhythmmania.storymode.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.binding.ReadOnlyVar
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItemState
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
            InboxItem.Memo("memo0", ReadOnlyVar.const("memo0")),
            InboxItem.Memo("memo1", ReadOnlyVar.const("memo1")),
            InboxItem.Memo("memo2", ReadOnlyVar.const("memo2")),
            InboxItem.Memo("memo3", ReadOnlyVar.const("memo3")),
            InboxItem.Memo("memo4", ReadOnlyVar.const("memo4")),
    ))
    val scenario: DesktopScenario = DesktopScenario(
            inboxItems,
            Progression(inboxItems.items.map { UnlockStage.singleItem(it.id, UnlockStageChecker.alwaysUnlocked()) }),
//            Progression(listOf(
//                    // TODO
//            )),
            InboxState()
    )
    private val desktopUI: DesktopUI = DesktopUI(scenario, this)
    
    init {
        scenario.inboxState.putItemState(inboxItems.items[0], InboxItemState.Completed(null))
        scenario.inboxState.putItemState(inboxItems.items[1], InboxItemState.Skipped)
        scenario.inboxState.putItemState(inboxItems.items[2], InboxItemState.Available(false))
        scenario.inboxState.putItemState(inboxItems.items[3], InboxItemState.Available(true))
        scenario.inboxState.putItemState(inboxItems.items[4], InboxItemState.Unavailable)

        scenario.progression.checkAll(scenario.inboxState)
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
