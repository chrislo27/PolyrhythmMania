package polyrhythmmania.storymode.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.storymode.inbox.InboxItems
import polyrhythmmania.storymode.inbox.InboxState
import polyrhythmmania.storymode.inbox.progression.Progression
import polyrhythmmania.storymode.screen.EarlyAccessMsgOnBottom
import polyrhythmmania.storymode.screen.desktop.DesktopControllerWithUI
import polyrhythmmania.storymode.screen.desktop.DesktopScenario
import polyrhythmmania.storymode.screen.desktop.DesktopUI


class TestStoryDesktopScreen(
        main: PRManiaGame, val prevScreen: Screen,
        private val inboxItems: InboxItems,
        private val progression: Progression
) : PRManiaScreen(main), EarlyAccessMsgOnBottom {

    val batch: SpriteBatch = main.batch

    val scenario: DesktopScenario = DesktopScenario(
            inboxItems,
            progression,
            InboxState() // Blank state intentionally
    )
    private val desktopUI: DesktopUI = DesktopUI(scenario, { dt -> DesktopControllerWithUI(dt) }, this)

    init {
        scenario.updateProgression()
        scenario.updateInboxItemAvailability(scenario.checkItemsThatWillBecomeAvailable())
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
