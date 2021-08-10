package polyrhythmmania.sidemodes


import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Disposable
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.Container
import polyrhythmmania.engine.Engine
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.world.World


abstract class SideMode(val main: PRManiaGame) : Disposable {

    val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem().apply {
        this.audioContext.out.gain = main.settings.gameplayVolume.getOrCompute() / 100f
    }
    val timingProvider: SimpleTimingProvider = SimpleTimingProvider {
        Gdx.app.postRunnable {
            throw it
        }
        true
    }
    val container: Container = Container(soundSystem, timingProvider)

    val engine: Engine = container.engine
    val world: World = engine.world

    /**
     * Call the first time to initialize the scene.
     */
    fun prepare() {
        initialize()
    }

    /**
     * Implementors should set up music and other long-load items here.
     */
    protected abstract fun initialize()

    override fun dispose() {
        container.disposeQuietly()
    }
    
    open fun getDebugString(): String = ""
}