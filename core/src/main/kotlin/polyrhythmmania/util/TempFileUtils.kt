package polyrhythmmania.util

import java.io.File


object TempFileUtils {
    
    const val PREFIX: String = "prmania"
    const val DEFAULT_SUFFIX: String = ".tmp"
    
    fun createTempFile(prefixType: String, deleteOnExit: Boolean, suffix: String? = DEFAULT_SUFFIX): File {
        val f = File.createTempFile(if (prefixType.isEmpty()) PREFIX else "${PREFIX}_$prefixType", suffix)
        if (deleteOnExit) f.deleteOnExit()
        return f
    }
}