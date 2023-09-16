package polyrhythmmania.world.render


enum class ForceSignLanguage(val jsonId: Int) {
    
    NO_FORCE(0), FORCE_JAPANESE(1), FORCE_ENGLISH(2),
    ;
    
    companion object {
        val JSON_MAP: Map<Int, ForceSignLanguage> = entries.associateBy { it.jsonId }
    }
}
