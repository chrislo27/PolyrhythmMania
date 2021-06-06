package polyrhythmmania.desktop

import com.badlogic.gdx.Files
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import paintbox.desktop.PaintboxDesktopLauncher
import paintbox.logging.Logger
import polyrhythmmania.PRMania
import polyrhythmmania.PRManiaGame

object DesktopLauncher {

    private fun printHelp(jCommander: JCommander) {
        println("${PRMania.TITLE} ${PRMania.VERSION}\n\n${StringBuilder().apply { jCommander.usageFormatter.usage(this) }}")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // https://github.com/chrislo27/RhythmHeavenRemixEditor/issues/273
        System.setProperty("jna.nosys", "true")

        try {
            // Check for bad arguments but don't cause a full crash
            JCommander.newBuilder().acceptUnknownOptions(false).addObject(PRManiaArguments()).build().parse(*args)
        } catch (e: ParameterException) {
            println("WARNING: Failed to parse arguments. Check below for details and help documentation. You may have strange parse results from ignoring unknown options.\n")
            e.printStackTrace()
            println("\n\n")
            printHelp(JCommander(PRManiaArguments()))
            println("\n\n")
        }

        val arguments = PRManiaArguments()
        val jcommander = JCommander.newBuilder().acceptUnknownOptions(true).addObject(arguments).build()
        jcommander.parse(*args)

        if (arguments.printHelp) {
            printHelp(jcommander)
            return
        }

        val app = PRManiaGame(PRManiaGame.createPaintboxSettings(args.toList(), Logger(), PRMania.MAIN_FOLDER.resolve("logs/")))
        PaintboxDesktopLauncher(app, arguments).editConfig {
            this.setAutoIconify(true)
            val emulatedSize = app.paintboxSettings.emulatedSize
            this.setWindowedMode(emulatedSize.width, emulatedSize.height)
            this.setWindowSizeLimits(PRMania.MINIMUM_SIZE.width, PRMania.MINIMUM_SIZE.height, -1, -1)
            this.setTitle(app.getTitle())
            this.setResizable(true)
            this.useVsync(true)
            this.setForegroundFPS(60)
            this.setIdleFPS(60)
            this.setInitialBackgroundColor(Color(0f, 0f, 0f, 1f))
            this.setAudioConfig(100, 4096, 16)
            this.setHdpiMode(HdpiMode.Logical)
            this.setBackBufferConfig(8, 8, 8, 8, 16, 0, /* samples = */ 2)
            this.setPreferencesConfig(".polyrhythmmania/prefs/", Files.FileType.External)
            
            val sizes: List<Int> = listOf(32, 24, 16)
            this.setWindowIcon(Files.FileType.Internal, *sizes.map { "icon/$it.png" }.toTypedArray())
        }
                .launch()
    }

}
