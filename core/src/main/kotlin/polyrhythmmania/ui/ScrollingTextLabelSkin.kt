package polyrhythmmania.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import paintbox.binding.FloatVar
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.control.TextLabelSkin
import paintbox.util.ColorStack
import paintbox.util.gdxutils.fillRect
import kotlin.math.min


/**
 * A text label that scrolls and wraps its text like a music player would.
 * [doClipping][paintbox.ui.UIElement.doClipping] should be enabled for the best effect
 */
class ScrollingTextLabelSkin(element: TextLabel) : TextLabelSkin(element) {
    
    val scrollRate: FloatVar = FloatVar(64f)
    val wrapAroundPauseSec: FloatVar = FloatVar(2f)
    val gapBetween: FloatVar = FloatVar { scrollRate.useF() }

    /**
     * The offset is positive.
     * It wraps around to 0 when the value is greater than or equal to
     * the [text block][TextLabel.internalTextBlock] width plus the content zone width.
     */
    private var currentScrollOffset: Float = 0f
    private var pauseTimer: Float = 0f
    
    init {
        element.internalTextBlock.addListener {
            reset()
        }
    }
    
    private fun reset() {
        currentScrollOffset = 0f
        pauseTimer = wrapAroundPauseSec.get()
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val text = element.internalTextBlock.getOrCompute()
        if (text.runs.isEmpty()) {
            return
        }

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
            reset()
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
        val deltaTime = Gdx.graphics.deltaTime
        val textWrapPoint = text.width + gapBetween.get()
        val scrollOffset: Float = if (textWrapPoint < w) 0f else if (pauseTimer > 0f) {
            pauseTimer = (pauseTimer - deltaTime).coerceAtLeast(0f)
            0f
        } else {
            val oldScrollOffset = this.currentScrollOffset
            val newScrollOffset = oldScrollOffset + this.scrollRate.get() * deltaTime
            this.currentScrollOffset = if (oldScrollOffset < textWrapPoint && newScrollOffset >= textWrapPoint) {
                pauseTimer = wrapAroundPauseSec.get()
                0f
            } else {
                newScrollOffset
            }
            -(this.currentScrollOffset)
        }
        if (compressX) {
            val maxTextWidth = w - bgPaddingInsets.left - bgPaddingInsets.right
            text.drawCompressed(batch, x + (xOffset).coerceAtLeast(bgPaddingInsets.left) + scrollOffset, (y - h + yOffset),
                    maxTextWidth,
                    element.textAlign.getOrCompute(), scaleX, scaleY)
            if (scrollOffset < 0f) {
                text.drawCompressed(batch, x + (xOffset).coerceAtLeast(bgPaddingInsets.left) + scrollOffset + textWrapPoint, (y - h + yOffset),
                        maxTextWidth,
                        element.textAlign.getOrCompute(), scaleX, scaleY)
            }
        } else {
            text.draw(batch, x + xOffset + scrollOffset, (y - h + yOffset), element.textAlign.getOrCompute(), scaleX, scaleY)
            if (scrollOffset < 0f) {
                text.draw(batch, x + xOffset + scrollOffset + textWrapPoint, (y - h + yOffset), element.textAlign.getOrCompute(), scaleX, scaleY)
            }
        }
        ColorStack.pop()

        batch.packedColor = lastPackedColor
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        // NO-OP
    }

}