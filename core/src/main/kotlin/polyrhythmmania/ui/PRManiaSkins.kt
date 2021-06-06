package polyrhythmmania.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.github.chrislo27.paintbox.ui.ColorStack
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.ScrollBar
import io.github.chrislo27.paintbox.ui.skin.DefaultSkins
import io.github.chrislo27.paintbox.ui.skin.SkinFactory
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import io.github.chrislo27.paintbox.util.gdxutils.fillRoundedRect


object PRManiaSkins {
    
    const val SCROLLBAR_SKIN: String = "PRMania_ScrollBar"
    
    init {
        DefaultSkins.register(SCROLLBAR_SKIN, SkinFactory { element: ScrollBar ->
            PRMScrollBarSkin(element)
        })
    }
}

open class PRMScrollBarSkin(element: ScrollBar) : ScrollBar.ScrollBarSkin(element) {
    init {
        bgColor.set(Color(1f, 1f, 1f, 0f))
        thumbColor.set(Color(0.31f, 0.31f, 0.31f, 1f))
        thumbHoveredColor.set(Color(0.41f, 0.41f, 0.41f, 1f))
        thumbPressedColor.set(Color(0.31f, 0.41f, 0.41f, 1f))
        
        listOf(element.increaseButton, element.decreaseButton).forEach { b ->
            b.padding.set(Insets(3f))
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

        val thumb = element.thumbArea
        val thumbBounds = thumb.bounds
        val thumbW = (if (element.orientation == ScrollBar.Orientation.VERTICAL)
            (1f)
        else (element.convertValueToPercentage(element.visibleAmount.getOrCompute()))) * thumbBounds.width.getOrCompute()
        val thumbH = (if (element.orientation == ScrollBar.Orientation.HORIZONTAL)
            (1f)
        else (element.convertValueToPercentage(element.visibleAmount.getOrCompute()))) * thumbBounds.height.getOrCompute()
        val currentValue = element.value.getOrCompute()
        val scrollableThumbArea = element.maximum.getOrCompute() - element.minimum.getOrCompute()
        val thin = 2f

        tmpColor.set(thumbColor.getOrCompute())
        tmpColor.a *= opacity
        batch.color = tmpColor
        when (element.orientation) {
            ScrollBar.Orientation.HORIZONTAL -> {
                batch.fillRect(rectX + thin, rectY - rectH + thumbBounds.y.getOrCompute() + (rectH - thin) * 0.5f,
                        thumbBounds.width.getOrCompute() - thin * 2, thin)
            }
            ScrollBar.Orientation.VERTICAL -> {
                batch.fillRect(rectX + (rectW - thin) * 0.5f, rectY - rectH + thumbBounds.y.getOrCompute() + thin,
                        thin, thumbBounds.height.getOrCompute() - thin * 2)
            }
        }

        val pressedState = element.thumbArea.pressedState.getOrCompute()
        tmpColor.set((when {
            element.apparentDisabledState.getOrCompute() -> disabledColor
            pressedState.pressed -> thumbPressedColor
            pressedState.hovered -> thumbHoveredColor
            else -> thumbColor
        }).getOrCompute())
        tmpColor.a *= opacity
        batch.color = tmpColor
        when (element.orientation) {
            ScrollBar.Orientation.HORIZONTAL -> {
                batch.fillRoundedRect(rectX + thumbBounds.x.getOrCompute()
                        + (if (element.orientation == ScrollBar.Orientation.HORIZONTAL) (currentValue / scrollableThumbArea * (thumbBounds.width.getOrCompute() - thumbW)) else 0f),
                        rectY - rectH + thumbBounds.y.getOrCompute(),
                        thumbW, thumbH, thumbH / 2f)
            }
            ScrollBar.Orientation.VERTICAL -> {
                batch.fillRoundedRect(rectX + thumbBounds.x.getOrCompute(),
                        rectY - rectH + thumbBounds.y.getOrCompute() + (thumbBounds.height.getOrCompute() - thumbH)
                                - (if (element.orientation == ScrollBar.Orientation.VERTICAL) (currentValue / scrollableThumbArea * (thumbBounds.height.getOrCompute() - thumbH)) else 0f),
                        thumbW, thumbH, thumbW / 2f)
            }
        }

        batch.packedColor = lastPackedColor
        ColorStack.pop()
    }
}