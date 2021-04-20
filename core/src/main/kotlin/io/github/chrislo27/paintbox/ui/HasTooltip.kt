package io.github.chrislo27.paintbox.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.PaintboxFont
import io.github.chrislo27.paintbox.font.TextBlock
import io.github.chrislo27.paintbox.ui.control.TextLabel


interface HasTooltip {

    val tooltipElement: Var<UIElement?>

    /**
     * Called when the [tooltip] is added to the scene. 
     * 
     * The [tooltip] should recompute its bounds if it is dynamically sized.
     * Behaviour for [Tooltip] is already implemented by default.
     */
    fun onTooltipStarted(tooltip: UIElement) {
        if (tooltip is Tooltip) 
            tooltip.resizeBoundsToContent()
    }

    /**
     * Called when the [tooltip] is removed.
     */
    fun onTooltipEnded(tooltip: UIElement) {
    }
}



open class Tooltip(text: String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
    : TextLabel(text, font) {
    
    init {
        this.backgroundColor.set(Color(0f, 0f, 0f, 0.8f))
        this.textColor.set(Color.WHITE)
        this.bgPadding.set(8f)
        this.renderBackground.set(true)
        this.doXCompression.set(true)
        this.renderAlign.set(Align.topLeft)
        
        this.text.addListener {
            resizeBoundsToContent()
        }
    }

    fun resizeBoundsToContent() {
        val textBlock: TextBlock = this.internalTextBlock.getOrCompute()
        if (textBlock.isRunInfoInvalid()) {
            textBlock.computeLayouts()
        }
        val textWidth = textBlock.width
        val textHeight = textBlock.height
        val bgPadding = this.bgPadding.getOrCompute()

        val computedWidth = bgPadding * 2 + textWidth
        val computedHeight = bgPadding * 2 + textHeight
        this.bounds.width.set(computedWidth)
        this.bounds.height.set(computedHeight)
    }

}