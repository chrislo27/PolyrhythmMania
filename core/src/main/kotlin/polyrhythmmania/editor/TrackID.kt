package polyrhythmmania.editor


enum class TrackID(val id: String, val deprecatedIDs: List<String>) {
    
    INPUT_0("input_0", emptyList()),
    INPUT_1("input_1", emptyList()),
    INPUT_2("input_2", emptyList()),
    FX_0("fx_0", emptyList()),
    FX_1("fx_1", emptyList()),
    FX_2("fx_2", emptyList()),
    ;
    
    companion object {
        val VALUES: List<TrackID> = values().toList()
        val ID_MAP: Map<String, TrackID> = VALUES.associateBy { it.id }
        val FULL_ID_MAP: Map<String, TrackID> = VALUES.flatMap { trackID ->
            listOf(trackID.id to trackID) + trackID.deprecatedIDs.map { id -> id to trackID }
        }.toMap()
    }
    
}
