package polyrhythmmania.gamemodes


import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Disposable
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.Container
import polyrhythmmania.container.GlobalContainerSettings
import polyrhythmmania.engine.Engine
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.statistics.PlayTimeType
import polyrhythmmania.world.World


abstract class GameMode(val main: PRManiaGame, val playTimeType: PlayTimeType) : Disposable {

    val soundSystem: SoundSystem = SoundSystem.createDefaultSoundSystem().apply {
        this.audioContext.out.gain = main.settings.gameplayVolume.getOrCompute() / 100f
    }
    val timingProvider: SimpleTimingProvider = SimpleTimingProvider {
        Gdx.app.postRunnable {
            throw it
        }
        true
    }
    val container: Container = Container(soundSystem, timingProvider, createGlobalContainerSettings())

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
    
    open fun renderUpdate() {}

    protected open fun createGlobalContainerSettings(): GlobalContainerSettings {
        return GlobalContainerSettings(main.settings.forceTexturePack.getOrCompute(), main.settings.forceTilesetPalette.getOrCompute())
    }

    override fun dispose() {
        container.disposeQuietly()
    }
    
    open fun getDebugString(): String = ""
}