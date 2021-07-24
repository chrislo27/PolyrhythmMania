package polyrhythmmania.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.util.ColorStack
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollBar
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.SkinFactory
import paintbox.util.gdxutils.fillRect
import paintbox.util.gdxutils.fillRoundedRect
import paintbox.util.gdxutils.grey


object PRManiaSkins {
    
    const val SCROLLBAR_SKIN: String = "PRMania_ScrollBar"
    const val EDITOR_SCROLLBAR_SKIN: String = "PRMania_ScrollBar_editor"
    
    init {
        DefaultSkins.register(SCROLLBAR_SKIN, SkinFactory { element: ScrollBar ->
            PRMScrollBarSkin(element)
        })
        DefaultSkins.register(EDITOR_SCROLLBAR_SKIN, SkinFactory { element: ScrollBar ->
            ScrollBar.ScrollBarSkin(element).also { skin ->
                skin.bgColor.set(Color().grey(0.1f, 1f))
                skin.incrementColor.set(Color(0.64f, 0.64f, 0.64f, 1f))
                skin.disabledColor.set(Color(0.31f, 0.31f, 0.31f, 1f))
                skin.thumbColor.set(Color(0.64f, 0.64f, 0.64f, 1f))
                skin.thumbHoveredColor.set(Color(0.70f, 0.70f, 0.70f, 1f))
                skin.thumbPressedColor.set(Color(0.50f, 0.64f, 0.64f, 1f))
            }
        })
    }
}

open class PRMScrollBarSkin(element: ScrollBar) : ScrollBar.ScrollBarSkin(element) {
    init {
        bgColor.set(Color(1f, 1f, 1f, 0f))
        thumbColor.set(Color(0.31f, 0.31f, 0.31f, 1f))
        thumbHoveredColor.set(Color(0.41f, 0.41f, 0.41f, 1f))
        thumbPressedColor.set(Color(0.31f, 0.41f, 0.41f, 1f))
        incrementColor.set(Color(0.31f, 0.31f, 0.31f, 1f))
        disabledColor.set(Color(0.31f, 0.31f, 0.31f, 0.5f))
        
        listOf(element.increaseButton, element.decreaseButton).forEach { b ->
            b.padding.set(Insets(3f))
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

        val thumb = element.thumbArea
        val thumbBounds = thumb.bounds
        val thumbW = (if (element.orientation == ScrollBar.Orientation.VERTICAL)
            (1f)
        else (element.convertValueToPercentage(element.visibleAmount.get()))) * thumbBounds.width.get()
        val thumbH = (if (element.orientation == ScrollBar.Orientation.HORIZONTAL)
            (1f)
        else (element.convertValueToPercentage(element.visibleAmount.get()))) * thumbBounds.height.get()
        val currentValue = element.value.get()
        val scrollableThumbArea = element.maximum.get() - element.minimum.get()
        val thin = 2f

        tmpColor.set(thumbColor.getOrCompute())
        tmpColor.a *= opacity
        batch.color = tmpColor
        when (element.orientation) {
            ScrollBar.Orientation.HORIZONTAL -> {
                batch.fillRect(rectX + thin, rectY - rectH + thumbBounds.y.get() + (rectH - thin) * 0.5f,
                        thumbBounds.width.get() - thin * 2, thin)
            }
            ScrollBar.Orientation.VERTICAL -> {
                batch.fillRect(rectX + (rectW - thin) * 0.5f, rectY - rectH + thumbBounds.y.get() + thin,
                        thin, thumbBounds.height.get() - thin * 2)
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
                batch.fillRoundedRect(rectX + thumbBounds.x.get()
                        + (if (element.orientation == ScrollBar.Orientation.HORIZONTAL) (currentValue / scrollableThumbArea * (thumbBounds.width.get() - thumbW)) else 0f),
                        rectY - rectH + thumbBounds.y.get(),
                        thumbW, thumbH, thumbH / 2f)
            }
            ScrollBar.Orientation.VERTICAL -> {
                batch.fillRoundedRect(rectX + thumbBounds.x.get(),
                        rectY - rectH + thumbBounds.y.get() + (thumbBounds.height.get() - thumbH)
                                - (if (element.orientation == ScrollBar.Orientation.VERTICAL) (currentValue / scrollableThumbArea * (thumbBounds.height.get() - thumbH)) else 0f),
                        thumbW, thumbH, thumbW / 2f)
            }
        }

        batch.packedColor = lastPackedColor
        ColorStack.pop()
    }
}