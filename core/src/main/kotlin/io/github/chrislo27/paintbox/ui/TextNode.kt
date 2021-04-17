package io.github.chrislo27.paintbox.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.font.PaintboxFont
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.font.TextBlock
import io.github.chrislo27.paintbox.font.TextRun
import io.github.chrislo27.paintbox.util.Var
import kotlin.math.min


open class TextNode(textBlock: TextBlock = TextBlock(emptyList())) : UIElement() {

    val textColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val textBlock: Var<TextBlock> = Var(textBlock)
    val renderAlign: Var<Int> = Var(Align.left)
    val textAlign: Var<TextAlign> = Var { TextAlign.fromInt(renderAlign.use()) }
    val doXCompression: Var<Boolean> = Var(true)

    constructor(font: PaintboxFont, text: String, color: Color = Color.WHITE, scaleX: Float = 1f, scaleY: Float = 1f)
            : this(TextBlock(listOf(TextRun(font, text, color, scaleX, scaleY))))

    constructor(font: PaintboxFont, text: String, color: Color = Color.WHITE, scale: Float = 1f)
            : this(TextBlock(listOf(TextRun(font, text, color, scale, scale))))

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val text = textBlock.getOrCompute()
        if (text.runs.isEmpty()) return

        val x = bounds.x.getOrCompute() + originX
        val y = originY - bounds.y.getOrCompute()
        val w = bounds.width.getOrCompute()
        val h = bounds.height.getOrCompute()
        val lastPackedColor = batch.packedColor
        val opacity = opacity.getOrCompute()
        val tmpColor = ColorStack.getAndPush()
        tmpColor.set(batch.color).mul(textColor.getOrCompute())
        tmpColor.a *= opacity

        if (text.isRunInfoInvalid()) {
            // Prevents flickering when drawing on first frame due to bounds not being computed yet
            text.computeLayouts()
        }

        val compressX = doXCompression.getOrCompute()
        val align = renderAlign.getOrCompute()
        val xOffset: Float = when {
            Align.isLeft(align) -> 0f
            Align.isRight(align) -> (w - (if (compressX) min(text.width, w)  else text.width))
            else -> (w - (if (compressX) min(text.width, w) else text.width)) / 2f
        }
        val yOffset: Float = when {
            Align.isTop(align) -> h - text.firstCapHeight
            Align.isBottom(align) -> 0f + (text.height - text.firstCapHeight)
            else -> h / 2 + text.height / 2 - text.firstCapHeight
        }

        batch.color = tmpColor // Sets the opacity
        text.drawCompressed(batch, x + xOffset, y - h + yOffset, if (compressX) w else 0f, textAlign.getOrCompute())
        ColorStack.pop()
        
        batch.packedColor = lastPackedColor
    }
}