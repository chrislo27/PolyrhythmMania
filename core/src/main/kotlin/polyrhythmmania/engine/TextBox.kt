package polyrhythmmania.engine


enum class TextBoxStyle {
    DIALOGUE, BLACK;
}

data class TextBox(val text: String, val requiresInput: Boolean,
                   val secsBeforeCanInput: Float = 0.5f, val style: TextBoxStyle = TextBoxStyle.BLACK) {
    fun toActive(): ActiveTextBox = ActiveTextBox(this)
}

data class ActiveTextBox(val textBox: TextBox) {
    var secondsTimer: Float = textBox.secsBeforeCanInput
    var isADown: Boolean = false
    var wasSoundInterfacePaused: Boolean = false
}
