package polyrhythmmania.storymode

import com.badlogic.gdx.files.FileHandle
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.Paintbox
import polyrhythmmania.PRMania
import polyrhythmmania.statistics.DurationStatFormatter
import polyrhythmmania.statistics.Stat
import polyrhythmmania.statistics.TimeAccumulator
import polyrhythmmania.storymode.contract.ContractCompletion


class StorySavefile private constructor(val saveNumber: Int) {
    
    companion object {
        
        const val NUM_FILES: Int = 3
        const val SAVE_FILE_VERSION: Int = 0
        
        fun newSaveFile(saveNumber: Int, disableSaving: Boolean = false): StorySavefile {
            return StorySavefile(saveNumber).apply { 
                this.disableSaving = disableSaving
            }
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

    /**
     * Disables saving. The [persist] function will not do anything if true.
     */
    var disableSaving: Boolean = false
    
    
    val playTime: Stat = Stat("storyModePlayTime", DurationStatFormatter.DEFAULT)
    private val playTimeAccumulator: TimeAccumulator = TimeAccumulator(playTime)
    val contractCompletion: MutableMap<String, ContractCompletion> = mutableMapOf()
    
    
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
        contractCompletion.clear()
        obj.get("skipped_contracts").asArray().forEach { value ->
            contractCompletion[value.asString()] = ContractCompletion.Skipped
        }
        obj.get("completed_contracts").asObject().forEach { member ->
            val completionData = member.value.asObject()
            contractCompletion[member.name] = ContractCompletion.Completed() // TODO use member.value
        }
    }
    
    fun persist() {
        if (disableSaving) return
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
        obj.add("skipped_contracts", Json.array().also { arr ->
            contractCompletion.filter { (_, v) -> v == ContractCompletion.Skipped }.forEach { (k, _) -> arr.add(k) }
        })
        obj.add("completed_contracts", Json.`object`().also { completedObj ->
            contractCompletion.filter { (_, v) -> v is ContractCompletion.Completed }.forEach { (k, v) ->
                v as ContractCompletion.Completed
                
                completedObj.add(k, Json.`object`().also { contractObj ->
                    // TODO
                })
            }
        })
    }
    
}