package polyrhythmmania.storymode.inbox.state

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.Paintbox
import java.time.LocalDateTime
import java.time.ZoneOffset


/**
 * Represents the state of the inbox, with what inbox items are unlocked or not.
 */
class InboxState {
    
    companion object {
        const val JSON_VERSION: Int = 1
        
        fun fromJson(obj: JsonObject): InboxState {
            val inboxState = InboxState()
            val version = obj.get("version").asInt()
            
            val items = obj.get("items").asArray()
            items.forEach { v ->
                val o = v.asObject()
                val state = inboxItemStateFromJson(o, version)
                inboxState.putItemState(state)
            }
            
            return inboxState
        }
        
        private fun inboxItemStateFromJson(obj: JsonObject, version: Int): InboxItemState {
            val itemID = obj.get("id").asString()
            val unlockStateObj = obj.get("unlockState").asObject()
            val unlockState: UnlockState = when (val type = unlockStateObj.get("type").asString()) {
                "available" -> {
                    UnlockState.Available(unlockStateObj.getBoolean("new", true))
                }
                "complete" -> {
                    val stageCompletionDataObj = unlockStateObj.get("stageCompletionData")
                    val stageCompletionData: UnlockState.Completed.StageCompletionData? = if (stageCompletionDataObj.isObject) {
                        // Default to current time
                        val firstClearTime = LocalDateTime.ofEpochSecond(unlockStateObj.getLong("firstClearTime", System.currentTimeMillis() / 1000), 0, ZoneOffset.UTC)
                        UnlockState.Completed.StageCompletionData(firstClearTime)
                    } else {
                        null
                    }
                    UnlockState.Completed(stageCompletionData)
                }
                "skipped" -> UnlockState.Skipped
                "unavailable" -> UnlockState.Unavailable
                else -> {
                    Paintbox.LOGGER.warn("Unknown unlock state for version $version: $type")
                    UnlockState.Unavailable
                }
            }
            return InboxItemState(itemID, unlockState)
        }
        
        private fun InboxItemState.toJson(): JsonObject {
            return Json.`object`().also { obj ->
                obj.add("id", this.itemID)
                obj.add("unlockState", Json.`object`().also { unlockObj ->
                    val unlockState = this.unlockState
                    unlockObj.add("type", when (unlockState) {
                        is UnlockState.Available -> "available"
                        is UnlockState.Completed -> "complete"
                        UnlockState.Skipped -> "skipped"
                        UnlockState.Unavailable -> "unavailable"
                    })
                    when (unlockState) {
                        is UnlockState.Available -> {
                            unlockObj.add("new", unlockState.newIndicator)
                        }
                        is UnlockState.Completed -> {
                            val stageCompletionData = unlockState.stageCompletionData
                            if (stageCompletionData != null) {
                                unlockObj.add("stageCompletionData", Json.`object`().also { o ->
                                    o.add("firstClearTime", stageCompletionData.firstClearTime.toEpochSecond(ZoneOffset.UTC))
                                })
                            } else {
                                unlockObj.add("stageCompletionData", Json.NULL)
                            }
                        }
                        UnlockState.Skipped -> {}
                        UnlockState.Unavailable -> {}
                    }
                })
            }
        }
    }

    data class InboxItemState(val itemID: String, val unlockState: UnlockState)

    
    val itemStates: Map<String, InboxItemState> = mutableMapOf()
    
    
    fun getItemState(itemID: String): InboxItemState? = itemStates[itemID]

    /**
     * Returns the last item or null.
     */
    fun putItemState(inboxItemState: InboxItemState): InboxItemState? = (itemStates as MutableMap).put(inboxItemState.itemID, inboxItemState)
    
    fun removeItemState(itemID: String): InboxItemState? = (itemStates as MutableMap).remove(itemID)
    
    fun toJson(): JsonObject {
        val obj = Json.`object`()
        obj.add("version", JSON_VERSION)
        
        obj.add("items", Json.array().also { arr ->
            itemStates.values.forEach { st ->
                arr.add(st.toJson())
            }
        })
        
        return obj
    }
    
}
