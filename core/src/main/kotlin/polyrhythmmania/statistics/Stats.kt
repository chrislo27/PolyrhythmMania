package polyrhythmmania.statistics

import com.badlogic.gdx.files.FileHandle
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject


abstract class Stats {
    
    companion object {
        const val SAVE_VERSION: Int = 1
    }
    
    private val _statMap: MutableMap<String, Stat> = linkedMapOf()
    val statMap: Map<String, Stat> = _statMap
    
    
    protected fun register(stat: Stat): Stat {
        _statMap[stat.id] = stat
        return stat
    }
    
    fun resetToInitialValues() {
        statMap.values.forEach { stat ->
            stat.setValue(stat.initialValue)
        }
    }
    
    fun resetToResetValues() {
        statMap.values.forEach { stat ->
            stat.setValue(stat.resetValue)
        }
    }
    
    fun fromJson(rootObj: JsonObject) {
        resetToInitialValues()
        
        val statsObj = rootObj["stats"].asObject()
        for (stat in statMap.values) {
            try {
                stat.setValue(statsObj.getInt(stat.id, stat.initialValue))
            } catch (ignored: Exception) {}
        }
    }

    fun fromJsonFile(file: FileHandle) {
        resetToInitialValues()
        if (!file.exists() || file.isDirectory) return
        
        return try {
            val str = file.readString("UTF-8")
            fromJson(Json.parse(str).asObject())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toJson(rootObj: JsonObject) {
        rootObj.add("version", SAVE_VERSION)
        rootObj.add("stats", Json.`object`().also { obj ->
            statMap.values.forEach { stat ->
                val value = stat.value.get()
                if (value != stat.initialValue) {
                    obj.add(stat.id, value)
                }
            }
        })
    }

    fun toJsonFile(file: FileHandle) {
        try {
            file.writeString(Json.`object`().also { obj ->
                toJson(obj)
            }.toString(), false, "UTF-8")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
}