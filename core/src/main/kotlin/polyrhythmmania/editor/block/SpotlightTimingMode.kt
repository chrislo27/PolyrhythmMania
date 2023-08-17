package polyrhythmmania.editor.block


enum class SpotlightTimingMode(override val jsonId: Int, override val localizationNameKey: String) : CubeTypeLike {
    
    INSTANT(0x0, "blockContextMenu.spotlightTimingMode.instant"),
    IN_ORDER(0x1, "blockContextMenu.spotlightTimingMode.inOrder"),
    SPAWN_PATTERN(0x2, "blockContextMenu.spotlightTimingMode.spawnPattern"),
    ;

    companion object {
        val INDEX_MAP: Map<Int, SpotlightTimingMode> = entries.associateBy { it.jsonId }
    }
}
