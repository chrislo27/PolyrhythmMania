package io.github.chrislo27.paintbox.ui.control

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.ui.*
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.skin.DefaultSkins
import io.github.chrislo27.paintbox.ui.skin.Skin
import io.github.chrislo27.paintbox.ui.skin.SkinFactory
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import java.awt.MouseInfo


/**
 * A [ScrollBar] is a scroll bar intended to be used with a [ScrollPane].
 */
open class ScrollBar(val orientation: Orientation) : Control<ScrollBar>() {
    companion object {
        const val SCROLLBAR_SKIN_ID: String = "ScrollBar"
        const val SCROLLBAR_INC_BUTTON_SKIN_ID: String = "ScrollBar_UnitIncreaseButton"
        const val MIN_DEFAULT: Float = 0f
        const val MAX_DEFAULT: Float = 100f
        const val UNIT_DEFAULT: Float = 1f
        const val BLOCK_DEFAULT: Float = 20f
        const val VISIBLE_AMOUNT_DEFAULT: Float = 15f

        init {
            DefaultSkins.register(ScrollBar.SCROLLBAR_SKIN_ID, SkinFactory { element: ScrollBar ->
                ScrollBarSkin(element)
            })
            DefaultSkins.register(ScrollBar.SCROLLBAR_INC_BUTTON_SKIN_ID, SkinFactory { element: Button ->
                ScrollBarIncrButtonSkin(element)
            })
        }
    }

    enum class Orientation {
        VERTICAL, HORIZONTAL;
    }

    val unitIncrement: FloatVar = FloatVar(UNIT_DEFAULT)
    val blockIncrement: FloatVar = FloatVar(BLOCK_DEFAULT)
    val minimum: FloatVar = FloatVar(MIN_DEFAULT)
    val maximum: FloatVar = FloatVar(MAX_DEFAULT)
    val visibleAmount: FloatVar = FloatVar(VISIBLE_AMOUNT_DEFAULT)
    private val _value: FloatVar = FloatVar(MIN_DEFAULT)
    val value: ReadOnlyVar<Float> = _value
    val thumbPressedState: ReadOnlyVar<PressedState> get() = thumbArea.pressedState

    protected val decreaseButton: UnitIncreaseButton
    protected val increaseButton: UnitIncreaseButton
    protected val thumbArea: ThumbPane

    init {
        decreaseButton = UnitIncreaseButton(this, getArrowButtonTexReg(), this.orientation, false).apply {
            Anchor.TopLeft.configure(this)
            when (orientation) {
                Orientation.VERTICAL -> this.bounds.height.bind { bounds.width.use() }
                Orientation.HORIZONTAL -> this.bounds.width.bind { bounds.height.use() }
            }
            this.skinID.set(ScrollBar.SCROLLBAR_INC_BUTTON_SKIN_ID)
            this.disabled.bind { _value.use() <= minimum.use() }
            this.setOnAction {
                decrement()
            }
        }
        increaseButton = UnitIncreaseButton(this, getArrowButtonTexReg(), this.orientation, true).apply {
            Anchor.BottomRight.configure(this)
            when (orientation) {
                Orientation.VERTICAL -> this.bounds.height.bind { bounds.width.use() }
                Orientation.HORIZONTAL -> this.bounds.width.bind { bounds.height.use() }
            }
            this.skinID.set(ScrollBar.SCROLLBAR_INC_BUTTON_SKIN_ID)
            this.disabled.bind { _value.use() >= maximum.use() }
            this.setOnAction {
                increment()
            }
        }
        // FIXME
        thumbArea = ThumbPane(this).apply {
            Anchor.Centre.configure(this)
            when (orientation) {
                Orientation.VERTICAL -> {
                    this.bindHeightToParent { -(bounds.width.use() * 2) }
                }
                Orientation.HORIZONTAL -> {
                    this.bindWidthToParent { -(bounds.height.use() * 2) }
                }
            }
        }
        this.addChild(decreaseButton)
        this.addChild(increaseButton)
        this.addChild(thumbArea)
    }

    init {
        minimum.addListener {
            setValue(_value.getOrCompute())
        }
        maximum.addListener {
            setValue(_value.getOrCompute())
        }
    }

    fun setValue(value: Float) {
        _value.set(value.coerceIn(minimum.getOrCompute(), maximum.getOrCompute()))
    }

    fun increment() {
        setValue(_value.getOrCompute() + unitIncrement.getOrCompute())
    }

    fun decrement() {
        setValue(_value.getOrCompute() - unitIncrement.getOrCompute())
    }

    fun incrementBlock() {
        setValue(_value.getOrCompute() + blockIncrement.getOrCompute())
    }

    fun decrementBlock() {
        setValue(_value.getOrCompute() - blockIncrement.getOrCompute())
    }

    protected open fun getArrowButtonTexReg(): TextureRegion {
        return PaintboxGame.paintboxSpritesheet.upArrow
    }

    protected fun convertValueToPercentage(v: Float): Float {
        val min = minimum.getOrCompute()
        val max = maximum.getOrCompute()
        return ((v - min) / (max - min)).coerceIn(0f, 1f)
    }

    protected fun convertPercentageToValue(v: Float): Float {
        val min = minimum.getOrCompute()
        val max = maximum.getOrCompute()
        return (v * (max - min) + min).coerceIn(min, max)
    }

    override fun getDefaultSkinID(): String = ScrollBar.SCROLLBAR_SKIN_ID

    class UnitIncreaseButton(val scrollBar: ScrollBar, textureRegion: TextureRegion, val orientation: Orientation, val isIncrease: Boolean)
        : Button("") {
        val imageNode: ImageNode = ImageNode(textureRegion).apply {
            this.rotation.set(when (orientation) {
                Orientation.VERTICAL -> if (isIncrease) 180f else 0f
                Orientation.HORIZONTAL -> if (isIncrease) 270f else 90f
            })
        }

        init {
            this.padding.set(Insets(4f))
            this.addChild(imageNode)
        }
    }

    class ThumbPane(val scrollBar: ScrollBar)
        : Pane() {

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
        private val lastMouseRelativeToRoot = Vector2(0f, 0f)
        private val lastMouseInside: Vector2 = lastMouseRelativeToRoot
        private var pressedOrigin: Float = 0f

        init {
            fun updateMouseInsidePos(x: Float, y: Float) {
                lastMouseInside.set(this.getPosRelativeToRoot(lastMouseRelativeToRoot))
                lastMouseRelativeToRoot.x = x - lastMouseInside.x
                lastMouseRelativeToRoot.y = y - lastMouseInside.y
            }
            this.addInputEventListener { event ->
                when (event) {
                    is ClickPressed -> updateMouseInsidePos(event.x, event.y)
                    is ClickReleased -> updateMouseInsidePos(event.x, event.y)
                    is MouseMoved -> updateMouseInsidePos(event.x, event.y)
                    is MouseEntered -> updateMouseInsidePos(event.x, event.y)
                    is TouchDragged -> updateMouseInsidePos(event.x, event.y)
                }
                val currentThumbPosPercent = getCurrentThumbPosPercentage()
                val currentThumbWidthPercent = getCurrentThumbWidthPercentage()
                val mousePercent = getMousePercentage()
                when (event) {
                    is MouseExited -> {
                        (isHoveredOver as Var).set(false)
                    }
                    is ClickReleased -> {
                        if (event.button == Input.Buttons.LEFT && isPressedDown.getOrCompute()) {
                            (isPressedDown as Var).set(false)
                        }
                    }
                    is MouseMoved -> {
                        if (mousePercent in currentThumbPosPercent..(currentThumbPosPercent + currentThumbWidthPercent)) {
                            (isHoveredOver as Var).set(true)
                        } else {
                            (isHoveredOver as Var).set(false)
                        }
                    }
                }
                if (!scrollBar.apparentDisabledState.getOrCompute()) {
                    when (event) {
                        is ClickPressed -> {
                            if (event.button == Input.Buttons.LEFT) {
                                // Jump a block increment or begin dragging the thumb


                                if (mousePercent in currentThumbPosPercent..(currentThumbPosPercent + currentThumbWidthPercent)) {
                                    // Inside the thumb.
                                    (isPressedDown as Var).set(true)
                                    pressedOrigin = mousePercent - currentThumbPosPercent
                                } else { // Outside the thumb, do block increments.
                                    if (currentThumbPosPercent > mousePercent) {
                                        scrollBar.decrementBlock()
                                    } else {
                                        scrollBar.incrementBlock()
                                    }
                                }
                                true
                            } else false
                        }
                        is TouchDragged -> {
                            if (pressedState.getOrCompute().pressed) {
                                val newValue = scrollBar.convertPercentageToValue(mousePercent - pressedOrigin)
                                scrollBar.setValue(newValue)
                                true
                            } else false
                        }
                        is ClickReleased -> {
                            if (isPressedDown.getOrCompute()) {
                                (isPressedDown as Var).set(false)
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
        }

        /**
         * Returns the mouse percentage based on the orientation. Note that it is relative to the top left
         * of the scroll thumb, which means values greater than 1 may be returned.
         */
        fun getMousePercentage(): Float {
            val orientation = scrollBar.orientation
            val thumbBounds = this.bounds
            val thumbBoundsWidth = thumbBounds.width.getOrCompute()
            val thumbBoundsHeight = thumbBounds.height.getOrCompute()
            val thumbW = (if (orientation == Orientation.VERTICAL)
                (1f)
            else (scrollBar.convertValueToPercentage(scrollBar.visibleAmount.getOrCompute()))) * thumbBoundsWidth
            val thumbH = (if (scrollBar.orientation == Orientation.HORIZONTAL)
                (1f)
            else (scrollBar.convertValueToPercentage(scrollBar.visibleAmount.getOrCompute()))) * thumbBoundsHeight

            return when (orientation) {
                Orientation.HORIZONTAL -> {
                    if (thumbBoundsWidth - thumbW > 0f)
                        (lastMouseInside.x) / (thumbBoundsWidth - thumbW)
                    else 0f
                }
                Orientation.VERTICAL -> {
                    if (thumbBoundsHeight - thumbH > 0f)
                        (lastMouseInside.y) / (thumbBoundsHeight - thumbH)
                    else 0f
                }
            }
        }

        /**
         * Returns the percentage position of the thumb. Note that it is relative to the top left
         * of the scroll thumb, but all values will be in the range 0.0 to 1.0.
         */
        fun getCurrentThumbPosPercentage(): Float {
//            val visibleAmt = scrollBar.visibleAmount.getOrCompute()
            val min = scrollBar.minimum.getOrCompute()
            val max = scrollBar.maximum.getOrCompute()
            return ((scrollBar._value.getOrCompute() - min) / (max - min))
        }

        /**
         * Returns the percentage width/height of the thumb. Note that it is relative to the top left
         * of the scroll thumb, but all values will be in the range 0.0 to 1.0.
         */
        fun getCurrentThumbWidthPercentage(): Float {
            val visibleAmt = scrollBar.visibleAmount.getOrCompute()
            val min = scrollBar.minimum.getOrCompute()
            val max = scrollBar.maximum.getOrCompute()
            return ((visibleAmt - min) / (max - min - visibleAmt))
        }
    }

    open class ScrollBarSkin(element: ScrollBar) : Skin<ScrollBar>(element) {

        val bgColor: Var<Color> = Var(Color(0.94f, 0.94f, 0.94f, 1f))
        val incrementColor: Var<Color> = Var(Color(0.31f, 0.31f, 0.31f, 1f))
        val disabledColor: Var<Color> = Var(Color(0.64f, 0.64f, 0.64f, 1f))
        val thumbColor: Var<Color> = Var(Color(0.76f, 0.76f, 0.76f, 1f))
        val thumbHoveredColor: Var<Color> = Var(Color(0.70f, 0.70f, 0.70f, 1f))
        val thumbPressedColor: Var<Color> = Var(Color(0.70f, 0.76f, 0.76f, 1f))

        init {
            // Apply certain properties to the ScrollBar's elements like the increment buttons
            listOf(element.increaseButton, element.decreaseButton).forEach { b ->
                b.imageNode.tint.bind {
                    if (b.apparentDisabledState.use()) disabledColor.use() else incrementColor.use()
                }
            }
        }

        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
            val contentBounds = element.contentZone
            val rectX = contentBounds.x.getOrCompute() + originX
            val rectY = originY - contentBounds.y.getOrCompute()
            val rectW = contentBounds.width.getOrCompute()
            val rectH = contentBounds.height.getOrCompute()
            val lastPackedColor = batch.packedColor
            val opacity = element.apparentOpacity.getOrCompute()
            val tmpColor = ColorStack.getAndPush()

            tmpColor.set(bgColor.getOrCompute())
            tmpColor.a *= opacity

            batch.color = tmpColor
            batch.fillRect(rectX, rectY - rectH, rectW, rectH)

            val pressedState = element.thumbArea.pressedState.getOrCompute()
            tmpColor.set((if (element.apparentDisabledState.getOrCompute()) disabledColor
            else if (pressedState.pressed) thumbPressedColor
            else if (pressedState.hovered) thumbHoveredColor
            else thumbColor).getOrCompute())
            tmpColor.a *= opacity
            batch.color = tmpColor
            val thumb = element.thumbArea
            val thumbBounds = thumb.bounds
            val thumbW = (if (element.orientation == Orientation.VERTICAL)
                (1f)
            else (element.convertValueToPercentage(element.visibleAmount.getOrCompute()))) * thumbBounds.width.getOrCompute()
            val thumbH = (if (element.orientation == Orientation.HORIZONTAL)
                (1f)
            else (element.convertValueToPercentage(element.visibleAmount.getOrCompute()))) * thumbBounds.height.getOrCompute()
            val currentValue = element._value.getOrCompute()
            val scrollableThumbArea = element.maximum.getOrCompute() - element.minimum.getOrCompute()
            when (element.orientation) {
                Orientation.HORIZONTAL -> {
                    batch.fillRect(rectX + thumbBounds.x.getOrCompute()
                            + (if (element.orientation == Orientation.HORIZONTAL) (currentValue / scrollableThumbArea * (thumbBounds.width.getOrCompute() - thumbW)) else 0f),
                            rectY - rectH + thumbBounds.y.getOrCompute(),
                            thumbW, thumbH)
                }
                Orientation.VERTICAL -> {
                    batch.fillRect(rectX + thumbBounds.x.getOrCompute(),
                            rectY - rectH + thumbBounds.y.getOrCompute() + (thumbBounds.height.getOrCompute() - thumbH)
                                    - (if (element.orientation == Orientation.VERTICAL) (currentValue / scrollableThumbArea * (thumbBounds.height.getOrCompute() - thumbH)) else 0f),
                            thumbW, thumbH)
                }
            }

            batch.packedColor = lastPackedColor
            ColorStack.pop()
        }

        override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        }
    }

    open class ScrollBarIncrButtonSkin(element: Button) : ButtonSkin(element) {
        init {
            this.defaultBgColor.set(Color(1f, 1f, 1f, 0f))
            this.hoveredBgColor.set(Color(0.85f, 0.85f, 0.85f, 0.5f))
            this.disabledBgColor.bind { defaultBgColor.use() }

            this.roundedRadius.set(0)
        }
    }
}

