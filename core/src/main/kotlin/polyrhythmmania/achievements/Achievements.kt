package polyrhythmmania.achievements

import com.badlogic.gdx.files.FileHandle
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.Paintbox
import polyrhythmmania.PRMania
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.statistics.Stat
import polyrhythmmania.statistics.StatTrigger
import java.time.Instant


object Achievements {
    
    fun interface FulfillmentListener {
        fun onFulfilled(achievement: Achievement)
    }
    
    const val SAVE_VERSION: Int = 1
    
    private val storageLoc: FileHandle by lazy { FileHandle(PRMania.MAIN_FOLDER.resolve("prefs/achievements.json")) }
    
    private val _achIDMap: MutableMap<String, Achievement> = linkedMapOf()
    val achievementIDMap: Map<String, Achievement> = _achIDMap
    
    private val _achFulfillmentMap: MutableMap<Achievement, Fulfillment> = mutableMapOf()
    val fulfillmentMap: Map<Achievement, Fulfillment> = _achFulfillmentMap
    
    val fulfillmentListeners: MutableList<FulfillmentListener> = mutableListOf()
    
    // -----------------------------------------------------------------------------------------------------------------

    
    
    // -----------------------------------------------------------------------------------------------------------------
    
    private fun <A : Achievement> register(ach: A): A {
        _achIDMap[ach.id] = ach
        if (ach is Achievement.StatTriggered) {
            // Add stat trigger for fulfillment
            ach.stat.triggers += StatTrigger { stat, oldValue, newValue ->
                if (newValue >= ach.threshold) {
                    fulfillAchievement(ach)
                }
            }
        }
        return ach
    }
    
    // -----------------------------------------------------------------------------------------------------------------
    
    fun clearAllFulfilledAchievements() {
        _achFulfillmentMap.clear()
    }

    /**
     * Marks the [achievement] as fulfilled.
     */
    fun fulfillAchievement(achievement: Achievement) {
        if (achievement !in _achFulfillmentMap) {
            _achFulfillmentMap[achievement] = Fulfillment(Instant.now())
            fulfillmentListeners.forEach { it.onFulfilled(achievement) }
        }
    }

    /**
     * Manually forces a check for all [Achievement.StatTriggered] achievements.
     */
    fun checkAllStatTriggeredAchievements() {
        _achIDMap.values.forEach { ach ->
            if (ach is Achievement.StatTriggered) {
                val newValue = ach.stat.value.get()
                if (newValue >= ach.threshold) {
                    fulfillAchievement(ach)
                }
            }
        }
    }
    
    // -----------------------------------------------------------------------------------------------------------------
    
    fun fromJson(rootObj: JsonObject) {
        clearAllFulfilledAchievements()
        
        val achObj = rootObj["achievements"].asObject()
        for (ach in achievementIDMap.values) {
            try {
                val fObj = achObj[ach.id].asObject()
                if (fObj != null) {
                    val f = Fulfillment.fromJson(fObj) ?: continue
                    _achFulfillmentMap[ach] = f
                }
            } catch (ignored: Exception) {}
        }
    }

    fun fromJsonFile(file: FileHandle) {
        clearAllFulfilledAchievements()
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
        rootObj.add("achievements", Json.`object`().also { obj ->
            fulfillmentMap.forEach { (ach, ful) ->
                obj.add(ach.id, Json.`object`().also { o ->
                    ful.toJson(o)
                })
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

    fun load() {
        Paintbox.LOGGER.debug("Achievements loaded", "Achievements")
        this.fromJsonFile(storageLoc)
    }

    fun persist() {
        Paintbox.LOGGER.debug("Achievements saved", "Achievements")
        this.toJsonFile(storageLoc)
    }
}