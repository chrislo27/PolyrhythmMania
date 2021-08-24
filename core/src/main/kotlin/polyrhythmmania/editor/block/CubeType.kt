package polyrhythmmania.editor.block


enum class CubeType(val jsonId: Int, val character: Char, val localizationNameKey: String) {
    
    NONE(0, '-', "blockContextMenu.spawnPattern.cubeType.none"),
    PISTON(1, 'P', "blockContextMenu.spawnPattern.cubeType.piston"),
    PLATFORM(2, '#', "blockContextMenu.spawnPattern.cubeType.platform"),
    
    NO_CHANGE(3, '_', "blockContextMenu.spawnPattern.cubeType.noChange"),
    PISTON_OPEN(4, 'O', "blockContextMenu.spawnPattern.cubeType.pistonOpen"),
    ;

    companion object {
        val VALUES: List<CubeType> = values().toList()
        val INDEX_MAP: Map<Int, CubeType> = VALUES.associateBy { it.jsonId }
        val CHAR_MAP: Map<Char, CubeType> = VALUES.associateBy { it.character }
    }
}
