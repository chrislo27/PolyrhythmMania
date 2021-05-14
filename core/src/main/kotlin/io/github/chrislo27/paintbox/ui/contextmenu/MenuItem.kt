package io.github.chrislo27.paintbox.ui.contextmenu

import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.Markup
import io.github.chrislo27.paintbox.font.PaintboxFont
import io.github.chrislo27.paintbox.font.TextBlock
import io.github.chrislo27.paintbox.font.TextRun
import io.github.chrislo27.paintbox.ui.UIElement


/**
 * A [MenuItem] is an item used in a [ContextMenu]
 */
sealed class MenuItem {
    companion object {
        val NOTHING_ACTION: () -> Unit = {}
    }
    
    var closeMenuAfterAction: Boolean = true
    var onAction: () -> Unit = NOTHING_ACTION
}

/**
 * A [MenuItem] that will spawn a sub-menu with other items.
 */
class Menu(val otherItems: List<MenuItem>) : MenuItem()

/**
 * A [MenuItem] that represents a horizontal separator.
 */
class SeparatorMenuItem() : MenuItem()

/**
 * A [MenuItem] that has a custom [element] as the node. This [element] should NOT depend on its parent
 * bounds for width and height!
 */
class CustomMenuItem(val element: UIElement) : MenuItem()

class SimpleMenuItem(val textBlock: TextBlock) : MenuItem() {
    companion object {
        fun create(text: String, font: PaintboxFont): SimpleMenuItem {
            return SimpleMenuItem(TextRun(font, text).toTextBlock())
        }

        fun create(text: String, markup: Markup): SimpleMenuItem {
            return SimpleMenuItem(markup.parse(text))
        }
    }
    
    init {
        this.closeMenuAfterAction = true
    }
}

class CheckBoxMenuItem(val checkState: Var<Boolean>, val textBlock: TextBlock) : MenuItem() {

    companion object {
        fun create(checkState: Var<Boolean>, text: String, font: PaintboxFont): CheckBoxMenuItem {
            return CheckBoxMenuItem(checkState, TextRun(font, text).toTextBlock())
        }

        fun create(checkState: Var<Boolean>, text: String, markup: Markup): CheckBoxMenuItem {
            return CheckBoxMenuItem(checkState, markup.parse(text))
        }
    }
}