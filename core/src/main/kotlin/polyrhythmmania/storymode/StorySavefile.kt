package polyrhythmmania.storymode

import com.badlogic.gdx.files.FileHandle
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.Paintbox
import polyrhythmmania.PRMania
import polyrhythmmania.statistics.DurationStatFormatter
import polyrhythmmania.statistics.Stat
import polyrhythmmania.statistics.TimeAccumulator


class StorySavefile private constructor(val saveNumber: Int) {
    
    companion object {
        
        const val NUM_FILES: Int = 3
        const val SAVE_FILE_VERSION: Int = 0
        
        fun newSaveFile(saveNumber: Int): StorySavefile {
            return StorySavefile(saveNumber)
        }
        
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
                LoadedState.FailedToLoad(saveNumber, e)
            }
        }
    }

    sealed class LoadedState(val number: Int) {
        class NoSavefile(number: Int, val blankFile: StorySavefile) : LoadedState(number)
        class FailedToLoad(number: Int, val exception: Exception) : LoadedState(number)
        class Loaded(number: Int, val savefile: StorySavefile) : LoadedState(number)
    }
    

    val storageLoc: FileHandle by lazy { FileHandle(PRMania.MAIN_FOLDER.resolve("prefs/storymode_save_${saveNumber}.json")) }
    
    
    val playTime: Stat = Stat("storyModePlayTime", DurationStatFormatter.DEFAULT)
    private val playTimeAccumulator: TimeAccumulator = TimeAccumulator(playTime)
    
    
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
        // TODO load
    }
    
    fun persist() {
        try {
            storageLoc.writeString(Json.`object`().also { obj ->
                toJson(obj)
            }.toString(), false, "UTF-8")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun toJson(obj: JsonObject) {
        obj.add("save_file_version", SAVE_FILE_VERSION)
        obj.add("game_version", PRMania.VERSION.toString())
        
        obj.add("play_time", playTime.value.get())
        // TODO write data
    }
    
}