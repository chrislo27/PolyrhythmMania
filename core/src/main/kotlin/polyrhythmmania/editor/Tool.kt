package polyrhythmmania.editor


/**
 * A tool is an enum representing the current available actions in the editor.
 */
enum class Tool(val textureKey: String, val localizationKey: String) {
    SELECTION("ui_icon_tool_selection", "tool.selection.name"),
    TEMPO_CHANGE("ui_icon_tool_tempo_change", "tool.tempoChange.name"),
    MUSIC_VOLUME("ui_icon_tool_music_volume", "tool.musicVolume.name"),
    
    ;
    
    companion object {
        val VALUES: List<Tool> = values().toList()
    }
}