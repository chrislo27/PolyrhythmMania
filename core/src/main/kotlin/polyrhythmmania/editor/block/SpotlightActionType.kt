package polyrhythmmania.editor.block


enum class SpotlightActionType(override val jsonId: Int, override val localizationNameKey: String) : CubeTypeLike {
    
    NO_CHANGE(0x2, "blockContextMenu.spotlightActionType.noChange"),
    TURN_ON(0x1, "blockContextMenu.spotlightActionType.on"),
    TURN_OFF(0x0, "blockContextMenu.spotlightActionType.off"),
    ;

    companion object {
        val VALUES: List<SpotlightActionType> = values().toList()
        val INDEX_MAP: Map<Int, SpotlightActionType> = VALUES.associateBy { it.jsonId }
    }
}
