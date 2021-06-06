package paintbox.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import paintbox.PaintboxGame
import paintbox.util.CloseListener


/**
 * The launcher to use for desktop applications.
 * The system property `file.encoding` is set to `UTF-8`.
 */
class PaintboxDesktopLauncher(val game: PaintboxGame, val arguments: PaintboxArguments) {

    val config: Lwjgl3ApplicationConfiguration = Lwjgl3ApplicationConfiguration()

    init {
        System.setProperty("file.encoding", "UTF-8")

        val fps = arguments.fps
        config.setForegroundFPS(fps)
        config.setIdleFPS(fps)
    }

    inline fun editConfig(func: Lwjgl3ApplicationConfiguration.() -> Unit): PaintboxDesktopLauncher {
        config.func()
        return this
    }

    fun launch(): Lwjgl3Application {
        val app = object : Lwjgl3Application(game, config) {
            override fun exit() {
                if ((game as? CloseListener)?.attemptClose() != false) {
                    super.exit()
                }
            }
        }
        return app
    }

}
