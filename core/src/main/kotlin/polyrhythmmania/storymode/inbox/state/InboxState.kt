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
            val unlockState: InboxItemUnlockState = when (val type = unlockStateObj.get("type").asString()) {
                "available" -> {
                    InboxItemUnlockState.Available(unlockStateObj.getBoolean("new", true))
                }
                "complete" -> {
                    val stageCompletionDataObj = unlockStateObj.get("stageCompletionData")
                    val stageCompletionData: InboxItemUnlockState.Completed.StageCompletionData? = if (stageCompletionDataObj.isObject) {
                        // Default to current time
                        val firstClearTime = LocalDateTime.ofEpochSecond(unlockStateObj.getLong("firstClearTime", System.currentTimeMillis() / 1000), 0, ZoneOffset.UTC)
                        InboxItemUnlockState.Completed.StageCompletionData(firstClearTime)
                    } else {
                        null
                    }
                    InboxItemUnlockState.Completed(stageCompletionData)
                }
                "skipped" -> InboxItemUnlockState.Skipped
                "unavailable" -> InboxItemUnlockState.Unavailable
                else -> {
                    Paintbox.LOGGER.warn("Unknown unlock state for version $version: $type")
                    InboxItemUnlockState.Unavailable
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
                        is InboxItemUnlockState.Available -> "available"
                        is InboxItemUnlockState.Completed -> "complete"
                        InboxItemUnlockState.Skipped -> "skipped"
                        InboxItemUnlockState.Unavailable -> "unavailable"
                    })
                    when (unlockState) {
                        is InboxItemUnlockState.Available -> {
                            unlockObj.add("new", unlockState.newIndicator)
                        }
                        is InboxItemUnlockState.Completed -> {
                            val stageCompletionData = unlockState.stageCompletionData
                            if (stageCompletionData != null) {
                                unlockObj.add("stageCompletionData", Json.`object`().also { o ->
                                    o.add("firstClearTime", stageCompletionData.firstClearTime.toEpochSecond(ZoneOffset.UTC))
                                })
                            } else {
                                unlockObj.add("stageCompletionData", Json.NULL)
                            }
                        }
                        InboxItemUnlockState.Skipped -> {}
                        InboxItemUnlockState.Unavailable -> {}
                    }
                })
            }
        }
    }

    data class InboxItemState(val itemID: String, val unlockState: InboxItemUnlockState)

    
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
