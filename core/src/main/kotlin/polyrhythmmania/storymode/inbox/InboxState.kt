package polyrhythmmania.storymode.inbox

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.Paintbox
import paintbox.binding.BooleanVar
import paintbox.binding.ReadOnlyBooleanVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var


/**
 * Represents the state of the inbox, with what inbox items are unlocked or not.
 */
class InboxState {
    
    companion object {
        const val CURRENT_JSON_VERSION: Int = 2
        
        fun fromJson(rootObj: JsonObject, inboxState: InboxState = InboxState()): InboxState {
            inboxState.clear()
            
            val version = rootObj.get("version").asInt()
            
            val items = rootObj.get("items").asArray()
            items.forEach { v ->
                val o = v.asObject()
                val state = inboxItemStateFromJson(o, version)
                val itemID = o.get("id").asString()
                inboxState.putItemState(itemID, state)
            }
            
            InboxStateDatafixes.runDatafix(version, inboxState)
            
            return inboxState
        }
        
        private fun inboxItemStateFromJson(obj: JsonObject, version: Int): InboxItemState {
            val completionStr = obj.get("completion").asString()
            val completionState: InboxItemCompletion = InboxItemCompletion.JSON_MAPPING[completionStr] ?: run {
                Paintbox.LOGGER.warn("Unknown inbox item completion state for version $version: $completionStr")
                InboxItemCompletion.UNAVAILABLE
            }
            val newIndicator = obj.getBoolean("new", InboxItemState.DEFAULT_UNAVAILABLE.newIndicator)
            val stageCompletionData = obj.get("stageCompletionData")?.takeIf { it.isObject }?.let { 
                StageCompletionData.fromJson(it.asObject())
            }
            val playedBefore = obj.getBoolean("playedBefore", completionState.shouldCountAsCompleted())
            val failureCount = obj.getInt("failureCount", 0).coerceAtLeast(0) // Strictly optional and must default to 0
            
            return InboxItemState(completionState, newIndicator, stageCompletionData, playedBefore, failureCount)
        }
        
        private fun inboxItemStateToJson(itemID: String, itemState: InboxItemState): JsonObject {
            return Json.`object`().also { obj ->
                obj.add("id", itemID)
                
                obj.add("completion", itemState.completion.jsonID)
                obj.add("new", itemState.newIndicator)
                val stageCompletionData = itemState.stageCompletionData
                obj.add("stageCompletionData", stageCompletionData?.toJson() ?: Json.NULL)
                obj.add("playedBefore", itemState.playedBefore)
                if (itemState.failureCount > 0) {
                    obj.add("failureCount", itemState.failureCount)
                }
            }
        }
    }

    
    private val itemStates: Map<String, Var<InboxItemState>> = mutableMapOf()
    
    val anyAvailable: ReadOnlyBooleanVar = BooleanVar { 
        onItemStatesChanged.use()
        itemStates.values.any { 
            val state = it.getOrCompute()
            state.newIndicator && state.completion == InboxItemCompletion.AVAILABLE
        }
    }

    /**
     * Changed whenever an item state changes.
     */
    val onItemStatesChanged: ReadOnlyBooleanVar = BooleanVar(false)
    
    fun getAllItemStates(): List<Pair<String, InboxItemState>> = itemStates.entries.map { it.key to it.value.getOrCompute() }
    
    fun getItemState(itemID: String): InboxItemState? = itemStates[itemID]?.getOrCompute()
    fun getItemState(item: InboxItem): InboxItemState? = getItemState(item.id)

    /**
     * Returns the last item or null.
     */
    fun putItemState(itemID: String, inboxItemState: InboxItemState): InboxItemState? {
        itemStates as MutableMap
        val v = itemStates[itemID]
        val old = v?.getOrCompute()
        
        v?.set(inboxItemState) ?: itemStates.put(itemID, Var(inboxItemState))
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
        val old = (itemStates as MutableMap).remove(itemID)?.getOrCompute()
        if (old != null) {
            (onItemStatesChanged as BooleanVar).invert()
        }
        return old
    }
    fun removeItemState(item: InboxItem): InboxItemState? = removeItemState(item.id)
    
    
    fun itemStateVar(itemID: String): ReadOnlyVar<InboxItemState?> {
        return Var.bind {
            val inboxItemStateVar = itemStates[itemID]
            if (inboxItemStateVar != null) {
                inboxItemStateVar.use()
            } else {
                onItemStatesChanged.use()
                null
            }
        }
    }
    fun itemStateVarOrUnavailable(itemID: String): ReadOnlyVar<InboxItemState> {
        val v = itemStateVar(itemID) // This should be cached so a new var doesn't get made each time
        return Var.bind { v.use() ?: InboxItemState.DEFAULT_UNAVAILABLE }
    }
    
    fun clear() {
        itemStates.keys.toList().forEach(this::removeItemState)
    }
    
    fun toJson(): JsonObject {
        val obj = Json.`object`()
        obj.add("version", CURRENT_JSON_VERSION)
        
        obj.add("items", Json.array().also { arr ->
            itemStates.forEach { (itemID, state) ->
                arr.add(inboxItemStateToJson(itemID, state.getOrCompute()))
            }
        })
        
        return obj
    }
    
}
