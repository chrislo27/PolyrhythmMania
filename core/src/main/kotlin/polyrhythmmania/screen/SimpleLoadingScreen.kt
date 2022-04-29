package polyrhythmmania.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.SceneRoot
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen


/**
 * A simple black screen with "Loading..." at the bottom right.
 */
class SimpleLoadingScreen(main: PRManiaGame) : PRManiaScreen(main) {

    private val batch: SpriteBatch = main.batch
    private val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    private val sceneRoot: SceneRoot = SceneRoot(uiCamera)
    
    init {
        val pane = Pane().apply { 
            this.margin.set(Insets(48f))
            
            this += TextLabel(Localization.getVar("loadingScreen.loading"), font = main.fontMainMenuHeading).apply { 
                Anchor.BottomRight.configure(this)
                this.renderAlign.set(Align.bottomRight)
                this.bounds.height.set(64f)
                this.bounds.width.set(300f)
                this.textColor.set(Color().grey(0.9f))
            }
        }
        sceneRoot += pane
    }
    
    override fun render(delta: Float) {
        val camera = uiCamera
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        
        super.render(delta)

        batch.projectionMatrix = camera.combined
        batch.begin()

        sceneRoot.renderAsRoot(batch)

        batch.end()
    }

    override fun dispose() {
    }
}