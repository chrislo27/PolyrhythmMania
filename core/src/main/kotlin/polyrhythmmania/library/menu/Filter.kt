package polyrhythmmania.library.menu


abstract class Filter(val filterable: Filterable) {
    
    abstract fun filter(levelEntryData: LevelEntryData): Boolean
    
}

class SimpleFilter(filterable: Filterable) : Filter(filterable) {
    override fun filter(levelEntryData: LevelEntryData): Boolean {
        TODO()
    }
}

class DifficultyFilter : Filter(Filterable.DIFFICULTY) {
    override fun filter(levelEntryData: LevelEntryData): Boolean {
        TODO()
    }
}
