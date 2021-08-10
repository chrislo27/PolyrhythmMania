package polyrhythmmania.editor.block


enum class CubeType(val jsonId: Int, val character: Char) {
    NONE(0, '-'),
    PISTON(1, 'P'),
    PLATFORM(2, '#'),
    
    NO_CHANGE(3, '_'),
    PISTON_OPEN(4, 'O'),
    ;

    companion object {
        val VALUES: List<CubeType> = values().toList()
        val INDEX_MAP: Map<Int, CubeType> = VALUES.associateBy { it.jsonId }
        val CHAR_MAP: Map<Char, CubeType> = VALUES.associateBy { it.character }
    }
}
