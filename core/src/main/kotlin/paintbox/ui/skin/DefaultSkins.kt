package paintbox.ui.skin


object DefaultSkins {
    
    private val skinFactories: MutableMap<String, SkinFactory<*, *, *>> = mutableMapOf()
    
    fun register(id: String, factory: SkinFactory<*, *, *>) {
        skinFactories[id] = factory
    }
    
    fun unregister(id: String): SkinFactory<*, *, *>? {
        return skinFactories.remove(id)
    }
    
    operator fun get(id: String): SkinFactory<*, *, *>? = skinFactories[id]
    
}