package polyrhythmmania.storymode.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.ui.Anchor
import paintbox.ui.ImageIcon
import paintbox.ui.SceneRoot
import paintbox.ui.area.Insets
import paintbox.ui.element.RectElement
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.storymode.StoryAssets


class StoryTitleScreen(main: PRManiaGame) : PRManiaScreen(main) {
    
    val batch: SpriteBatch = main.batch
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport)

    init {
        // FIXME
        sceneRoot += RectElement(Color(1f, 165f / 255f, 0.5f, 1f)).apply { 
            this += ImageIcon(TextureRegion(StoryAssets.get<Texture>("logo"))).apply { 
                this.bindHeightToParent(multiplier = 0.4f)
                this.margin.set(Insets(8f))
                Anchor.TopCentre.configure(this)
            }
        }
    }
    
    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        super.render(delta)

        val camera = uiCamera
        batch.projectionMatrix = camera.combined
        batch.begin()

        sceneRoot.renderAsRoot(batch)

        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        uiViewport.update(width, height)
    }
    
    override fun dispose() {
    }
}