package io.github.chrislo27.paintbox.ui.control

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.font.PaintboxFont
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.font.TextBlock
import io.github.chrislo27.paintbox.font.TextRun
import io.github.chrislo27.paintbox.ui.ColorStack
import io.github.chrislo27.paintbox.ui.skin.DefaultSkins
import io.github.chrislo27.paintbox.ui.skin.Skin
import io.github.chrislo27.paintbox.ui.skin.SkinFactory
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import kotlin.math.min


/**
 * A [TextLabel] is a [Control] that renders a [TextBlock]
 */
open class TextLabel(text: String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
    : Control<TextLabel>() {

    companion object {
        const val SKIN_ID: String = "TextLabel"

        init {
            DefaultSkins.register(SKIN_ID, SkinFactory { element: TextLabel ->
                TextLabelSkin(element)
            })
        }

        fun createInternalTextBlockVar(label: TextLabel): Var<TextBlock> {
            return Var {
                TextRun(label.font.use(), label.text.use(), Color.WHITE,
                        label.scaleX.use(), label.scaleY.use()).toTextBlock()
            }
        }
    }

    val text: Var<String> = Var(text)
    val font: Var<PaintboxFont> = Var(font)

    /**
     * If the alpha value is 0, the skin controls what text colour is used.
     */
    val textColor: Var<Color> = Var(Color(0f, 0f, 0f, 0f))

    val scaleX: FloatVar = FloatVar(1f)
    val scaleY: FloatVar = FloatVar(1f)

    /**
     * If the alpha value is 0, the skin controls what background colour is used.
     */
    val backgroundColor: Var<Color> = Var(Color(1f, 1f, 1f, 0f))

    val renderBackground: Var<Boolean> = Var(false)
    val bgPadding: FloatVar = FloatVar(0f)

    val renderAlign: Var<Int> = Var(Align.left)
    val textAlign: Var<TextAlign> = Var { TextAlign.fromInt(renderAlign.use()) }
    val doXCompression: Var<Boolean> = Var(true)

    /**
     * Defaults to an auto-generated [TextBlock] with the given [text].
     * If this is overwritten, this [TextLabel]'s [textColor] should be set to have a non-zero opacity.
     */
    val internalTextBlock: Var<TextBlock> by lazy { createInternalTextBlockVar(this) }

    constructor(binding: Var.Context.() -> String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
            : this("", font) {
        text.bind(binding)
    }

    fun setScaleXY(scaleXY: Float) {
        this.scaleX.set(scaleXY)
        this.scaleY.set(scaleXY)
    }

    @Suppress("RemoveRedundantQualifierName")
    override fun getDefaultSkinID(): String = TextLabel.SKIN_ID

}

open class TextLabelSkin(element: TextLabel) : Skin<TextLabel>(element) {

    val defaultTextColor: Var<Color> = Var(Color(0f, 0f, 0f, 1f))
    val defaultBgColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))

    private val textColorToUse: ReadOnlyVar<Color> = Var {
        val color = element.textColor.use()
        if (color.a <= 0f) {
            // Use the skin's default colour.
            defaultTextColor.use()
        } else {
            element.textColor.use()
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
        val text = element.internalTextBlock.getOrCompute()
        if (text.runs.isEmpty()) return

        val bounds = element.contentZone
        val x = bounds.x.getOrCompute() + originX
        val y = originY - bounds.y.getOrCompute()
        val w = bounds.width.getOrCompute()
        val h = bounds.height.getOrCompute()
        val lastPackedColor = batch.packedColor
        val opacity = element.apparentOpacity.getOrCompute()
        val tmpColor = ColorStack.getAndPush()
        tmpColor.set(batch.color).mul(textColorToUse.getOrCompute())
        tmpColor.a *= opacity

        if (text.isRunInfoInvalid()) {
            // Prevents flickering when drawing on first frame due to bounds not being computed yet
            text.computeLayouts()
        }

        val bgPadding = element.bgPadding.getOrCompute()
        val textPaddingOffsetX: Float = bgPadding
        val textPaddingOffsetY: Float = bgPadding
        val compressX = element.doXCompression.getOrCompute()
        val align = element.renderAlign.getOrCompute()
        val textWidth = text.width
        val textHeight = text.height
        val xOffset: Float = when {
            Align.isLeft(align) -> 0f + textPaddingOffsetX
            Align.isRight(align) -> (w - (if (compressX) (min(textWidth, w)) else textWidth) - textPaddingOffsetX)
            else -> (w - (if (compressX) min(textWidth, w) else textWidth)) / 2f
        }
        val yOffset: Float = when {
            Align.isTop(align) -> h - text.firstCapHeight - textPaddingOffsetY
            Align.isBottom(align) -> 0f + (textHeight - text.firstCapHeight) + textPaddingOffsetY
            else -> (h + textHeight) / 2 - text.firstCapHeight
        }

        if (element.renderBackground.getOrCompute()) {
            // Draw a rectangle behind the text, only sizing to the text area.
            val bx = (x + xOffset) - bgPadding
            val by = (y - h + yOffset - textHeight + text.firstCapHeight) - bgPadding
            val bw = (if (compressX) min(w, textWidth) else textWidth) + bgPadding * 2
            val bh = textHeight + bgPadding * 2

            val bgColor = ColorStack.getAndPush().set(bgColorToUse.getOrCompute())
            bgColor.a *= opacity
            batch.color = bgColor
            batch.fillRect(bx.coerceAtLeast(x), by/*.coerceAtLeast(y - bh.coerceAtMost(h))*/,
                    if (compressX) bw.coerceAtMost(w) else bw, bh/*.coerceAtMost(h)*/)
            ColorStack.pop()
        }

        batch.color = tmpColor // Sets the opacity
        text.drawCompressed(batch, x + xOffset, y - h + yOffset,
                if (compressX) (w - textPaddingOffsetX * 2f) else 0f, element.textAlign.getOrCompute())
        ColorStack.pop()

        batch.packedColor = lastPackedColor
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        // NO-OP
    }

}
