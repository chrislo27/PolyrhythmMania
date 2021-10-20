package polyrhythmmania.library.menu


class LibrarySortFilter(val filterFunc: (LevelEntryData) -> Boolean, val sorting: Comparator<LevelEntryData>) {

    companion object {
        val DEFAULT: LibrarySortFilter = LibrarySortFilter({ true }, LevelEntryData.comparator)
    }
    
    fun filter(list: List<LevelEntryData>): List<LevelEntryData> {
        return list.filter(filterFunc)
    }
    
    fun filterMutable(list: MutableList<LevelEntryData>) {
        list.removeIf { !filterFunc(it) }
    }
    
    fun sort(list: List<LevelEntryData>): List<LevelEntryData> {
        return list.sortedWith(sorting)
    }
    
    fun sortMutable(list: MutableList<LevelEntryData>) {
        list.sortWith(sorting)
    }
    
    fun sortAndFilter(list: List<LevelEntryData>): List<LevelEntryData> {
        val result = list.toMutableList()
        filterMutable(result)
        sortMutable(result)
        return result
    }
    
}
