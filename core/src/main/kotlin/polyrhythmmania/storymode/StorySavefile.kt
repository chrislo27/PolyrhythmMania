package polyrhythmmania.storymode

import com.badlogic.gdx.files.FileHandle
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.Paintbox
import polyrhythmmania.PRMania
import polyrhythmmania.statistics.DurationStatFormatter
import polyrhythmmania.statistics.Stat
import polyrhythmmania.statistics.TimeAccumulator
import polyrhythmmania.storymode.inbox.InboxState


class StorySavefile private constructor(val saveNumber: Int) {

    companion object {
        const val NUM_FILES: Int = 3
        const val SAVE_FILE_VERSION: Int = 0
        
        fun getStorageLocForSaveNumber(saveNumber: Int): FileHandle {
            return FileHandle(PRMania.MAIN_FOLDER.resolve("prefs/storymode_save_${saveNumber}.json"))
        }
        
        fun newSaveFile(saveNumber: Int, disableSaving: Boolean = false): StorySavefile {
            return StorySavefile(saveNumber).apply { 
                this.disableSaving = disableSaving
            }
        }
        
        fun newDebugSaveFile(): StorySavefile = newSaveFile(0, disableSaving = true)
        
        fun loadFromSave(saveNumber: Int): StorySavefile? {
            return try {
                StorySavefile(saveNumber).apply {
                    loadFromStorageLoc()
                }
            } catch (e: Exception) {
                Paintbox.LOGGER.error("Failed to load story mode savefile $saveNumber", throwable = e)
                null
            }
        }
        
        fun attemptLoad(saveNumber: Int): LoadedState {
            return try {
                val s = StorySavefile(saveNumber)
                if (s.doesSavefileExist()) {
                    s.loadFromStorageLoc()
                    LoadedState.Loaded(saveNumber, s)
                } else {
                    LoadedState.NoSavefile(saveNumber, s)
                }
            } catch (e: Exception) {
                Paintbox.LOGGER.error("Failed to load story mode savefile $saveNumber", throwable = e)
                LoadedState.FailedToLoad(saveNumber, e, getStorageLocForSaveNumber(saveNumber))
            }
        }
    }

    sealed class LoadedState(val number: Int, val storageLoc: FileHandle) {
        class NoSavefile(number: Int, val blankFile: StorySavefile) : LoadedState(number, blankFile.storageLoc)
        class FailedToLoad(number: Int, val exception: Exception, storageLoc: FileHandle) : LoadedState(number, storageLoc)
        class Loaded(number: Int, val savefile: StorySavefile) : LoadedState(number, savefile.storageLoc)
    }
    

    val storageLoc: FileHandle by lazy { getStorageLocForSaveNumber(this.saveNumber) }
    /**
     * Disables saving. The [persist] function will not do anything if true.
     */
    var disableSaving: Boolean = false
    
    
    val playTime: Stat = Stat("storyModePlayTime", DurationStatFormatter.DEFAULT)
    private val playTimeAccumulator: TimeAccumulator = TimeAccumulator(playTime)
    
    val inboxState: InboxState = InboxState()
    
    
    fun updatePlayTime() {
        playTimeAccumulator.update()
    }
    
    
    fun doesSavefileExist(): Boolean {
        return storageLoc.exists() && !storageLoc.isDirectory
    }
    
    private fun loadFromStorageLoc() {
        if (!doesSavefileExist()) {
            return // No file. Treat as new.
        }
        // May throw any exception if something fails to load.
        
        val str = storageLoc.readString("UTF-8")
        val obj = Json.parse(str).asObject()
        val saveFileVersion = obj.getInt("save_file_version", SAVE_FILE_VERSION)
        
        playTime.setValue(obj.getInt("play_time", 0))
        val inboxStateObj = obj.get("inbox_state").asObject()
        InboxState.fromJson(inboxStateObj, inboxState)
    }
    
    fun toJsonString(): String {
        return Json.`object`().also { obj ->
            toJson(obj)
        }.toString()
    }
    
    fun persist() {
        persistTo(this.storageLoc)
    }
    
    fun persistTo(loc: FileHandle) {
        try {
            // Intentionally build the string here, even if disableSaving == true.
            val jsonString = toJsonString()

            if (disableSaving) {
                Paintbox.LOGGER.debug("Not actually persisting due to disableSaving flag", tag = "StorySavefile")
                return
            }
            
            loc.writeString(jsonString, false, "UTF-8")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun toJson(obj: JsonObject) {
        obj.add("save_file_version", SAVE_FILE_VERSION)
        obj.add("game_version", PRMania.VERSION.toString())
        
        obj.add("play_time", playTime.value.get())
        obj.add("inbox_state", inboxState.toJson())
    }
    
}