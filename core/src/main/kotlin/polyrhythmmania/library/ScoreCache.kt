package polyrhythmmania.library

import com.badlogic.gdx.files.FileHandle
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import java.util.*


/**
 * A cache of scores for levels. A level is represented by a UUID.
 */
data class ScoreCache(val map: Map<UUID, LevelScore>) {

    companion object {
        fun fromJson(obj: JsonObject): ScoreCache {
            val map: MutableMap<UUID, LevelScore> = mutableMapOf()
            val mapArray: JsonArray = obj.get("map")?.takeIf { it.isArray }?.asArray() ?: Json.array()
            mapArray.forEach { item ->
                try {
                    val itemObj = item.asObject()
                    val levelScore = LevelScore.fromJson(itemObj)
                    map[levelScore.uuid] = levelScore
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return ScoreCache(map)
        }

        fun fromJsonFile(file: FileHandle): ScoreCache {
            if (!file.exists() || file.isDirectory) return ScoreCache(emptyMap())
            return try {
                val str = file.readString("UTF-8")
                fromJson(Json.parse(str).asObject())
            } catch (e: Exception) {
                e.printStackTrace()
                ScoreCache(emptyMap())
            }
        }
    }

    fun keepXBestAttempts(limit: Int = 10): ScoreCache {
        return this.copy(map = this.map.values.map { it.keepXBestAttempts(limit) }.associateBy { it.uuid })
    }

    fun toJson(obj: JsonObject) {
        obj.add("map", Json.array().also { arr ->
            map.values.forEach { ls ->
                try {
                    arr.add(Json.`object`().also { o ->
                        ls.toJson(o)
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
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