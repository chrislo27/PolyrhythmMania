package polyrhythmmania.library.menu


enum class Sortable(val nameKey: String) {
    // LevelMetadata
    CREATED_DATE("levelMetadata.initialCreationDate"),
    LEVEL_CREATOR("levelMetadata.levelCreator"),
    SONG_NAME("levelMetadata.songName"), // This is the filename for legacy levels
    SONG_ARTIST("levelMetadata.songArtist"),
    ALBUM_NAME("levelMetadata.albumName"),
    ALBUM_YEAR("levelMetadata.albumYear"),
    GENRE("levelMetadata.genre"),
    DIFFICULTY("levelMetadata.difficulty"),
    
    // ExportStatistics
    DURATION("mainMenu.librarySortFilter.sort.duration"),
    INPUT_COUNT("mainMenu.librarySortFilter.sort.inputCount"),
    INPUT_RATE("mainMenu.librarySortFilter.sort.inputRate"),
    AVERAGE_BPM("mainMenu.librarySortFilter.sort.averageBpm"),
    MIN_BPM("mainMenu.librarySortFilter.sort.minBpm"),
    MAX_BPM("mainMenu.librarySortFilter.sort.maxBpm"),
    
    // Other/ScoreCache
    LAST_PLAYED("mainMenu.librarySortFilter.sort.lastPlayed"),
    PLAY_COUNT("mainMenu.librarySortFilter.sort.playCount"),
    ;
    
    companion object {
        val VALUES: List<Sortable> = values().toList()
    }
}