package paintbox.font

import com.badlogic.gdx.graphics.Color


/**
 * Font and style information about a single run of text. A run can have multiple lines (newlines).
 * Runs of text are rendered together by "joining" them after one another.
 */
data class TextRun(
        val font: PaintboxFont,
        val text: String,
        val color: Int, // argb8888
        val scaleX: Float = 1f,
        val scaleY: Float = 1f,
        val offsetXEm: Float = 0f,
        val offsetYEm: Float = 0f,
        val carryOverOffsetX: Boolean = true,
        val carryOverOffsetY: Boolean = false,
        val xAdvanceEm: Float = 0f,
                  ) {
    
    constructor(font: PaintboxFont, text: String, color: Color = Color.WHITE,
                scaleX: Float = 1f, scaleY: Float = 1f,  offsetXEm: Float = 0f, offsetYEm: Float = 0f, 
                carryOverOffsetX: Boolean = true, carryOverOffsetY: Boolean = false, xAdvanceEm: Float = 0f,)
            : this(font, text, Color.argb8888(color), scaleX, scaleY,
                   offsetXEm, offsetYEm, carryOverOffsetX, carryOverOffsetY, xAdvanceEm)
    
    fun toTextBlock(): TextBlock = TextBlock(listOf(this))
}
