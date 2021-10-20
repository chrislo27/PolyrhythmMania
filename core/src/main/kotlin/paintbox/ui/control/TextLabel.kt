package paintbox.ui.control

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import paintbox.PaintboxGame
import paintbox.binding.BooleanVar
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
        const val TEXTLABEL_SKIN_ID: String = "TextLabel"

        init {
            DefaultSkins.register(TEXTLABEL_SKIN_ID, SkinFactory { element: TextLabel ->
                TextLabelSkin(element)
            })
        }

        fun createInternalTextBlockVar(label: TextLabel): Var<TextBlock> {
            return Var {
                val markup: Markup? = label.markup.use()
                (markup?.parse(label.text.use())
                        ?: TextRun(label.font.use(), label.text.use(), Color.WHITE,
                                /*label.scaleX.use(), label.scaleY.use()*/ 1f, 1f).toTextBlock()).also { textBlock ->
                    if (label.doLineWrapping.useB()) {
                        textBlock.lineWrapping.set(label.contentZone.width.useF() / (if (label.doesScaleXAffectWrapping.useB()) label.scaleX.useF() else 1f))
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
    
    val doesScaleXAffectWrapping: BooleanVar = BooleanVar(true)

    /**
     * If the alpha value is 0, the skin controls what background colour is used.
     */
    val backgroundColor: Var<Color> = Var(Color(1f, 1f, 1f, 0f))

    val renderBackground: BooleanVar = BooleanVar(false)
    val bgPadding: Var<Insets> = Var.bind { padding.use() }

    val renderAlign: Var<Int> = Var(Align.left)
    val textAlign: Var<TextAlign> = Var { TextAlign.fromInt(renderAlign.use()) }
    val doXCompression: BooleanVar = BooleanVar(true)
    val doLineWrapping: BooleanVar = BooleanVar(false)

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
    override fun getDefaultSkinID(): String = TextLabel.TEXTLABEL_SKIN_ID

    /**
     * Resizes this element to fit the bounds of the [internalTextBlock].
     * [limitWidth] and [limitHeight] will strictly limit the width/height if they are greater than zero.
     * 
     */
    fun resizeBoundsToContent(affectWidth: Boolean = true, affectHeight: Boolean = true,
                              limitWidth: Float = 0f, limitHeight: Float = 0f) {
        val textBlock: TextBlock = this.internalTextBlock.getOrCompute()
        if (textBlock.isRunInfoInvalid()) {
            textBlock.computeLayouts()
        }
        if (!affectWidth && !affectHeight) return
        
        val textWidth = textBlock.width * scaleX.get()
        val textHeight = textBlock.height * scaleY.get()
        
        val borderInsets = this.border.getOrCompute()
        val marginInsets = this.margin.getOrCompute()
        val paddingInsets = this.bgPadding.getOrCompute().maximize(this.padding.getOrCompute())
        
        fun Insets.leftright(): Float = this.left + this.right
        fun Insets.topbottom(): Float = this.top + this.bottom

        if (affectWidth) {
            var computedWidth = borderInsets.leftright() + marginInsets.leftright() + paddingInsets.leftright() + textWidth
            if (limitWidth > 0f) {
                computedWidth = computedWidth.coerceAtMost(limitWidth)
            }
            this.bounds.width.set(computedWidth)
        }
        if (affectHeight) {
            var computedHeight = borderInsets.topbottom() + marginInsets.topbottom() + paddingInsets.topbottom() + textHeight
            if (limitHeight > 0f) {
                computedHeight = computedHeight.coerceAtMost(limitHeight)
            }
            this.bounds.height.set(computedHeight)
        }
    }
}

open class TextLabelSkin(element: TextLabel) : Skin<TextLabel>(element) {

    val defaultTextColor: Var<Color> = Var(Color(0f, 0f, 0f, 1f))
    val defaultBgColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))

    protected val textColorToUse: ReadOnlyVar<Color> = Var {
        val color = element.textColor.use()
        if (color.a <= 0f) {
            // Use the skin's default colour.
            defaultTextColor.use()
        } else {
            element.textColor.use()
        }
    }
    protected val bgColorToUse: ReadOnlyVar<Color> = Var {
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
        val x = bounds.x.get() + originX
        val y = originY - bounds.y.get()
        val w = bounds.width.get()
        val h = bounds.height.get()
        val lastPackedColor = batch.packedColor
        val opacity = element.apparentOpacity.get()
        val tmpColor = ColorStack.getAndPush()
        tmpColor.set(batch.color).mul(textColorToUse.getOrCompute())
        tmpColor.a *= opacity

        if (text.isRunInfoInvalid()) {
            // Prevents flickering when drawing on first frame due to bounds not being computed yet
            text.computeLayouts()
        }

        val bgPaddingInsets = if (element.renderBackground.get()) element.bgPadding.getOrCompute() else Insets.ZERO
        val compressX = element.doXCompression.get()
        val align = element.renderAlign.getOrCompute()
        val scaleX = element.scaleX.get()
        val scaleY = element.scaleY.get()
        val textWidth = text.width * scaleX
        val textHeight = text.height * scaleY
        val xOffset: Float = when {
            Align.isLeft(align) -> 0f + bgPaddingInsets.left
            Align.isRight(align) -> (w - ((if (compressX) (min(textWidth, w)) else textWidth) + bgPaddingInsets.right))
            else -> (w - (if (compressX) min(textWidth, w) else textWidth)) / 2f
        }
        val firstCapHeight = text.firstCapHeight * scaleY
        val yOffset: Float = when {
            Align.isTop(align) -> h - firstCapHeight - bgPaddingInsets.top
            Align.isBottom(align) -> 0f + (textHeight - firstCapHeight) + bgPaddingInsets.bottom
            else -> ((h + textHeight) / 2 - firstCapHeight)
        }

        if (element.renderBackground.get()) {
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

        batch.color = tmpColor // Sets the opacity of the text
        if (compressX) {
            val maxTextWidth = w - bgPaddingInsets.left - bgPaddingInsets.right
            text.drawCompressed(batch, x + (xOffset).coerceAtLeast(bgPaddingInsets.left), (y - h + yOffset),
                    maxTextWidth,
                    element.textAlign.getOrCompute(), scaleX, scaleY)
        } else {
            text.draw(batch, x + xOffset, (y - h + yOffset), element.textAlign.getOrCompute(), scaleX, scaleY)
        }
        ColorStack.pop()

        batch.packedColor = lastPackedColor
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        // NO-OP
    }

}
