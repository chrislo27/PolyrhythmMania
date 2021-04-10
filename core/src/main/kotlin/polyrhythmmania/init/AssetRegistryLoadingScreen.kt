package polyrhythmmania.init

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.util.gdxutils.drawRect
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen

class AssetRegistryLoadingScreen(main: PRManiaGame)
    : PRManiaScreen(main) {

    private val camera: OrthographicCamera = OrthographicCamera().apply {
        setToOrtho(false, 1280f, 720f)
    }
    var onStart: () -> Unit = {}
    var nextScreen: (() -> Screen?) = {null}
    private var firstFrame = true
    private var invokedNextScreen = false

    override fun render(delta: Float) {
        if (firstFrame) {
            onStart()
        }
        
        val progress = if (firstFrame) run {
            firstFrame = false
            0f
        } else AssetRegistry.load(delta)

        val cam = camera
        val batch = main.batch
        batch.projectionMatrix = cam.combined

        batch.setColor(1f, 1f, 1f, 1f)

        val viewportWidth = cam.viewportWidth
        val viewportHeight = cam.viewportHeight
        val width = viewportWidth * 0.75f
        val height = viewportHeight * 0.05f
        val offsetY = -220f
        val line = height / 8f

        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        

        batch.fillRect(viewportWidth * 0.5f - width * 0.5f,
                       viewportHeight * 0.5f - (height) * 0.5f + offsetY,
                       width * progress, height)
        batch.drawRect(viewportWidth * 0.5f - width * 0.5f - line * 2,
                       viewportHeight * 0.5f - (height) * 0.5f - line * 2 + offsetY,
                       width + (line * 4), height + (line * 4),
                       line)

        batch.end()
        
        super.render(delta)

        if (progress >= 1f && !invokedNextScreen) {
            invokedNextScreen = true
            main.screen = nextScreen.invoke()
        }
    }

    override fun dispose() {
    }
}