package io.github.chrislo27.paintbox.ui.control

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.font.PaintboxFont
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.font.TextBlock
import io.github.chrislo27.paintbox.font.TextRun
import io.github.chrislo27.paintbox.ui.skin.DefaultSkins
import io.github.chrislo27.paintbox.ui.skin.Skin
import io.github.chrislo27.paintbox.ui.skin.SkinFactory
import io.github.chrislo27.paintbox.util.ReadOnlyVar
import io.github.chrislo27.paintbox.util.Var
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import kotlin.math.min


open class TextLabel(text: String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
    : Control<TextLabel>() {
    
    companion object {
        const val SKIN_ID: String = "TextLabel"
        
        init {
            DefaultSkins.register(SKIN_ID, SkinFactory { element: TextLabel ->
                TextLabelSkin(element)
            })
        }
    }
    
    val text: Var<String> = Var(text)
    val font: Var<PaintboxFont> = Var(font)

    /**
     * If the alpha value is 0, the skin controls what text colour is used.
     */
    val textColor: Var<Color> = Var(Color(0f, 0f, 0f, 0f))
    /**
     * If the alpha value is 0, the skin controls what background colour is used.
     */
    val backgroundColor: Var<Color> = Var(Color(1f, 1f, 1f, 0f))
    
    val renderBackground: Var<Boolean> = Var(false)
    val bgPadding: Var<Float> = Var(5f)
    
    val scaleX: Var<Float> = Var(1f)
    val scaleY: Var<Float> = Var(1f)
    
    val renderAlign: Var<Int> = Var(Align.left)
    val textAlign: Var<TextAlign> = Var { TextAlign.fromInt(renderAlign.use()) }
    val doXCompression: Var<Boolean> = Var(true)

    /**
     * Defaults to an auto-generated [TextBlock] with the given [text].
     * If this is overwritten, this [TextLabel]'s [textColor] should be set to have a non-zero opacity.
     */
    val internalTextBlock: Var<TextBlock> = Var {
        TextRun(this@TextLabel.font.use(), this@TextLabel.text.use(), textColor.use(), scaleX.use(), scaleY.use()).toTextBlock()
    }

    override fun getSkinID(): String = TextLabel.SKIN_ID
    
}

open class TextLabelSkin(element: TextLabel) : Skin<TextLabel>(element) {
    
    val defaultTextColor: Var<Color> = Var(Color(0f, 0f, 0f, 1f))
    val defaultBgColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    
    private val textBlockToUse: ReadOnlyVar<TextBlock> = Var {
        val color = element.textColor.use()
        if (color.a <= 0f) {
            // Use the skin's default colour.
            element.internalTextBlock.use().recolorAll(defaultTextColor.use())
        } else {
            element.internalTextBlock.use()
        }
    }
    private val bgColorToUse: ReadOnlyVar<Color> = Var {
        val color = element.backgroundColor.use()
        if (color.a <= 0f) {
            // Use the skin's default colour.
            defaultBgColor.use()
        } else {
            element.backgroundColor.use()
        }
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val text = textBlockToUse.getOrCompute()
        if (text.runs.isEmpty()) return

        val bounds = element.bounds
        val x = bounds.x.getOrCompute() + originX
        val y = originY - bounds.y.getOrCompute()
        val w = bounds.width.getOrCompute()
        val h = bounds.height.getOrCompute()
        val lastPackedColor = batch.packedColor

        if (text.isRunInfoInvalid()) {
            // Prevents flickering when drawing on first frame due to bounds not being computed yet
            text.computeLayouts()
        }

        val bgPadding = element.bgPadding.getOrCompute()
        val textPaddingOffsetX: Float = bgPadding
        val textPaddingOffsetY: Float = bgPadding
        val compressX = element.doXCompression.getOrCompute()
        val align = element.renderAlign.getOrCompute()
        val xOffset: Float = when {
            Align.isLeft(align) -> 0f + textPaddingOffsetX
            Align.isRight(align) -> (w - (if (compressX) (min(text.width, w)) else (text.width)) + textPaddingOffsetX)
            else -> (w - (if (compressX) min(text.width, w) else text.width)) / 2f + textPaddingOffsetX
        }
        val yOffset: Float = when {
            Align.isTop(align) -> h - text.firstCapHeight - textPaddingOffsetY
            Align.isBottom(align) -> 0f + (text.height - text.firstCapHeight) + textPaddingOffsetY
            else -> (h + text.height) / 2 - text.firstCapHeight
        }
        
        if (element.renderBackground.getOrCompute()) {
            // Draw a rectangle behind the text, only sizing to the text area.
            val bx = (x + xOffset) - bgPadding
            val by = (y - h + yOffset - text.height + text.firstCapHeight) - bgPadding
            val bw = (if (compressX) min(w, text.width) else text.width) + bgPadding * 2
            val bh = (text.height) + bgPadding * 2

            val bgColor = bgColorToUse.getOrCompute()
            batch.color = bgColor
            batch.fillRect(bx.coerceAtLeast(x), by/*.coerceAtLeast(y - bh.coerceAtMost(h))*/,
                           if (compressX) bw.coerceAtMost(w) else bw, bh/*.coerceAtMost(h)*/)
        }

        text.drawCompressed(batch, x + xOffset, y - h + yOffset,
                            if (compressX) (w - textPaddingOffsetX * 2f) else 0f, element.textAlign.getOrCompute())

        batch.packedColor = lastPackedColor
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        // NO-OP
    }
    
}
