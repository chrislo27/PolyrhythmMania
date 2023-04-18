package polyrhythmmania.util

import polyrhythmmania.soundsystem.sample.GdxAudioReader
import java.io.File
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.time.toJavaDuration


object TempFileUtils {

    // The temp folder is always relative to the user home, regardless of portable mode
    val TEMP_FOLDER: File = File(System.getProperty("user.home") + "/.polyrhythmmania/tmp/").apply { 
        mkdirs()
    }
    const val PREFIX: String = "prmania"
    const val DEFAULT_SUFFIX: String = ".tmp"
    val DEFAULT_MIN_FILE_AGE: Duration = Duration.of(48, ChronoUnit.HOURS)
    
    fun createTempFile(prefixType: String, suffix: String? = DEFAULT_SUFFIX): File {
        if (!TEMP_FOLDER.exists()) TEMP_FOLDER.mkdirs()
        val f = File.createTempFile(if (prefixType.isEmpty()) PREFIX else "${PREFIX}_$prefixType", suffix, TEMP_FOLDER)
        f.deleteOnExit()
        return f
    }
    
    fun clearEntireTempFolder() {
        TEMP_FOLDER.deleteRecursively() // This might not get files with active locks, but will get old unused files
    }
    
    fun clearTempFolderOlderThan(minAge: Duration = DEFAULT_MIN_FILE_AGE) {
        TEMP_FOLDER.walkBottomUp().filter { f ->
            val age = (System.currentTimeMillis() - f.lastModified()).coerceAtLeast(0L).toDuration(DurationUnit.MILLISECONDS).toJavaDuration()
            age > minAge || GdxAudioReader.TEMP_FILE_NAME in f.nameWithoutExtension
        }.forEach { f ->
            f.delete()
        }
    }
}