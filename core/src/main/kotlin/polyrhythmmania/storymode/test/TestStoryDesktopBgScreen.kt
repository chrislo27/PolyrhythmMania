package polyrhythmmania.storymode.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.ui.SceneRoot
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.storymode.screen.EarlyAccessMsgOnBottom
import polyrhythmmania.storymode.screen.desktop.DesktopBackground
import polyrhythmmania.storymode.screen.desktop.DesktopUI


class TestStoryDesktopBgScreen(
        main: PRManiaGame, val prevScreen: Screen,
) : PRManiaScreen(main), EarlyAccessMsgOnBottom {
    
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 320f * DesktopUI.UI_SCALE, 180f * DesktopUI.UI_SCALE) // 320x180 is the virtual resolution. Needs to be 4x (1280x720) for font scaling
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    
    val bg: DesktopBackground = DesktopBackground(uiCamera)


    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        super.render(delta)

        val batch = main.batch
        val camera = uiCamera
        batch.projectionMatrix = camera.combined
        batch.begin()
        bg.render(batch)
        sceneRoot.renderAsRoot(batch)
        batch.end()
    }

    override fun shouldMakeTextTransparent(): Boolean {
        return true
    }

    override fun renderUpdate() {
        super.renderUpdate()
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            main.screen = prevScreen
        } else if (Gdx.input.isKeyJustPressed(Keys.S)) {
            bg.sendEnvelope()
        } else if (Gdx.input.isKeyJustPressed(Keys.R)) {
            bg.resetPistons()
            bg.removeAllEnvelopes()
        }
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        uiViewport.update(width, height)
    }
    
    override fun dispose() {
    }
}
