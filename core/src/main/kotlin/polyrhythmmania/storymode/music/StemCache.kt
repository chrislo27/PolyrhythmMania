package polyrhythmmania.storymode.music


class StemCache(map: Map<String, () -> Stem>) {

    private val factory: Map<String, () -> Stem> = map.toMap()
    
    val keys: Set<String> = map.keys
    private val loaded: MutableMap<String, Stem> = mutableMapOf()
    
    
    fun loadAll() {
        keys.forEach(this::get)
    }
    
    fun evict(id: String) {
        val loadedStem = loaded[id] ?: return
        loaded.remove(id, loadedStem)
    }
    
    fun evictAll() {
        keys.forEach(this::evict)
    }
    
    fun getOrLoad(id: String): Stem? {
        if (id !in keys) return null

        val factory = factory[id] ?: return null
        return loaded.getOrPut(id, factory)
    }
    
    operator fun get(id: String): Stem? = getOrLoad(id)
    
}
