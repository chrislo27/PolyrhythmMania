package polyrhythmmania.container.manifest

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.util.Version
import polyrhythmmania.PRMania
import polyrhythmmania.container.Container
import polyrhythmmania.container.ContainerException
import polyrhythmmania.container.LevelMetadata
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


/**
 * Library-view specific level metadata. This is a subset of actual level metadata that operates directly
 * on the manifest.json top-level JSON object.
 */
data class LibraryRelevantData(
        val containerVersion: Int, val programVersion: Version,
        val isAutosave: Boolean, val exportStatistics: ExportStatistics?,
        val levelUUID: UUID?, val levelMetadata: LevelMetadata?
) {

    companion object {

        fun fromManifestJson(manifestObj: JsonObject, lastModified: Long): Pair<LibraryRelevantData, LoadInfo> {
            val containerVersion = manifestObj.getInt("containerVersion", 0)
            val programVersion: Version = Version.parse(manifestObj.getString("programVersion", null))
                    ?: throw ContainerException("Missing programVersion field or could not parse version string")
            val isAutosave: Boolean = if (containerVersion >= 10) manifestObj.getBoolean("isAutosave", false) else false
            val isProject: Boolean = if (containerVersion >= 10) manifestObj.getBoolean("isProject", true) else false
            val levelUUID: UUID? = if (containerVersion >= 10 && !isProject) (UUID.fromString(manifestObj.getString("levelUUID", ""))) else null
            
            var wasLevelMetadataLoaded = false
            var levelMetadata: LevelMetadata? = null
            if (containerVersion >= Container.VERSION_LEVEL_METADATA_ADDED) {
                val metadataObj = manifestObj.get("levelMetadata")?.asObject()
                if (metadataObj != null) {
                    levelMetadata = LevelMetadata.fromJson(metadataObj,
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(lastModified), ZoneOffset.UTC))
                            .truncateWithLimits()
                    wasLevelMetadataLoaded = true
                }
            }
            var wasExportStatsLoaded = false
            var exportStatistics: ExportStatistics? = null
            if (containerVersion >= 10 && !isProject) {
                val metadataObj = manifestObj.get("exportStatistics")?.asObject()
                if (metadataObj != null) {
                    exportStatistics = ExportStatistics.fromJson(metadataObj)
                    wasExportStatsLoaded = true
                }
            }
            
            return Pair(LibraryRelevantData(containerVersion, programVersion, isAutosave, exportStatistics, levelUUID,
                    levelMetadata), LoadInfo(wasLevelMetadataLoaded, wasExportStatsLoaded))
        }
    }
    
    data class LoadInfo(val wasLevelMetadataLoaded: Boolean, val wasExportStatisticsLoaded: Boolean)

    val isProject: Boolean = exportStatistics == null
    
    fun writeToManifestJson(jsonObj: JsonObject) {
        jsonObj.add("containerVersion", this.containerVersion)
        jsonObj.add("programVersion", this.programVersion.toString())
        jsonObj.add("isAutosave", this.isAutosave)
        jsonObj.add("isProject", this.isProject)
        if (!this.isProject) {
            jsonObj.add("levelUUID", UUID.randomUUID().toString())
        }
        if (this.levelMetadata != null) {
            jsonObj.add("levelMetadata", levelMetadata.truncateWithLimits().toJson())
        }
        if (this.exportStatistics != null) {
            jsonObj.add("exportStatistics", Json.`object`().also { obj ->
                this.exportStatistics.writeToJson(obj)
            })
        }
    }
}