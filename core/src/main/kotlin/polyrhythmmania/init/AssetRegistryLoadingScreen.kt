package polyrhythmmania.init

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.binding.FloatVar
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.SceneRoot
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.element.RectElement
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen

class AssetRegistryLoadingScreen(main: PRManiaGame)
    : PRManiaScreen(main) {

    private enum class Substate {
        BEFORE_START,
        LOADING_ASSETS,
        FINISHED_ASSETS,
        ;
    }

    private val camera: OrthographicCamera = OrthographicCamera().apply {
        setToOrtho(false, 1280f, 720f)
    }
    private val uiViewport: Viewport = FitViewport(camera.viewportWidth, camera.viewportHeight, camera)
    private val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    
    private val loadingBarProgress: FloatVar = FloatVar(0f)

    /**
     * Called at the very start of loading.
     */
    var onStart: () -> Unit = {}

    /**
     * Called when asset loading completes.
     */
    var onAssetLoadingComplete: () -> Unit = {}

    /**
     * Called to create the next screen. The next screen may not necessarily be set at the same time
     * (use [PRManiaScreen.show]/[PRManiaScreen.showTransition] to listen for that).
     */
    var nextScreenProducer: (() -> Screen?) = { null }

    private var substate: Substate = Substate.BEFORE_START
    
    init {
        this.sceneRoot += Pane().apply { 
            this.bounds.width.set(960f)
            this.bounds.height.set(36f)
            Anchor.BottomCentre.configure(this, offsetY = -64f)
            
            val borderColor = Color.WHITE
            val borderSize = 4f
            this.border.set(Insets(borderSize))
            this.borderStyle.set(SolidBorder(borderColor))
            this.padding.set(Insets(borderSize))
            
            this += RectElement(borderColor).apply { 
                this.bindWidthToParent(multiplierBinding = { loadingBarProgress.use() }) { 0f }
            }
        }
    }

    override fun render(delta: Float) {
        if (substate == Substate.BEFORE_START) {
            // Substate changed as part of progress check
            onStart()
        }

        val progress = if (substate == Substate.BEFORE_START) run {
            substate = Substate.LOADING_ASSETS
            0f
        } else AssetRegistry.load(delta)
        this.loadingBarProgress.set(progress)
        
        if (progress >= 1f) {
            if (substate == Substate.LOADING_ASSETS) {
                substate = Substate.FINISHED_ASSETS
                onAssetLoadingComplete()
                val nextScreenRes = nextScreenProducer.invoke()
                Gdx.app.postRunnable {
                    Gdx.app.postRunnable { // Delay by 2 frames
                        main.screen = nextScreenRes
                    }
                }
            }
        }
        

        // Start of rendering -------------------------------------------------------------------------------------
        
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        
        super.render(delta)
        
        val batch = main.batch
        val camera = this.camera
        batch.projectionMatrix = camera.combined
        batch.begin()

        sceneRoot.renderAsRoot(batch)

        batch.end()

        // End of rendering ---------------------------------------------------------------------------------------
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        uiViewport.update(width, height)
    }

    override fun dispose() {
    }
}