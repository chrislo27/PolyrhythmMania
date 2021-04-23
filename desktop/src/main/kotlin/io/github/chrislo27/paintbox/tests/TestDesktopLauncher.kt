package io.github.chrislo27.paintbox.tests

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.HdpiMode
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.PaintboxSettings
import io.github.chrislo27.paintbox.ResizeAction
import io.github.chrislo27.paintbox.desktop.PaintboxDesktopLauncher
import io.github.chrislo27.paintbox.logging.Logger
import io.github.chrislo27.paintbox.tests.newui.NewUITestGame
import io.github.chrislo27.paintbox.tests.newui.ScaledFontTestGame
import io.github.chrislo27.paintbox.tests.textblocks.TextBlockTestGame
import io.github.chrislo27.paintbox.util.Version
import io.github.chrislo27.paintbox.util.WindowSize
import org.lwjgl.glfw.GLFW


internal object TestDesktopLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        // https://github.com/chrislo27/RhythmHeavenRemixEditor/issues/273
        System.setProperty("jna.nosys", "true")

        fun getDefaultLauncher(app: PaintboxGame): PaintboxDesktopLauncher {
            return PaintboxDesktopLauncher(app).editConfig {
                this.setAutoIconify(true)
                val emulatedSize = app.paintboxSettings.emulatedSize
                this.setWindowedMode(emulatedSize.width, emulatedSize.height)
                this.setTitle(app.getTitle())
                this.setResizable(true)
                this.useVsync(true)
                this.setForegroundFPS(60)
                this.setIdleFPS(60)
                this.setInitialBackgroundColor(Color(0f, 0f, 0f, 1f))
                this.setAudioConfig(100, 4096, 16)
                this.setHdpiMode(HdpiMode.Logical)
            }
        }

        val logger = Logger()
        val settings = PaintboxSettings(args.toList(), logger, null, Version(0, 1, 0), 
                WindowSize(1280, 720), ResizeAction.ANY_SIZE, WindowSize(800, 450))
        val test1: Pair<PaintboxGame, (PaintboxDesktopLauncher) -> Unit> by lazy {
            TestGame(settings) to {}
        }
        val test2: Pair<PaintboxGame, (PaintboxDesktopLauncher) -> Unit> by lazy {
            TextBlockTestGame(settings) to {}
        }
        val test3: Pair<PaintboxGame, (PaintboxDesktopLauncher) -> Unit> by lazy {
            NewUITestGame(settings) to {}
        }
        val test4: Pair<PaintboxGame, (PaintboxDesktopLauncher) -> Unit> by lazy {
            ScaledFontTestGame(settings) to {}
        }
        
        val selectedTest: Pair<PaintboxGame, (PaintboxDesktopLauncher) -> Unit> = test4
        getDefaultLauncher(selectedTest.first).apply {
            game.programLaunchArguments = args.toList()
            selectedTest.second.invoke(this)
        }
                .launch()
    }
}