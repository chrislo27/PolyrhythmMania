package polyrhythmmania.statistics

import com.badlogic.gdx.files.FileHandle
import polyrhythmmania.PRMania


object GlobalStats : Stats() {
    
    private val storageLoc: FileHandle by lazy { FileHandle(PRMania.MAIN_FOLDER.resolve("prefs/statistics.json")) }
    
    // Register statistics
    
    
    // ---------------------------------------------------------------------------------------------------------------
    
    fun load() {
        this.fromJsonFile(storageLoc)
    }
    
    fun persist() {
        this.toJsonFile(storageLoc)
    }
    
}