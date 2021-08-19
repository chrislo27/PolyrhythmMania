package polyrhythmmania.util

import polyrhythmmania.PRMania
import java.io.File


object TempFileUtils {

    val TEMP_FOLDER: File = PRMania.MAIN_FOLDER.resolve("tmp/").apply { 
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