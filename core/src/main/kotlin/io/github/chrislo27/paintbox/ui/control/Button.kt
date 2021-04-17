package io.github.chrislo27.paintbox.ui.control

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.font.PaintboxFont
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.font.TextBlock
import io.github.chrislo27.paintbox.font.TextRun
import io.github.chrislo27.paintbox.ui.*
import io.github.chrislo27.paintbox.ui.skin.DefaultSkins
import io.github.chrislo27.paintbox.ui.skin.Skin
import io.github.chrislo27.paintbox.ui.skin.SkinFactory
import io.github.chrislo27.paintbox.util.ReadOnlyVar
import io.github.chrislo27.paintbox.util.Var
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import io.github.chrislo27.paintbox.util.gdxutils.fillRoundedRect
import kotlin.math.min


open class Button(text: String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
    : Control<Button>() {

    companion object {
        const val SKIN_ID: String = "Button"

        init {
            DefaultSkins.register(SKIN_ID, SkinFactory { element: Button ->
                ButtonSkin(element)
            })
        }

        fun createInternalTextBlockVar(button: Button): Var<TextBlock> {
            return Var {
                TextRun(button.font.use(), button.text.use(), Color.WHITE,
                        button.scaleX.use(), button.scaleY.use()).toTextBlock()
            }
        }
    }

    val text: Var<String> = Var(text)
    val font: Var<PaintboxFont> = Var(font)
    val scaleX: Var<Float> = Var(1f)
    val scaleY: Var<Float> = Var(1f)

    val renderAlign: Var<Int> = Var(Align.center)
    val textAlign: Var<TextAlign> = Var { TextAlign.fromInt(renderAlign.use()) }
    val doXCompression: Var<Boolean> = Var(true)

    /**
     * Defaults to an auto-generated [TextBlock] with the given [text].
     */
    val internalTextBlock: Var<TextBlock> by lazy { createInternalTextBlockVar(this) }

    val isHoveredOver: ReadOnlyVar<Boolean> = Var(false)
    val isPressedDown: ReadOnlyVar<Boolean> = Var(false)
    val pressedState: ReadOnlyVar<PressedState> = Var {
        val hovered = isHoveredOver.use()
        val pressed = isPressedDown.use()
        if (hovered && pressed) {
            PressedState.PRESSED_AND_HOVERED
        } else if (hovered) {
            PressedState.HOVERED
        } else if (pressed) {
            PressedState.PRESSED
        } else {
            PressedState.NONE
        }
    }

    @Suppress("RemoveRedundantQualifierName")
    override fun getSkinID(): String = Button.SKIN_ID

    override fun defaultInputEventListener(event: InputEvent): Boolean {
        return if (!apparentDisabledState.getOrCompute()) {
            when (event) {
                is ClickPressed -> {
                    if (event.button == Input.Buttons.LEFT) {
                        (isPressedDown as Var).set(true)
                    }
                    false
                }
                is ClickReleased -> {
                    if (event.button == Input.Buttons.LEFT && isPressedDown.getOrCompute()) {
                        (isPressedDown as Var).set(false)
                    }
                    if (event.isWithinBounds) {
                        if (event.button == Input.Buttons.LEFT) {
                            if (!onAction()) {
                                onLeftClick(event)
                            } else true
                        } else if (event.button == Input.Buttons.RIGHT) {
                            onRightClick(event)
                        } else if (event.button == Input.Buttons.MIDDLE) {
                            onMiddleClick(event)
                        } else false
                    } else false
                }
                is MouseEntered -> {
                    (isHoveredOver as Var).set(true)
                    onHoverStart(event)
                }
                is MouseExited -> {
                    (isHoveredOver as Var).set(false)
                    onHoverEnd(event)
                }
                else -> false
            }
        } else {
            false
        }
    }

    enum class PressedState {
        NONE,
        HOVERED,
        PRESSED,
        PRESSED_AND_HOVERED;
    }
}

open class ButtonSkin(element: Button) : Skin<Button>(element) {

    val roundedRadius: Var<Int> = Var(5)
    
    val defaultTextColor: Var<Color> = Var(Color(0f, 0f, 0f, 1f))
    val defaultBgColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val hoveredTextColor: Var<Color> = Var(Color(0f, 0f, 0f, 1f))
    val hoveredBgColor: Var<Color> = Var(Color(0.95f, 0.95f, 0.95f, 1f))
    val pressedTextColor: Var<Color> = Var(Color(0f, 0f, 0f, 1f))
    val pressedBgColor: Var<Color> = Var(Color(0.75f, 0.95f, 0.95f, 1f))
    val pressedAndHoveredTextColor: Var<Color> = Var(Color(0f, 0f, 0f, 1f))
    val pressedAndHoveredBgColor: Var<Color> = Var(Color(0.75f, 1f, 1f, 1f))
    val disabledTextColor: Var<Color> = Var(Color(0.5f, 0.5f, 0.5f, 1f))
    val disabledBgColor: Var<Color> = Var(Color(0.8f, 0.8f, 0.8f, 1f))

    private val textColorToUse: ReadOnlyVar<Color> = Var {
        val pressedState = element.pressedState.use()
        if (element.apparentDisabledState.use()) {
            disabledTextColor.use()
        } else {
            when (pressedState) {
                Button.PressedState.NONE -> defaultTextColor.use()
                Button.PressedState.HOVERED -> hoveredTextColor.use()
                Button.PressedState.PRESSED -> pressedTextColor.use()
                Button.PressedState.PRESSED_AND_HOVERED -> pressedAndHoveredTextColor.use()
            }
        }
    }
    private val bgColorToUse: ReadOnlyVar<Color> = Var {
        val pressedState = element.pressedState.use()
        if (element.apparentDisabledState.use()) {
            disabledBgColor.use()
        } else {
            when (pressedState) {
                Button.PressedState.NONE -> defaultBgColor.use()
                Button.PressedState.HOVERED -> hoveredBgColor.use()
                Button.PressedState.PRESSED -> pressedBgColor.use()
                Button.PressedState.PRESSED_AND_HOVERED -> pressedAndHoveredBgColor.use()
            }
        }
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val bounds = element.bounds
        val x = bounds.x.getOrCompute() + originX
        val y = originY - bounds.y.getOrCompute()
        val w = bounds.width.getOrCompute()
        val h = bounds.height.getOrCompute()
        val lastPackedColor = batch.packedColor
        val opacity = element.apparentOpacity.getOrCompute()

        val rectColor: Color = ColorStack.getAndPush()
        rectColor.set(bgColorToUse.getOrCompute())
        rectColor.a *= opacity
        batch.color = rectColor
        var roundedRad = roundedRadius.getOrCompute()
//        val roundedRect: TextureRegion = PaintboxGame.paintboxSpritesheet.roundedCorner
//        if (roundedRad > w / 2f) {
//            roundedRad = (w / 2f).toInt()
//        }
//        if (roundedRad > h / 2f) {
//            roundedRad = (h / 2f).toInt()
//        }
//        if (roundedRad <= 0) {
//            batch.fillRect(x, y - h, w, h)
//        } else {
//            batch.fillRect(x + roundedRad, y - h + roundedRad, w - roundedRad * 2, h - roundedRad * 2)
//            batch.fillRect(x, y - h + roundedRad, (roundedRad).toFloat(), h - roundedRad * 2)
//            batch.fillRect(x + w - roundedRad, y - h + roundedRad, (roundedRad).toFloat(), h - roundedRad * 2)
//            batch.fillRect(x + roundedRad, y - h, w - roundedRad * 2, (roundedRad).toFloat())
//            batch.fillRect(x + roundedRad, y - roundedRad, w - roundedRad * 2, (roundedRad).toFloat())
//            batch.draw(roundedRect, x, y - roundedRad, (roundedRad).toFloat(), (roundedRad).toFloat()) // TL
//            batch.draw(roundedRect, x, y - h + roundedRad, (roundedRad).toFloat(), (-roundedRad).toFloat()) // BL
//            batch.draw(roundedRect, x + w, y - roundedRad, (-roundedRad).toFloat(), (roundedRad).toFloat()) // TR
//            batch.draw(roundedRect, x + w, y - h + roundedRad, (-roundedRad).toFloat(), (-roundedRad).toFloat()) // BR
//        }
        batch.fillRoundedRect(x, y - h, w, h, roundedRad.toFloat())
        batch.packedColor = lastPackedColor
        ColorStack.pop()

        val text = element.internalTextBlock.getOrCompute()
        if (text.runs.isNotEmpty()) {
            val tmpColor = ColorStack.getAndPush()
            tmpColor.set(batch.color).mul(textColorToUse.getOrCompute())
            tmpColor.a *= opacity

            if (text.isRunInfoInvalid()) {
                // Prevents flickering when drawing on first frame due to bounds not being computed yet
                text.computeLayouts()
            }

            val compressX = element.doXCompression.getOrCompute()
            val align = element.renderAlign.getOrCompute()
            val xOffset: Float = when {
                Align.isLeft(align) -> 0f
                Align.isRight(align) -> (w - (if (compressX) min(text.width, w) else text.width))
                else -> (w - (if (compressX) min(text.width, w) else text.width)) / 2f
            }
            val yOffset: Float = when {
                Align.isTop(align) -> h - text.firstCapHeight
                Align.isBottom(align) -> 0f + (text.height - text.firstCapHeight)
                else -> h / 2 + text.height / 2 - text.firstCapHeight
            }

            batch.color = tmpColor // Sets the text colour and opacity
            text.drawCompressed(batch, x + xOffset, y - h + yOffset, if (compressX) w else 0f, element.textAlign.getOrCompute())
            ColorStack.pop()
        }

        batch.packedColor = lastPackedColor
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        // NO-OP
    }

}