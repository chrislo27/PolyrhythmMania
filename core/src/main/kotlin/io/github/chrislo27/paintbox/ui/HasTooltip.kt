package io.github.chrislo27.paintbox.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.PaintboxFont
import io.github.chrislo27.paintbox.font.TextBlock
import io.github.chrislo27.paintbox.ui.area.Insets
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


open class Tooltip
    : TextLabel {

    init {
        this.backgroundColor.set(Color(0f, 0f, 0f, 0.85f))
        this.textColor.set(Color.WHITE)
        this.bgPadding.set(Insets(8f))
        this.renderBackground.set(true)
        this.doXCompression.set(true)
        this.renderAlign.set(Align.topLeft)

        this.text.addListener {
            resizeBoundsToContent()
        }
    }

    constructor(text: String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
            : super(text, font)

    constructor(binding: Var.Context.() -> String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
            : super(binding, font)

    fun resizeBoundsToContent() {
        val textBlock: TextBlock = this.internalTextBlock.getOrCompute()
        if (textBlock.isRunInfoInvalid()) {
            textBlock.computeLayouts()
        }
        val textWidth = textBlock.width
        val textHeight = textBlock.height
        val bgPaddingInsets = this.bgPadding.getOrCompute()

        val computedWidth = bgPaddingInsets.left + bgPaddingInsets.right + textWidth
        val computedHeight = bgPaddingInsets.top + bgPaddingInsets.bottom + textHeight
        this.bounds.width.set(computedWidth)
        this.bounds.height.set(computedHeight)
    }

}