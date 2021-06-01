package polyrhythmmania.init

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.util.gdxutils.drawRect
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
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

    override fun render(delta: Float) {
        if (substate == Substate.BEFORE_START) {
            // Substate changed as part of progress check
            onStart()
        }

        val progress = if (substate == Substate.BEFORE_START) run {
            substate = Substate.LOADING_ASSETS
            0f
        } else AssetRegistry.load(delta)

        // Start of rendering -------------------------------------------------------------------------------------
        val cam = camera
        val batch = main.batch
        batch.projectionMatrix = cam.combined

        val viewportWidth = cam.viewportWidth
        val viewportHeight = cam.viewportHeight
        val width = 960f
        val height = 24f
        val line = height / 8f
        val x = viewportWidth * 0.5f - width * 0.5f
        val y = 64f

        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        batch.fillRect(x, y, width * progress, height)
        batch.drawRect(x - line * 2, y - line * 2, width + (line * 4), height + (line * 4), line)
        batch.end()

        super.render(delta)

        // End of rendering ---------------------------------------------------------------------------------------

        if (progress >= 1f) {
            if (substate == Substate.LOADING_ASSETS) {
                substate = Substate.FINISHED_ASSETS
                onAssetLoadingComplete()
                val nextScreenRes = nextScreenProducer.invoke()
                Gdx.app.postRunnable {
                    main.screen = nextScreenRes
                }
            }
        }
    }

    override fun dispose() {
    }
}