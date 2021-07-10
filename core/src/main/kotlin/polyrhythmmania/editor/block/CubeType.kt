package polyrhythmmania.editor.block


enum class CubeType(val jsonId: Int) {
    NONE(0),
    PISTON(1),
    PLATFORM(2),
    
    NO_CHANGE(3),
    PISTON_OPEN(4),
    ;

    companion object {
        val VALUES: List<CubeType> = values().toList()
        val INDEX_MAP: Map<Int, CubeType> = VALUES.associateBy { it.jsonId }
    }
}
