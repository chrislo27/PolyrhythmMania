package polyrhythmmania.library.menu

import polyrhythmmania.library.LevelEntry


abstract class Filter(val filterable: Filterable) {
    
    abstract fun filter(levelEntry: LevelEntry.Modern): Boolean
    
}
