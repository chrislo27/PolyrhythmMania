package polyrhythmmania.container

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import java.time.LocalDateTime
import java.time.Year
import java.time.format.DateTimeFormatter


data class LevelMetadata(
        /** UTC */ val initialCreationDate: LocalDateTime,
        /** Not persisted, taken from file proeprty */ val lastModifiedDate: LocalDateTime,
        val levelCreator: String,
        val description: String,
        val songName: String,
        val songArtist: String,
        val albumName: String,
        val albumYear: Int,
        val genre: String,
) {
    
    companion object {
        const val LIMIT_LEVEL_CREATOR: Int = 100
        const val LIMIT_DESCRIPTION: Int = 256
        const val LIMIT_SONG_NAME: Int = 64
        const val LIMIT_ARTIST_NAME: Int = 64
        const val LIMIT_ALBUM_NAME: Int = 64
        const val LIMIT_GENRE: Int = 32
        val LIMIT_YEAR: IntRange = 0..Year.MAX_VALUE
        
        val DEFAULT_DATE: LocalDateTime = LocalDateTime.of(2021, 8, 26, 0, 0, 0)
        const val BLANK_YEAR: Int = 0
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
        
        val DEFAULT_METADATA: LevelMetadata = LevelMetadata(
                DEFAULT_DATE, DEFAULT_DATE, "", "", "", "", "", BLANK_YEAR, "",
        )
        
        fun fromJson(obj: JsonObject, lastModifiedDate: LocalDateTime): LevelMetadata {
            return LevelMetadata(try {
                LocalDateTime.parse(obj.getString("initialCreationDate", ""), DATE_FORMATTER)
            } catch (ignored: Exception) {
                DEFAULT_DATE
            }, lastModifiedDate,
                    obj.getString("levelCreator", DEFAULT_METADATA.levelCreator),
                    obj.getString("description", DEFAULT_METADATA.description),
                    obj.getString("songName", DEFAULT_METADATA.songName),
                    obj.getString("songArtist", DEFAULT_METADATA.songArtist),
                    obj.getString("albumName", DEFAULT_METADATA.albumName),
                    obj.getInt("albumYear", DEFAULT_METADATA.albumYear),
                    obj.getString("genre", DEFAULT_METADATA.genre),
            )
        }
    }
    
    fun toJson(): JsonObject {
        val obj = Json.`object`()
        obj.set("initialCreationDate", initialCreationDate.format(DATE_FORMATTER))
        obj.set("levelCreator", levelCreator)
        obj.set("description", description)
        obj.set("songName", songName)
        obj.set("songArtist", songArtist)
        obj.set("albumName", albumName)
        obj.set("albumYear", albumYear)
        obj.set("genre", genre)
        return obj
    }

    /**
     * Checks the fields and their limits. If no limits were hit, returns a copy of this [LevelMetadata] object
     * with any invalid fields truncated.
     */
    fun truncateWithLimits(): LevelMetadata {
        return this.copy(
                levelCreator = this.levelCreator.take(LIMIT_LEVEL_CREATOR),
                description = this.description.take(LIMIT_DESCRIPTION),
                songName = this.songName.take(LIMIT_SONG_NAME),
                songArtist = this.songArtist.take(LIMIT_ARTIST_NAME),
                albumName = this.albumName.take(LIMIT_ALBUM_NAME),
                albumYear = this.albumYear.coerceIn(LIMIT_YEAR),
                genre = this.genre.take(LIMIT_GENRE),
        )
    }
}
