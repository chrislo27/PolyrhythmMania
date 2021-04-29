package polyrhythmmania.editor


/**
 * A tool is an enum representing the current available actions in the editor.
 */
enum class Tool(val textureKey: String) {
    SELECTION("ui_icon_tool_selection"),
    TEMPO_CHANGE("ui_icon_tool_tempo_change"),
    
    ;
    
    companion object {
        val VALUES: List<Tool> = values().toList()
    }
}