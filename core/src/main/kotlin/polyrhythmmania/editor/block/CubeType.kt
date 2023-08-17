package polyrhythmmania.editor.block


/**
 * An enum that has a JSON int ID and localization key.
 */
interface CubeTypeLike {
    val jsonId: Int
    val localizationNameKey: String
}

enum class CubeType(override val jsonId: Int, val character: Char, override val localizationNameKey: String)
    : CubeTypeLike {
    
    NONE(0, '-', "blockContextMenu.spawnPattern.cubeType.none"),
    PISTON(1, 'P', "blockContextMenu.spawnPattern.cubeType.piston"),
    PLATFORM(2, '#', "blockContextMenu.spawnPattern.cubeType.platform"),
    
    NO_CHANGE(3, '_', "blockContextMenu.spawnPattern.cubeType.noChange"),
    PISTON_OPEN(4, 'O', "blockContextMenu.spawnPattern.cubeType.pistonOpen"),
    RETRACT_PISTON(5, 'R', "blockContextMenu.spawnPattern.cubeType.retractPiston"),
    ;

    companion object {
        val INDEX_MAP: Map<Int, CubeType> = entries.associateBy { it.jsonId }
        val CHAR_MAP: Map<Char, CubeType> = entries.associateBy { it.character }
    }
}
