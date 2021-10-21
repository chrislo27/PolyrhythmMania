package polyrhythmmania.library

import paintbox.util.Version
import polyrhythmmania.container.LevelMetadata
import polyrhythmmania.container.manifest.ExportStatistics
import java.io.File
import java.util.*
import kotlin.Comparator


sealed class LevelEntry(val uuid: UUID, val file: File, val containerVersion: Int, val programVersion: Version) {

    companion object {
        val legacyModernComparator: Comparator<LevelEntry> = Comparator { first, second ->
            if (first is Modern && second is Legacy) -1
            else if (first is Legacy && second is Modern) 1
            else 0
        }
        val comparator: Comparator<LevelEntry> = Comparator { first, second ->
            if (first is Modern && second is Legacy) -1
            else if (first is Legacy && second is Modern) 1
            else {
                if (first is Legacy && second is Legacy) {
                    first.file.compareTo(second.file)
                } else {
                    first as Modern
                    second as Modern
                    
                    val titleCompare = first.getTitle().compareTo(second.getTitle())
                    if (titleCompare == 0) {
                        first.getSubtitle().compareTo(second.getSubtitle())
                    } else titleCompare
                }
            }
        }
    }

    /**
     * This is a level with no level metadata (legacy).
     *
     * The only available information is the name, taken from the filename.
     */
    class Legacy(file: File, containerVersion: Int, programVersion: Version)
        : LevelEntry(UUID.randomUUID(), file, containerVersion, programVersion) {

        private val title: String = file.nameWithoutExtension

        override fun getTitle(): String {
            return title
        }

        override fun getSubtitle(): String = ""
    }

    class Modern(uuid: UUID, file: File, containerVersion: Int, programVersion: Version,
                 val levelMetadata: LevelMetadata, val exportStatistics: ExportStatistics)
        : LevelEntry(uuid, file, containerVersion, programVersion) {
        
        private val title: String = levelMetadata.songName.replace("\n", "")
        private val subtitle: String = levelMetadata.getFullAlbumInfo().replace("\n", "")

        override fun getTitle(): String = title
        override fun getSubtitle(): String = subtitle
    }

    abstract fun getTitle(): String
    abstract fun getSubtitle(): String
}
