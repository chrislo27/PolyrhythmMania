package polyrhythmmania.world.render


enum class ForceTilesetPalette(val jsonId: Int) {
    
    NO_FORCE(0), FORCE_PR1(1), FORCE_PR2(2),
    ORANGE_BLUE(10),
    ;
    
    companion object {
        val JSON_MAP: Map<Int, ForceTilesetPalette> = entries.associateBy { it.jsonId }
    }
}
