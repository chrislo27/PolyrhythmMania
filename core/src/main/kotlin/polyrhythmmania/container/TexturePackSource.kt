package polyrhythmmania.container


enum class TexturePackSource(val jsonId: Int) {
    STOCK_GBA(0),
    STOCK_HD(1),
    CUSTOM(-1);
    
    companion object {
        val VALUES: List<TexturePackSource> = values().toList()
        val INDEX_MAP: Map<Int, TexturePackSource> = VALUES.associateBy { it.jsonId }
    }
}
