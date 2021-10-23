package polyrhythmmania.library.menu


enum class Filterable(val nameKey: String) {
    // LevelMetadata
    LEVEL_CREATOR("levelMetadata.levelCreator"), // Based on what's available
    SONG_NAME("levelMetadata.songName"), // Based on what's available
    SONG_ARTIST("levelMetadata.songArtist"), // Based on what's available
    ALBUM_NAME("levelMetadata.albumName"), // Based on what's available
    ALBUM_YEAR("levelMetadata.albumYear"), // Numeric
    GENRE("levelMetadata.genre"), // Based on preset genres + what's available
    DIFFICULTY("levelMetadata.difficulty"), // Numeric
}