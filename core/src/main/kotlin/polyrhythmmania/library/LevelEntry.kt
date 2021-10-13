package polyrhythmmania.library

import paintbox.util.Version
import polyrhythmmania.container.LevelMetadata
import polyrhythmmania.container.manifest.ExportStatistics
import java.io.File
import java.util.*


sealed class LevelEntry(val uuid: UUID, val file: File, val containerVersion: Int, val programVersion: Version) {

    /**
     * This is a level with no level metadata (legacy).
     *
     * The only available information is the name, taken from the filename.
     */
    class Legacy(file: File, containerVersion: Int, programVersion: Version)
        : LevelEntry(UUID.randomUUID(), file, containerVersion, programVersion)

    class Modern(uuid: UUID, file: File, containerVersion: Int, programVersion: Version,
                 val levelMetadata: LevelMetadata, val exportStatistics: ExportStatistics)
        : LevelEntry(uuid, file, containerVersion, programVersion)

}
