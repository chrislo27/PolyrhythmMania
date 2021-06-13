package polyrhythmmania.editor


/**
 * A tool is an enum representing the current available actions in the editor.
 */
enum class Tool(val textureKey: String, val localizationKey: String) {
    SELECTION("selection", "tool.selection.name"),
    TEMPO_CHANGE("tempo_change", "tool.tempoChange.name"),
    MUSIC_VOLUME("music_volume", "tool.musicVolume.name"),
    TIME_SIGNATURE("time_signature", "tool.timeSignature.name"),
    
    ;
    
    companion object {
        val VALUES: List<Tool> = values().toList()
    }
}