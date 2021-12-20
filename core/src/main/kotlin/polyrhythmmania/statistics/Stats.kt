package polyrhythmmania.statistics

import com.badlogic.gdx.files.FileHandle
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject


open class Stats {
    
    companion object {
        const val SAVE_VERSION: Int = 1
    }
    
    private val _statMap: MutableMap<String, Stat> = mutableMapOf()
    val statMap: Map<String, Stat> = _statMap
    
    
    protected fun register(stat: Stat): Stat {
        _statMap[stat.id] = stat
        return stat
    }
    
    fun clear() {
        statMap.values.forEach { stat ->
            stat.value.set(0)
        }
    }
    
    fun fromJson(rootObj: JsonObject) {
        clear()
        
        val statsObj = rootObj["stats"].asObject()
        for (stat in statMap.values) {
            try {
                stat.value.set(statsObj.getInt(stat.id, 0))
            } catch (ignored: Exception) {}
        }
    }

    fun fromJsonFile(file: FileHandle) {
        clear()
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
                obj.add(stat.id, stat.value.get())
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