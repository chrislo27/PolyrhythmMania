package polyrhythmmania.util

import com.badlogic.gdx.files.FileHandle
import paintbox.Paintbox
import paintbox.packing.PackedSheet


object DumpPackedSheets {
    
    fun dump(all: List<PackedSheet>) {
        if (all.isEmpty()) {
            Paintbox.LOGGER.info("No packed sheets to dump (list was empty)")
            return
        }
        
        val tmpDir = FileHandle.tempDirectory("prmania_packedsheets")
        val size = all.size
        val sizeDigits = size.toString().length
        all.forEachIndexed { index, sheet ->
            val file = tmpDir.child("ps-${index.toString().padStart(sizeDigits, '0')}.png")
            sheet.outputToFile(file)
        }
        Paintbox.LOGGER.info("[DumpPackedSheets] Dumped ${all.size} sheets to ${tmpDir.file().absolutePath}")
    }
    
}