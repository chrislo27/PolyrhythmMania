package polyrhythmmania.storymode.inbox

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.Paintbox
import paintbox.binding.BooleanVar
import paintbox.binding.ReadOnlyBooleanVar
import java.time.LocalDateTime
import java.time.ZoneOffset


/**
 * Represents the state of the inbox, with what inbox items are unlocked or not.
 */
class InboxState {
    
    companion object {
        const val JSON_VERSION: Int = 1
        
        fun fromJson(rootObj: JsonObject): InboxState {
            val inboxState = InboxState()
            val version = rootObj.get("version").asInt()
            
            val items = rootObj.get("items").asArray()
            items.forEach { v ->
                val o = v.asObject()
                val state = inboxItemStateFromJson(o, version)
                val itemID = o.get("id").asString()
                inboxState.putItemState(itemID, state)
            }
            
            return inboxState
        }
        
        private fun inboxItemStateFromJson(obj: JsonObject, version: Int): InboxItemState {
            val unlockStateObj = obj.get("unlockState").asObject()
            val unlockState: InboxItemState = when (val type = unlockStateObj.get("type").asString()) {
                "available" -> {
                    InboxItemState.Available(unlockStateObj.getBoolean("new", true))
                }
                "complete" -> {
                    val stageCompletionDataObj = unlockStateObj.get("stageCompletionData")
                    val stageCompletionData: InboxItemState.Completed.StageCompletionData? = if (stageCompletionDataObj.isObject) {
                        // Default to current time
                        val firstClearTime = LocalDateTime.ofEpochSecond(unlockStateObj.getLong("firstClearTime", System.currentTimeMillis() / 1000), 0, ZoneOffset.UTC)
                        InboxItemState.Completed.StageCompletionData(firstClearTime)
                    } else {
                        null
                    }
                    InboxItemState.Completed(stageCompletionData)
                }
                "skipped" -> InboxItemState.Skipped
                "unavailable" -> InboxItemState.Unavailable
                else -> {
                    Paintbox.LOGGER.warn("Unknown unlock state for version $version: $type")
                    InboxItemState.Unavailable
                }
            }
            return unlockState
        }
        
        private fun inboxItemStateToJson(itemID: String, itemState: InboxItemState): JsonObject {
            return Json.`object`().also { obj ->
                obj.add("id", itemID)
                obj.add("unlockState", Json.`object`().also { unlockObj ->
                    unlockObj.add("type", when (itemState) {
                        is InboxItemState.Available -> "available"
                        is InboxItemState.Completed -> "complete"
                        InboxItemState.Skipped -> "skipped"
                        InboxItemState.Unavailable -> "unavailable"
                    })
                    when (itemState) {
                        is InboxItemState.Available -> {
                            unlockObj.add("new", itemState.newIndicator)
                        }
                        is InboxItemState.Completed -> {
                            val stageCompletionData = itemState.stageCompletionData
                            if (stageCompletionData != null) {
                                unlockObj.add("stageCompletionData", Json.`object`().also { o ->
                                    o.add("firstClearTime", stageCompletionData.firstClearTime.toEpochSecond(ZoneOffset.UTC))
                                })
                            } else {
                                unlockObj.add("stageCompletionData", Json.NULL)
                            }
                        }
                        InboxItemState.Skipped -> {}
                        InboxItemState.Unavailable -> {}
                    }
                })
            }
        }
    }

    
    private val itemStates: Map<String, InboxItemState> = mutableMapOf()

    /**
     * Changed whenever an item state changes.
     */
    val onItemStatesChanged: ReadOnlyBooleanVar = BooleanVar(false)
    
    
    fun getItemState(itemID: String): InboxItemState? = itemStates[itemID]
    fun getItemState(item: InboxItem): InboxItemState? = getItemState(item.id)

    /**
     * Returns the last item or null.
     */
    fun putItemState(itemID: String, inboxItemState: InboxItemState): InboxItemState? {
        itemStates as MutableMap
        val old = itemStates.put(itemID, inboxItemState)
        if (old != inboxItemState) {
            (onItemStatesChanged as BooleanVar).invert()
        }
        return old
    }
    /**
     * Returns the last item or null.
     */
    fun putItemState(item: InboxItem, inboxItemState: InboxItemState): InboxItemState? = putItemState(item.id, inboxItemState)
    
    
    fun removeItemState(itemID: String): InboxItemState? {
        val old = (itemStates as MutableMap).remove(itemID)
        if (old != null) {
            (onItemStatesChanged as BooleanVar).invert()
        }
        return old
    }
    fun removeItemState(item: InboxItem): InboxItemState? = removeItemState(item.id)
    
    fun toJson(): JsonObject {
        val obj = Json.`object`()
        obj.add("version", JSON_VERSION)
        
        obj.add("items", Json.array().also { arr ->
            itemStates.forEach { (itemID, state) ->
                arr.add(inboxItemStateToJson(itemID, state))
            }
        })
        
        return obj
    }
    
}
