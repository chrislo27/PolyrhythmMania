package polyrhythmmania.world.render

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.utils.Disposable
import paintbox.util.gdxutils.NestedFrameBuffer
import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.Container
import polyrhythmmania.container.GlobalContainerSettings
import polyrhythmmania.soundsystem.SimpleTimingProvider


/**
 * Used to render a [Container] to force JIT to occur
 */
class PreloadWorldScene(private val main: PRManiaGame) : Disposable {

    private val container: Container = Container(
        null,
        SimpleTimingProvider { false },
        GlobalContainerSettings(
            forceTexturePack = ForceTexturePack.NO_FORCE,
            forceTilesetPalette = ForceTilesetPalette.NO_FORCE,
            forceSignLanguage = ForceSignLanguage.NO_FORCE,
            reducedMotion = false
        )
    )
    private var frameBuffer: NestedFrameBuffer? = null
    
    init {
        try {
            frameBuffer = NestedFrameBuffer(Pixmap.Format.RGBA8888, 1280, 720, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Must be created in the GL thread
        container.resetMutableState()
    }
    
    fun render() {
        val fb = frameBuffer
        fb?.begin()
        container.renderer.render(main.batch)
        fb?.end()
    }
    
    override fun dispose() {
        frameBuffer?.dispose()
    }
}