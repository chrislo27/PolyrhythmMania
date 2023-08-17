package polyrhythmmania.engine

import paintbox.font.TextAlign
import polyrhythmmania.editor.EditorSpecialFlags
import java.util.*


enum class TextBoxStyle(
        val jsonId: Int,
        val requiredEditorFlags: EnumSet<EditorSpecialFlags> = EnumSet.noneOf(EditorSpecialFlags::class.java)
) {
    
    DIALOGUE(0),
    BANNER(1),
    
    SM_ROBOT(1000, EnumSet.of(EditorSpecialFlags.STORY_MODE))
    ;
    
    companion object {
        val JSON_MAPPING: Map<Int, TextBoxStyle> = entries.associateBy { it.jsonId }
    }
}

data class TextBox(
        val text: String, val requiresInput: Boolean,
        val secsBeforeCanInput: Float = 0.5f,
        val style: TextBoxStyle = TextBoxStyle.DIALOGUE,
        val align: TextAlign = TextAlign.LEFT
) {
    fun toActive(): ActiveTextBox = ActiveTextBox(this)
}

data class ActiveTextBox(val textBox: TextBox) {
    var secondsTimer: Float = textBox.secsBeforeCanInput
    var isADown: Boolean = false
    var wasSoundInterfacePaused: Boolean = false
    
    var onComplete: (Engine) -> Unit = {}
}
