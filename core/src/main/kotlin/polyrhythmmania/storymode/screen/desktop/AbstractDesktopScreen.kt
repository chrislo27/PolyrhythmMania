package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.storymode.StorySession


abstract class AbstractDesktopScreen(
        main: PRManiaGame, val storySession: StorySession,
        val prevScreen: () -> Screen, val scenario: DesktopScenario,
) : PRManiaScreen(main) {
    
    protected val batch: SpriteBatch = main.batch
    
    val desktopUI: DesktopUI by lazy { DesktopUI(scenario, { dt -> DesktopControllerWithUI(dt) }, this) }
    

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        super.render(delta)

        // Render desktop UI components
        desktopUI.render(batch)
    }

    override fun renderUpdate() {
        super.renderUpdate()
        storySession.renderUpdate()
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
