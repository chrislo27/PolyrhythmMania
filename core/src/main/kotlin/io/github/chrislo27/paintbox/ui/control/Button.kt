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
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.skin.DefaultSkins
import io.github.chrislo27.paintbox.ui.skin.Skin
import io.github.chrislo27.paintbox.ui.skin.SkinFactory
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import java.util.*
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
    
    init {
        this.padding.set(Insets(2f))
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

    val roundedRadius: Var<Int> = Var(2)

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

    val roundedCorners: EnumSet<Corner> = EnumSet.allOf(Corner::class.java)

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
        val paddingBounds = element.paddingZone
        val rectX = paddingBounds.x.getOrCompute() + originX
        val rectY = originY - paddingBounds.y.getOrCompute()
        val rectW = paddingBounds.width.getOrCompute()
        val rectH = paddingBounds.height.getOrCompute()
        val lastPackedColor = batch.packedColor
        val opacity = element.apparentOpacity.getOrCompute()

        val rectColor: Color = ColorStack.getAndPush()
        rectColor.set(bgColorToUse.getOrCompute())
        rectColor.a *= opacity
        batch.color = rectColor
        var roundedRad = roundedRadius.getOrCompute()
        val paintboxSpritesheet = PaintboxGame.paintboxSpritesheet
        val roundedRect: TextureRegion = paintboxSpritesheet.roundedCorner
        val spritesheetFill: TextureRegion = paintboxSpritesheet.fill
        if (roundedRad > rectW / 2f) {
            roundedRad = (rectW / 2f).toInt()
        }
        if (roundedRad > rectH / 2f) {
            roundedRad = (rectH / 2f).toInt()
        }
        if (roundedRad <= 0) {
            batch.fillRect(rectX, rectY - rectH, rectW, rectH)
        } else {
            batch.fillRect(rectX + roundedRad, rectY - rectH + roundedRad, rectW - roundedRad * 2, rectH - roundedRad * 2)
            batch.fillRect(rectX, rectY - rectH + roundedRad, (roundedRad).toFloat(), rectH - roundedRad * 2)
            batch.fillRect(rectX + rectW - roundedRad, rectY - rectH + roundedRad, (roundedRad).toFloat(), rectH - roundedRad * 2)
            batch.fillRect(rectX + roundedRad, rectY - rectH, rectW - roundedRad * 2, (roundedRad).toFloat())
            batch.fillRect(rectX + roundedRad, rectY - roundedRad, rectW - roundedRad * 2, (roundedRad).toFloat())
            val roundedCornersSet = roundedCorners
            batch.draw(if (Corner.TOP_LEFT in roundedCornersSet) roundedRect else spritesheetFill,
                    rectX, rectY - roundedRad, (roundedRad).toFloat(), (roundedRad).toFloat()) // TL
            batch.draw(if (Corner.BOTTOM_LEFT in roundedCornersSet) roundedRect else spritesheetFill,
                    rectX, rectY - rectH + roundedRad, (roundedRad).toFloat(), (-roundedRad).toFloat()) // BL
            batch.draw(if (Corner.TOP_RIGHT in roundedCornersSet) roundedRect else spritesheetFill,
                    rectX + rectW, rectY - roundedRad, (-roundedRad).toFloat(), (roundedRad).toFloat()) // TR
            batch.draw(if (Corner.BOTTOM_RIGHT in roundedCornersSet) roundedRect else spritesheetFill,
                    rectX + rectW, rectY - rectH + roundedRad, (-roundedRad).toFloat(), (-roundedRad).toFloat()) // BR
        }
        batch.packedColor = lastPackedColor
        ColorStack.pop()

        val text = element.internalTextBlock.getOrCompute()
        if (text.runs.isNotEmpty()) {
            val textBounds = element.contentZone
            val textX = textBounds.x.getOrCompute() + originX
            val textY = originY - textBounds.y.getOrCompute()
            val textW = textBounds.width.getOrCompute()
            val textH = textBounds.height.getOrCompute()
            
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
                Align.isRight(align) -> (textW - (if (compressX) min(text.width, textW) else text.width))
                else -> (textW - (if (compressX) min(text.width, textW) else text.width)) / 2f
            }
            val yOffset: Float = when {
                Align.isTop(align) -> textH - text.firstCapHeight
                Align.isBottom(align) -> 0f + (text.height - text.firstCapHeight)
                else -> textH / 2 + text.height / 2 - text.firstCapHeight
            }

            batch.color = tmpColor // Sets the text colour and opacity
            text.drawCompressed(batch, textX + xOffset, textY - textH + yOffset, if (compressX) textW else 0f, element.textAlign.getOrCompute())
            ColorStack.pop()
        }

        batch.packedColor = lastPackedColor
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        // NO-OP
    }

    enum class Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

}