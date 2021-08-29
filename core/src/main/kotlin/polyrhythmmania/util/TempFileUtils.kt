package polyrhythmmania.util

import java.io.File


object TempFileUtils {

    // The temp folder is always relative to the user home, regardless of portable mode
    val TEMP_FOLDER: File = File(System.getProperty("user.home") + "/.polyrhythmmania/tmp/").apply { 
        mkdirs()
    }
    const val PREFIX: String = "prmania"
    const val DEFAULT_SUFFIX: String = ".tmp"
    
    fun createTempFile(prefixType: String, suffix: String? = DEFAULT_SUFFIX): File {
        if (!TEMP_FOLDER.exists()) TEMP_FOLDER.mkdirs()
        val f = File.createTempFile(if (prefixType.isEmpty()) PREFIX else "${PREFIX}_$prefixType", suffix, TEMP_FOLDER)
        f.deleteOnExit()
        return f
    }
    
    fun clearTempFolder() {
        TEMP_FOLDER.deleteRecursively() // This might not get files with active locks, but will get old unused files
    }
}