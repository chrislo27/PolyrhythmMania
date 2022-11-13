package polyrhythmmania.storymode.contract


data class SongNameAndSource(val songName: String, val songSourceMaterial: String?, val songNameWithLineBreaks: String = songName) {
    companion object {
        fun tengoku(songName: String, songNameWithLineBreaks: String = songName): SongNameAndSource {
            return SongNameAndSource(songName, SongInfo.SRC_TENGOKU, songNameWithLineBreaks)
        }
        fun ds(songName: String, songNameWithLineBreaks: String = songName): SongNameAndSource {
            return SongNameAndSource(songName, SongInfo.SRC_DS, songNameWithLineBreaks)
        }
        fun fever(songName: String, songNameWithLineBreaks: String = songName): SongNameAndSource {
            return SongNameAndSource(songName, SongInfo.SRC_FEVER, songNameWithLineBreaks)
        }
        fun megamix(songName: String, songNameWithLineBreaks: String = songName): SongNameAndSource {
            return SongNameAndSource(songName, SongInfo.SRC_MEGAMIX, songNameWithLineBreaks)
        }
    }
}

/**
 * Attribution rules:
 * - Primary [songNameAndSource] is the first instance of the song
 * - Places where the song appears in future installments with a different name can be put in [otherAliases]
 * - From a RH installment, the source should be one of [SRC_TENGOKU], [SRC_DS], [SRC_FEVER], or [SRC_MEGAMIX],
 * using the North American name with the console in parentheses
 * - For any names with no officially NA-English translation, the original Japanese name is used first and then
 * the colloquial English transliteration ("Rhythm Tengoku", "Toss Boys", etc) in parentheses
 */
data class SongInfo(
        val songNameAndSource: SongNameAndSource,
        val songArtist: String,
        val otherAliases: List<SongNameAndSource> = emptyList(),
) {
    companion object {
        const val ARTIST_NINTENDO: String = "Nintendo"
        const val SRC_TENGOKU: String = "リズム天国 (Rhythm Tengoku) (GBA)"
        const val SRC_DS: String = "Rhythm Heaven (DS)"
        const val SRC_FEVER: String = "Rhythm Heaven Fever (Wii)"
        const val SRC_MEGAMIX: String = "Rhythm Heaven Megamix (3DS)"
        
        fun tengoku(songName: String, otherAliases: List<SongNameAndSource> = emptyList(), songNameWithLineBreaks: String = songName): SongInfo {
            return SongInfo(SongNameAndSource.tengoku(songName, songNameWithLineBreaks), ARTIST_NINTENDO, otherAliases)
        }
        fun ds(songName: String, otherAliases: List<SongNameAndSource> = emptyList(), songNameWithLineBreaks: String = songName): SongInfo {
            return SongInfo(SongNameAndSource.ds(songName, songNameWithLineBreaks), ARTIST_NINTENDO, otherAliases)
        }
        fun fever(songName: String, otherAliases: List<SongNameAndSource> = emptyList(), songNameWithLineBreaks: String = songName): SongInfo {
            return SongInfo(SongNameAndSource.fever(songName, songNameWithLineBreaks), ARTIST_NINTENDO, otherAliases)
        }
        fun megamix(songName: String, otherAliases: List<SongNameAndSource> = emptyList(), songNameWithLineBreaks: String = songName): SongInfo {
            return SongInfo(SongNameAndSource.megamix(songName, songNameWithLineBreaks), ARTIST_NINTENDO, otherAliases)
        }
    }
}
