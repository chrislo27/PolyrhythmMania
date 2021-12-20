package polyrhythmmania.world.render


enum class ForceTilesetPalette(val jsonId: Int) {
    
    NO_FORCE(0), FORCE_PR1(1), FORCE_PR2(2);
    
    companion object {
        val VALUES: List<ForceTilesetPalette> = values().toList()
        val JSON_MAP: Map<Int, ForceTilesetPalette> = VALUES.associateBy { it.jsonId }
    }
}
