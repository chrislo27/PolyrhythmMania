package paintbox.logging

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.StreamUtils
import paintbox.PaintboxGame
import paintbox.lazysound.LazySound
import paintbox.util.BranchedOutputStream
import paintbox.util.MemoryUtils
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.thread

/**
 * Splits the console out/err into a file and normal console.
 */
object SysOutPiper {

    val logFileDatetimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT)

    lateinit var oldOut: PrintStream
        private set
    lateinit var oldErr: PrintStream
        private set

    private lateinit var newOut: BranchedOutputStream
    private lateinit var newErr: BranchedOutputStream

    private lateinit var stream: FileOutputStream

    @Volatile
    private var piped: Boolean = false

    lateinit var logFile: File
        private set
    lateinit var logDatetime: LocalDateTime
        private set

    fun pipe(args: List<String>, game: PaintboxGame, folder: File) {
        if (piped)
            return
        piped = true
        oldOut = System.out
        oldErr = System.err
        logDatetime = LocalDateTime.now()

        folder.mkdirs()
        val file: File = File(folder, "log_${logFileDatetimeFormat.format(logDatetime)}.txt")
        file.createNewFile()
        logFile = file

        stream = FileOutputStream(file)

        val ps = PrintStream(stream)
        ps.println("==============\nAUTO-GENERATED\n==============\n")
        val builder = StringBuilder()
        builder.append("Program Specifications:\n")
        builder.append("    Launch arguments: $args\n")
        builder.append("    Version: " + game.version.toString() + "\n")
        builder.append("    Application type: " + Gdx.app.type.toString() + "\n")
        builder.append("    Lazy loading enabled: " + LazySound.loadLazilyWithAssetManager + "\n")

        builder.append("\n")

        builder.append("System Specifications:\n")
        builder.append("    Java Version: " + System.getProperty("java.version") + " " + System.getProperty(
                "sun.arch.data.model") + " bit" + "\n")
        builder.append("    Java Vendor: ${System.getProperty("java.vendor")}\n")
        builder.append("    Kotlin Version: ${kotlin.KotlinVersion.CURRENT}\n")
        builder.append("    OS Name: " + System.getProperty("os.name") + "\n")
        builder.append("    OS Version: " + System.getProperty("os.version") + "\n")
        builder.append("    JVM memory available: " + MemoryUtils.maxMemoryKiB + " KiB\n")

        builder.append("\n")

        builder.append("Graphics Specifications:\n")
        builder.append("    Resolution: " + Gdx.graphics.width + "x" + Gdx.graphics.height + "\n")
        builder.append("    Fullscreen: " + Gdx.graphics.isFullscreen + "\n")
        builder.append("    GL_VENDOR: " + Gdx.gl.glGetString(GL20.GL_VENDOR) + "\n")
        builder.append("    GL_RENDERER: " + Gdx.gl.glGetString(GL20.GL_RENDERER) + "\n")
        builder.append("    GL_VERSION: " + Gdx.gl.glGetString(GL20.GL_VERSION) + "\n")
        ps.println(builder.toString())
        ps.println("\n")
        ps.flush()

        newOut = BranchedOutputStream(oldOut, stream)
        newErr = BranchedOutputStream(oldErr, stream)

        System.setOut(PrintStream(newOut))
        System.setErr(PrintStream(newErr))

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            StreamUtils.closeQuietly(stream)
        })
    }

}
