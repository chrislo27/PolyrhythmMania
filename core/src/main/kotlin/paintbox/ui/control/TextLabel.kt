package paintbox.ui.control

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import paintbox.PaintboxGame
import paintbox.binding.FloatVar
import paintbox.util.ColorStack
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.Skin
import paintbox.ui.skin.SkinFactory
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.*
import paintbox.ui.area.Insets
import paintbox.util.gdxutils.fillRect
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
                val markup: Markup? = label.markup.use()
                if (markup != null) {
                    markup.parse(label.text.use())
                } else {
                    TextRun(label.font.use(), label.text.use(), Color.WHITE,
                            /*label.scaleX.use(), label.scaleY.use()*/ 1f, 1f).toTextBlock()
                }.also { textBlock ->
                    if (label.doLineWrapping.use()) {
                        textBlock.lineWrapping = label.contentZone.width.use()
                    }
                }
            }
        }
    }

    val text: Var<String> = Var(text)
    val font: Var<PaintboxFont> = Var(font)

    /**
     * The [Markup] object to use. If null, no markup parsing is done. If not null,
     * then the markup determines the TextBlock (and other values like [textColor] are ignored).
     */
    val markup: Var<Markup?> = Var(null)

    /**
     * If the alpha value is 0, the skin controls what text colour is used.
     */
    val textColor: Var<Color> = Var(Color(0f, 0f, 0f, 0f))

    /**
     * Determines the x-scale the text is rendered at.
     */
    val scaleX: FloatVar = FloatVar(1f)
    /**
     * Determines the y-scale the text is rendered at.
     */
    val scaleY: FloatVar = FloatVar(1f)

    /**
     * If the alpha value is 0, the skin controls what background colour is used.
     */
    val backgroundColor: Var<Color> = Var(Color(1f, 1f, 1f, 0f))

    val renderBackground: Var<Boolean> = Var(false)
    val bgPadding: Var<Insets> = Var.bind { padding.use() }

    val renderAlign: Var<Int> = Var(Align.left)
    val textAlign: Var<TextAlign> = Var { TextAlign.fromInt(renderAlign.use()) }
    val doXCompression: Var<Boolean> = Var(true)
    val doLineWrapping: Var<Boolean> = Var(false)

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

        val bgPaddingInsets = element.bgPadding.getOrCompute()
        val textPaddingOffsetX: Float = bgPaddingInsets.left
        val textPaddingOffsetY: Float = bgPaddingInsets.top
        val compressX = element.doXCompression.getOrCompute()
        val align = element.renderAlign.getOrCompute()
        val scaleX = element.scaleX.getOrCompute()
        val scaleY = element.scaleY.getOrCompute()
        val textWidth = text.width * scaleX
        val textHeight = text.height * scaleY
        val xOffset: Float = when {
            Align.isLeft(align) -> 0f + textPaddingOffsetX
            Align.isRight(align) -> (w - (if (compressX) (min(textWidth, w)) else textWidth) - textPaddingOffsetX)
            else -> (w - (if (compressX) min(textWidth, w) else textWidth)) / 2f
        }
        val firstCapHeight = text.firstCapHeight * scaleY
        val yOffset: Float = when {
            Align.isTop(align) -> h - firstCapHeight - textPaddingOffsetY
            Align.isBottom(align) -> 0f + (textHeight - firstCapHeight) + textPaddingOffsetY
            else -> (h + textHeight) / 2 - firstCapHeight
        }

        if (element.renderBackground.getOrCompute()) {
            // Draw a rectangle behind the text, only sizing to the text area.
            val bx = (x + xOffset) - bgPaddingInsets.left
            val by = (y - h + yOffset - textHeight + firstCapHeight) - bgPaddingInsets.top
            val bw = (if (compressX) min(w, textWidth) else textWidth) + bgPaddingInsets.left + bgPaddingInsets.right
            val bh = textHeight + bgPaddingInsets.top + bgPaddingInsets.bottom

            val bgColor = ColorStack.getAndPush().set(bgColorToUse.getOrCompute())
            bgColor.a *= opacity
            batch.color = bgColor
            batch.fillRect(bx.coerceAtLeast(x), by/*.coerceAtLeast(y - bh.coerceAtMost(h))*/,
                    if (compressX) bw.coerceAtMost(w) else bw, bh/*.coerceAtMost(h)*/)
            ColorStack.pop()
        }

        batch.color = tmpColor // Sets the opacity
        text.drawCompressed(batch, x + xOffset, (y - h + yOffset),
                if (compressX) (w - textPaddingOffsetX * 2f) else 0f, element.textAlign.getOrCompute(),
                scaleX, scaleY)
        ColorStack.pop()

        batch.packedColor = lastPackedColor
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        // NO-OP
    }

}
