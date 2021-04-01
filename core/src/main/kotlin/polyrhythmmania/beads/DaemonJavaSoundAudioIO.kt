package polyrhythmmania.beads

import net.beadsproject.beads.core.io.JavaSoundAudioIO
import kotlin.concurrent.thread

/**
 * The default [JavaSoundAudioIO] thread is non-daemon. This class uses reflection to use a daemon thread.
 */
class DaemonJavaSoundAudioIO : JavaSoundAudioIO() {
    
    override fun start(): Boolean {
        fun runRealTime() {
            JavaSoundAudioIO::class.java.getDeclaredMethod("runRealTime").apply {
                isAccessible = true
                invoke(this@DaemonJavaSoundAudioIO)
            }
        }

        val audioThread = thread(start = false, isDaemon = true, priority = threadPriority, name = "DaemonJavaSoundAudioIO audio thread") {
            create()
            runRealTime()
            destroy()
        }
        audioThread.start()

        JavaSoundAudioIO::class.java.getDeclaredField("audioThread").apply {
            isAccessible = true
            set(this@DaemonJavaSoundAudioIO, audioThread)
        }

        return true
    }

}