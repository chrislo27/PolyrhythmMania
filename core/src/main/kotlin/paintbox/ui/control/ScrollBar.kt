package paintbox.ui.control

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import paintbox.PaintboxGame
import paintbox.binding.*
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.Skin
import paintbox.ui.skin.SkinFactory
import paintbox.util.ColorStack
import paintbox.util.gdxutils.fillRect
import paintbox.util.gdxutils.fillRoundedRect


/**
 * A [ScrollBar] is a scroll bar intended to be used with a [ScrollPane].
 */
open class ScrollBar(val orientation: Orientation) : Control<ScrollBar>() {
    companion object {
        const val SCROLLBAR_SKIN_ID: String = "ScrollBar"
        const val SCROLLBAR_INC_BUTTON_SKIN_ID: String = "ScrollBar_UnitIncreaseButton"
        const val MIN_DEFAULT: Float = 0f
        const val MAX_DEFAULT: Float = 100f
        const val UNIT_DEFAULT: Float = 15f
        const val BLOCK_DEFAULT: Float = 40f
        const val VISIBLE_AMOUNT_DEFAULT: Float = 15f

        protected val DEFAULT_USER_CHANGED_VALUE_LISTENER: (Float) -> Unit = {}

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
    val value: ReadOnlyFloatVar = FloatVar {
        val min = minimum.useF()
        val max = maximum.useF()
        _value.useF().coerceIn(min, max)
    }
    val thumbPressedState: ReadOnlyVar<PressedState> get() = thumbArea.pressedState

    val decreaseButton: UnitIncreaseButton
    val increaseButton: UnitIncreaseButton
    val thumbArea: ThumbPane

    var userChangedValueListener: (newValue: Float) -> Unit = DEFAULT_USER_CHANGED_VALUE_LISTENER

    init {
        decreaseButton = UnitIncreaseButton(this, getArrowButtonTexReg(), this.orientation, false).apply {
            Anchor.TopLeft.configure(this)
            when (orientation) {
                Orientation.VERTICAL -> this.bindHeightToSelfWidth()
                Orientation.HORIZONTAL -> this.bindWidthToSelfHeight()
            }
            this.skinID.set(ScrollBar.SCROLLBAR_INC_BUTTON_SKIN_ID)
            this.disabled.bind { value.useF() <= minimum.useF() }
            this.setOnAction {
                decrement()
            }
        }
        increaseButton = UnitIncreaseButton(this, getArrowButtonTexReg(), this.orientation, true).apply {
            Anchor.BottomRight.configure(this)
            when (orientation) {
                Orientation.VERTICAL -> this.bindHeightToSelfWidth()
                Orientation.HORIZONTAL -> this.bindWidthToSelfHeight()
            }
            this.skinID.set(ScrollBar.SCROLLBAR_INC_BUTTON_SKIN_ID)
            this.disabled.bind { value.useF() >= maximum.useF() }
            this.setOnAction {
                increment()
            }
        }
        thumbArea = ThumbPane(this).apply {
            Anchor.Centre.configure(this)
            when (orientation) {
                Orientation.VERTICAL -> {
                    this.bindHeightToParent { -(bounds.width.useF() * 2) }
                }
                Orientation.HORIZONTAL -> {
                    this.bindWidthToParent { -(bounds.height.useF() * 2) }
                }
            }
        }
        this.addChild(decreaseButton)
        this.addChild(increaseButton)
        this.addChild(thumbArea)
    }

    fun setValue(value: Float) {
        setValue(value, false)
    }

    protected fun setValue(value: Float, wasUserChange: Boolean) {
        val oldValue = _value.get()
        val newValue = value.coerceIn(minimum.get(), maximum.get())
        if (newValue != oldValue) {
            _value.set(newValue)
            if (wasUserChange) {
                userChangedValueListener.invoke(newValue)
            }
        }
    }

    fun increment() {
        setValue(value.get() + unitIncrement.get(), true)
    }

    fun decrement() {
        setValue(value.get() - unitIncrement.get(), true)
    }

    fun incrementBlock() {
        setValue(value.get() + blockIncrement.get(), true)
    }

    fun decrementBlock() {
        setValue(value.get() - blockIncrement.get(), true)
    }

    protected open fun getArrowButtonTexReg(): TextureRegion {
        return PaintboxGame.paintboxSpritesheet.upArrow
    }

    fun convertValueToPercentage(v: Float): Float {
        val min = minimum.get()
        val max = maximum.get()
        return ((v - min) / (max - min)).coerceIn(0f, 1f)
    }

    fun convertPercentageToValue(v: Float): Float {
        val min = minimum.get()
        val max = maximum.get()
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
        : Pane(), HasPressedState {

        override val isHoveredOver: ReadOnlyBooleanVar = BooleanVar(false)
        override val isPressedDown: ReadOnlyBooleanVar = BooleanVar(false)
        override val pressedState: ReadOnlyVar<PressedState> = HasPressedState.createDefaultPressedStateVar(isHoveredOver, isPressedDown)
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
                        (isHoveredOver as BooleanVar).set(false)
                    }
                    is ClickReleased -> {
                        if (event.button == Input.Buttons.LEFT && isPressedDown.get()) {
                            (isPressedDown as BooleanVar).set(false)
                        }
                    }
                    is MouseMoved -> {
                        if (mousePercent in currentThumbPosPercent..(currentThumbPosPercent + currentThumbWidthPercent)) {
                            (isHoveredOver as BooleanVar).set(true)
                        } else {
                            (isHoveredOver as BooleanVar).set(false)
                        }
                    }
                }
                if (!scrollBar.apparentDisabledState.get()) {
                    when (event) {
                        is ClickPressed -> {
                            if (event.button == Input.Buttons.LEFT) {
                                // Jump a block increment or begin dragging the thumb


                                if (mousePercent in currentThumbPosPercent..(currentThumbPosPercent + currentThumbWidthPercent)) {
                                    // Inside the thumb.
                                    (isPressedDown as BooleanVar).set(true)
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
                                scrollBar.setValue(newValue, true)
                                true
                            } else false
                        }
                        is ClickReleased -> {
                            if (isPressedDown.get()) {
                                (isPressedDown as BooleanVar).set(false)
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
            val thumbBoundsWidth = thumbBounds.width.get()
            val thumbBoundsHeight = thumbBounds.height.get()
            val thumbW = (if (orientation == Orientation.VERTICAL)
                (1f)
            else (scrollBar.convertValueToPercentage(scrollBar.visibleAmount.get()))) * thumbBoundsWidth
            val thumbH = (if (scrollBar.orientation == Orientation.HORIZONTAL)
                (1f)
            else (scrollBar.convertValueToPercentage(scrollBar.visibleAmount.get()))) * thumbBoundsHeight

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
            val min = scrollBar.minimum.get()
            val max = scrollBar.maximum.get()
            return ((scrollBar.value.get() - min) / (max - min))
        }

        /**
         * Returns the percentage width/height of the thumb. Note that it is relative to the top left
         * of the scroll thumb, but all values will be in the range 0.0 to 1.0.
         */
        fun getCurrentThumbWidthPercentage(): Float {
            val visibleAmt = scrollBar.visibleAmount.get()
            val min = scrollBar.minimum.get()
            val max = scrollBar.maximum.get()
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
                    if (b.apparentDisabledState.useB()) disabledColor.use() else incrementColor.use()
                }
            }
        }

        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
            val contentBounds = element.contentZone
            val rectX = contentBounds.x.get() + originX
            val rectY = originY - contentBounds.y.get()
            val rectW = contentBounds.width.get()
            val rectH = contentBounds.height.get()
            val lastPackedColor = batch.packedColor
            val opacity = element.apparentOpacity.get()
            val tmpColor = ColorStack.getAndPush()

            tmpColor.set(bgColor.getOrCompute())
            tmpColor.a *= opacity

            batch.color = tmpColor
            batch.fillRect(rectX, rectY - rectH, rectW, rectH)

            val pressedState = element.thumbArea.pressedState.getOrCompute()
            tmpColor.set((when {
                element.apparentDisabledState.get() -> disabledColor
                pressedState.pressed -> thumbPressedColor
                pressedState.hovered -> thumbHoveredColor
                else -> thumbColor
            }).getOrCompute())
            tmpColor.a *= opacity
            batch.color = tmpColor
            val thumb = element.thumbArea
            val thumbBounds = thumb.bounds
            val thumbW = (if (element.orientation == Orientation.VERTICAL)
                (1f)
            else (element.convertValueToPercentage(element.visibleAmount.get()))) * thumbBounds.width.get()
            val thumbH = (if (element.orientation == Orientation.HORIZONTAL)
                (1f)
            else (element.convertValueToPercentage(element.visibleAmount.get()))) * thumbBounds.height.get()
            val currentValue = element.value.get()
            val scrollableThumbArea = element.maximum.get() - element.minimum.get()
            when (element.orientation) {
                Orientation.HORIZONTAL -> {
                    batch.fillRoundedRect(rectX + thumbBounds.x.get()
                            + (if (element.orientation == Orientation.HORIZONTAL) (currentValue / scrollableThumbArea * (thumbBounds.width.get() - thumbW)) else 0f),
                            rectY - rectH + thumbBounds.y.get(),
                            thumbW, thumbH, thumbH / 2f)
//                    batch.fillRect(rectX + thumbBounds.x.getOrCompute()
//                            + (if (element.orientation == Orientation.HORIZONTAL) (currentValue / scrollableThumbArea * (thumbBounds.width.getOrCompute() - thumbW)) else 0f),
//                            rectY - rectH + thumbBounds.y.getOrCompute(),
//                            thumbW, thumbH)
                }
                Orientation.VERTICAL -> {
                    batch.fillRoundedRect(rectX + thumbBounds.x.get(),
                            rectY - rectH + thumbBounds.y.get() + (thumbBounds.height.get() - thumbH)
                                    - (if (element.orientation == Orientation.VERTICAL) (currentValue / scrollableThumbArea * (thumbBounds.height.get() - thumbH)) else 0f),
                            thumbW, thumbH, thumbW / 2f)
//                    batch.fillRect(rectX + thumbBounds.x.getOrCompute(),
//                            rectY - rectH + thumbBounds.y.getOrCompute() + (thumbBounds.height.getOrCompute() - thumbH)
//                                    - (if (element.orientation == Orientation.VERTICAL) (currentValue / scrollableThumbArea * (thumbBounds.height.getOrCompute() - thumbH)) else 0f),
//                            thumbW, thumbH)
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

