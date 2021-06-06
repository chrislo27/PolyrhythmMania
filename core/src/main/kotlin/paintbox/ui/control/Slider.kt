package paintbox.ui.control

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import paintbox.PaintboxGame
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.Skin
import paintbox.ui.skin.SkinFactory
import paintbox.util.MathHelper
import paintbox.util.gdxutils.fillRect
import paintbox.util.gdxutils.fillRoundedRect


/**
 * A [Slider] is a scroll bar intended to be used with a [ScrollPane].
 */
open class Slider : Control<Slider>() {
    companion object {
        const val SLIDER_SKIN_ID: String = "Slider"
        const val MIN_DEFAULT: Float = 0f
        const val MAX_DEFAULT: Float = 100f
        const val TICK_DEFAULT: Float = 10f

        init {
            DefaultSkins.register(SLIDER_SKIN_ID, SkinFactory { element: Slider ->
                SliderSkin(element)
            })
        }
    }

    val minimum: FloatVar = FloatVar(MIN_DEFAULT)
    val maximum: FloatVar = FloatVar(MAX_DEFAULT)
    val tickUnit: FloatVar = FloatVar(TICK_DEFAULT)
    private val _value: FloatVar = FloatVar(MIN_DEFAULT)
    val value: ReadOnlyVar<Float> = _value


    init {
        minimum.addListener {
            setValue(_value.getOrCompute())
        }
        maximum.addListener {
            setValue(_value.getOrCompute())
        }
        
        val lastMouseRelativeToRoot = Vector2(0f, 0f)
        addInputEventListener { event ->
            if (event is MouseInputEvent && (event is TouchDragged || event is ClickPressed)) {
                if (pressedState.getOrCompute().pressed) {
                    val lastMouseInside: Vector2 = this.getPosRelativeToRoot(lastMouseRelativeToRoot)
                    lastMouseRelativeToRoot.x = event.x - lastMouseInside.x
                    lastMouseRelativeToRoot.y = event.y - lastMouseInside.y
                    
                    setValue(convertPercentageToValue((lastMouseRelativeToRoot.x / bounds.width.getOrCompute()).coerceIn(0f, 1f)))
                    
                    true
                } else false
            } else false
        }
    }

    fun setValue(value: Float) {
        val tick = tickUnit.getOrCompute().coerceAtLeast(0f)
        val snapped = if (tick > 0f) {
            MathHelper.snapToNearest(value, tick)
        } else value
        _value.set(snapped.coerceIn(minimum.getOrCompute(), maximum.getOrCompute()))
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

    override fun getDefaultSkinID(): String = SLIDER_SKIN_ID


    open class SliderSkin(element: Slider) : Skin<Slider>(element) {

        val bgColor: Var<Color> = Var(Color(0.94f, 0.94f, 0.94f, 1f))
        val filledColor: Var<Color> = Var(Color(0.24f, 0.74f, 0.94f, 1f))

        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
            val contentBounds = element.contentZone
            val rectX = contentBounds.x.getOrCompute() + originX
            val rectY = originY - contentBounds.y.getOrCompute()
            val rectW = contentBounds.width.getOrCompute()
            val rectH = contentBounds.height.getOrCompute()
            val lastPackedColor = batch.packedColor
            val opacity = element.apparentOpacity.getOrCompute()
            val tmpColor = ColorStack.getAndPush()

            val lineH = rectH * 0.4f
            val linePad = 4f
            val circleH = rectH
            
            tmpColor.set(bgColor.getOrCompute())
            tmpColor.a *= opacity
            batch.color = tmpColor
            batch.fillRoundedRect(rectX + linePad, rectY - rectH * 0.5f - lineH * 0.5f, rectW - linePad * 2, lineH, lineH * 0.5f)
            tmpColor.set(filledColor.getOrCompute())
            tmpColor.a *= opacity
            batch.color = tmpColor
            val valueAsPercent = element.convertValueToPercentage(element._value.getOrCompute())
            batch.fillRoundedRect(rectX + linePad, rectY - rectH * 0.5f - lineH * 0.5f, (rectW - linePad * 2) * valueAsPercent, lineH, lineH * 0.5f)

            tmpColor.mul(0.97f)
            batch.color = tmpColor
            batch.draw(PaintboxGame.paintboxSpritesheet.circleFilled, rectX + (valueAsPercent * (rectW - circleH)), rectY - rectH, circleH, circleH)

            batch.packedColor = lastPackedColor
            ColorStack.pop()
        }

        override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        }
    }
}

