package polyrhythmmania.library.menu

import polyrhythmmania.library.LevelEntry
import java.time.LocalDateTime


data class LibrarySortFilter(
        val sortDescending: Boolean,
        val sortOn: Sortable,
        val legacyOnTop: Boolean,
        val filters: List<Filter>,
) {

    companion object {
        val DEFAULT: LibrarySortFilter = LibrarySortFilter(false, Sortable.SONG_NAME, false, emptyList())
    }
    
    private fun filterFunc(item: LevelEntryData): Boolean {
        // TODO
        return true
    }
    
    private fun sortingComparatorModern(): Comparator<LevelEntryData> {
        var cmp: Comparator<LevelEntryData> = when (sortOn) {
            Sortable.CREATED_DATE -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.initialCreationDate
            }
            Sortable.LEVEL_CREATOR -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.levelCreator
            }
            Sortable.SONG_NAME ->Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.songName
            }
            Sortable.SONG_ARTIST -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.songArtist
            }
            Sortable.ALBUM_NAME -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.albumName
            }
            Sortable.ALBUM_YEAR -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.albumYear
            }
            Sortable.GENRE -> Comparator.comparing {
                (it.levelEntry as LevelEntry.Modern).levelMetadata.genre
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
        var cmp = LevelEntryData.comparator.thenComparing { led: LevelEntryData ->
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
