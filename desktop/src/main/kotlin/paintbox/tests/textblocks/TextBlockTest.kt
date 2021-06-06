package paintbox.tests.textblocks

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.PaintboxGame
import paintbox.PaintboxScreen
import paintbox.PaintboxSettings
import paintbox.ResizeAction
import paintbox.font.TextAlign
import paintbox.font.TextBlock
import paintbox.font.TextRun
import paintbox.logging.Logger
import paintbox.util.Version
import paintbox.util.WindowSize
import paintbox.util.gdxutils.drawRect
import paintbox.util.gdxutils.fillRect
import java.io.File


internal class TextBlockTestGame(paintboxSettings: PaintboxSettings)
    : PaintboxGame(paintboxSettings) {

    override fun getTitle(): String {
        return "Text block test"
    }

    override fun create() {
        super.create()
        this.setScreen(TextBlockTestScreen(this))
    }
}

internal class TextBlockTestScreen(override val main: TextBlockTestGame) : PaintboxScreen() {
    
    var textBlock: TextBlock = generateTextBlock()
    
    fun generateTextBlock(): TextBlock {
        return TextBlock(
                listOf(
                        TextRun(main.debugFont, "Test "),
                        TextRun(main.debugFontItalic, "italicized "),
                        TextRun(main.debugFont, "blocky! one newline\n"),
                        
                        TextRun(main.debugFont, "Firebrick line 2, 2 newlines\n\n", Color.FIREBRICK),
                        
                        TextRun(main.debugFont, "Big run ", scaleX = 2f, scaleY = 2f),
                        TextRun(main.debugFontItalic, "italicized ", scaleX = 2f, scaleY = 2f),
                        TextRun(main.debugFont, "blocky? one newline\n", scaleX = 2f, scaleY = 2f),
                        
                        TextRun(main.debugFont, "Back to normal "),
                        TextRun(main.debugFontItalic, "subscript ", scaleX = 0.58f, scaleY = 0.58f, offsetYEm = -0.333f),
                        TextRun(main.debugFont, "not anymore... 3 newlines\n\n\nStart is on same TextRun"),
                        
                        TextRun(main.debugFontItalic, "superscript ", offsetYEm = 1.333f),
                        TextRun(main.debugFont, "and not. 1 newline\n"),
                        
                        TextRun(main.debugFont, "Let's carry over the y offset "),
                        TextRun(main.debugFontItalic, "superscript ", offsetYEm = 1.333f, carryOverOffsetY = true),
                        TextRun(main.debugFont, "what happens now? 1 nl\n"),
                        
                        TextRun(main.debugFontItalic, "And normal italicized font again. 1 nl\n"),
                        
                        TextRun(main.debugFont, "The quick brown fox jumps over the lazy dog. 1 nl\n"),
                        TextRun(main.debugFont, "The quick brown fox jumps over the lazy dog. 2 nl\n\n"),
                        TextRun(main.debugFont, "     The quick brown fox jumps over the lazy dog. 1 nl\n"),
                        TextRun(main.debugFont, "The quick brown fox jumps over the lazy dog. 3 nl\n\n\n"),
                        TextRun(main.debugFont, "The quick brown fox jumps over the lazy dog. 1 nl\n"),
                        TextRun(main.debugFont, "The quick brown fox jumps over the lazy dog."),
                      )/*.map { if (it.color == Color.argb8888(Color.WHITE)) it.copy(color = Color.argb8888(Color.BLACK)) else it }*/,
                        )
    }
    
    private var textAlign = TextAlign.LEFT
    
    override fun render(delta: Float) {
        val batch = main.batch
        batch.begin()
        batch.setColor(0.1f, 0.1f, 0.1f, 1f)
        batch.fillRect(0f, 0f, Gdx.graphics.width + 0f, Gdx.graphics.height + 0f)

        batch.setColor(1f, 1f, 1f, 1f)
        val textBlock = textBlock
        val startX = 100f
        val startY = Gdx.graphics.height - 100f
        
        textBlock.drawCompressed(batch, startX, startY, textBlock.width, textAlign)
        
        batch.setColor(0f, 1f, 0f, 1f)
        batch.drawRect(startX, startY - textBlock.height, textBlock.width, textBlock.height, 1f)
        batch.setColor(1f, 1f, 0f, 1f)
        batch.drawRect(startX, startY - textBlock.height + textBlock.firstCapHeight + textBlock.lastDescent, textBlock.width, textBlock.height - textBlock.lastDescent, 1f)
        
        
        batch.end()
        super.render(delta)
    }

    override fun keyTyped(character: Char): Boolean {
        when (character) {
            'r' -> {
                textBlock = generateTextBlock()
                return true
            }
            '1' -> {
                textAlign = TextAlign.LEFT
                return true
            }
            '2' -> {
                textAlign = TextAlign.CENTRE
                return true
            }
            '3' -> {
                textAlign = TextAlign.RIGHT
                return true
            }
        }
        return false
    }

    override fun dispose() {
    }
}