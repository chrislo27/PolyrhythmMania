package polyrhythmmania.library.menu

import polyrhythmmania.library.LevelEntry
import polyrhythmmania.library.score.GlobalScoreCache
import java.util.*


data class LibrarySortFilter(
        val sortDescending: Boolean,
        val sortOn: Sortable,
        val legacyOnTop: Boolean,
        
        val filterLevelCreator: FilterOnStringList,
        val filterSongName: FilterOnStringList,
        val filterSongArtist: FilterOnStringList,
        val filterAlbumName: FilterOnStringList,
        val filterAlbumYear: FilterInteger,
        val filterGenre: FilterOnStringList,
        val filterDifficulty: FilterInteger,
) {

    companion object {
        val DEFAULT: LibrarySortFilter = LibrarySortFilter(false, Sortable.SONG_NAME, false,
                FilterOnStringList(false, Filterable.LEVEL_CREATOR, "", listOf("")),
                FilterOnStringList(false, Filterable.SONG_NAME, "", listOf("")),
                FilterOnStringList(false, Filterable.SONG_ARTIST, "", listOf("")),
                FilterOnStringList(false, Filterable.ALBUM_NAME, "", listOf("")),
                FilterInteger(false, Filterable.ALBUM_YEAR, FilterInteger.Op.EQ, 0, true),
                FilterOnStringList(false, Filterable.GENRE, "", listOf("")),
                FilterInteger(false, Filterable.DIFFICULTY, FilterInteger.Op.EQ, 0, true),
        )
    }

    val filters: List<Filter> = listOf(filterLevelCreator, filterSongName, filterSongArtist, filterAlbumName, filterAlbumYear, filterGenre, filterDifficulty)
    val enabledFilters: List<Filter> = filters.filter { it.enabled }
    val anyFiltersEnabled: Boolean = enabledFilters.isNotEmpty()
    
    private fun filterFunc(item: LevelEntryData): Boolean {
        if (anyFiltersEnabled) {
            if (item.levelEntry is LevelEntry.Legacy) return false

            val levelEntry = item.levelEntry as LevelEntry.Modern
            return enabledFilters.all { filter ->
                filter.filter(levelEntry)
            }
        } else {
            return true
        }
    }
    
    private fun sortingComparatorModern(): Comparator<LevelEntryData> {
        var cmp: Comparator<LevelEntryData> = when (sortOn) {
            Sortable.CREATED_DATE -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.initialCreationDate
            }
            Sortable.LEVEL_CREATOR -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.levelCreator.lowercase(Locale.ROOT)
            }
            Sortable.SONG_NAME ->Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.songName.lowercase(Locale.ROOT)
            }
            Sortable.SONG_ARTIST -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.songArtist.lowercase(Locale.ROOT)
            }
            Sortable.ALBUM_NAME -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.albumName.lowercase(Locale.ROOT)
            }
            Sortable.ALBUM_YEAR -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.albumYear
            }
            Sortable.GENRE -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.genre.lowercase(Locale.ROOT)
            }
            Sortable.DIFFICULTY -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.difficulty
            }
            Sortable.DURATION -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).exportStatistics.durationSec
            }
            Sortable.INPUT_COUNT -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).exportStatistics.inputCount
            }
            Sortable.INPUT_RATE -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).exportStatistics.averageInputsPerMinute
            }
            Sortable.AVERAGE_BPM -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).exportStatistics.averageBPM
            }
            Sortable.MIN_BPM -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).exportStatistics.minBPM
            }
            Sortable.MAX_BPM -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).exportStatistics.maxBPM
            }
            Sortable.LAST_PLAYED -> {
                Comparator.comparing<LevelEntryData?, Long?> {
                    GlobalScoreCache.scoreCache.getOrCompute().map[it.levelEntry.uuid]?.lastPlayed?.epochSecond ?: -1L
                }.reversed()
            }
        }
        cmp = cmp.thenComparing { led: LevelEntryData ->
            led.levelEntry.uuid
        }
        if (sortDescending) {
            cmp = cmp.reversed()
        }
        return cmp
    }
    
    private fun sortingComparatorLegacy(): Comparator<LevelEntryData> {
        var cmp: Comparator<LevelEntryData> = when (sortOn) {
            Sortable.LAST_PLAYED -> {
                Comparator.comparing<LevelEntryData?, Long?> {
                    GlobalScoreCache.scoreCache.getOrCompute().map[it.levelEntry.uuid]?.lastPlayed?.epochSecond ?: -1L
                }.reversed()
            }
            else -> LevelEntryData.comparator
        }
        cmp = cmp.thenComparing { led: LevelEntryData ->
            led.levelEntry.uuid
        }
        if (sortDescending) {
            cmp = cmp.reversed()
        }
        return cmp
    }
    
    fun sortAndFilter(list: List<LevelEntryData>): List<LevelEntryData> {
        val filtered: List<LevelEntryData> = list.filter(this::filterFunc)
        var modern: List<LevelEntryData> = filtered.filter { it.levelEntry is LevelEntry.Modern }
        var legacy: List<LevelEntryData> = filtered.filter { it.levelEntry is LevelEntry.Legacy}
        
        modern = modern.sortedWith(sortingComparatorModern())
        legacy = legacy.sortedWith(sortingComparatorLegacy())
        
        return if (legacyOnTop) (legacy + modern) else (modern + legacy)
    }
    
}
