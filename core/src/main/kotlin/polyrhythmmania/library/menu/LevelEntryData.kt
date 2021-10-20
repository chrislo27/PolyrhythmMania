package polyrhythmmania.library.menu

import polyrhythmmania.library.LevelEntry

data class LevelEntryData(val levelEntry: LevelEntry, val button: LibraryEntryButton) {
    
    companion object {
        /**
         * Compares on [LevelEntry's default comparator][LevelEntry.comparator].
         */
        val comparator: Comparator<LevelEntryData> = Comparator.comparing({ it.levelEntry }, LevelEntry.comparator)
    }
    
}
