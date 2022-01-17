package polyrhythmmania.world.render


enum class ForceTexturePack(val jsonId: Int) {
    
    NO_FORCE(0), FORCE_GBA(1), FORCE_HD(2), FORCE_ARCADE(3);
    
    companion object {
        val VALUES: List<ForceTexturePack> = values().toList()
        val JSON_MAP: Map<Int, ForceTexturePack> = VALUES.associateBy { it.jsonId }
    }
}
