package io.github.chrislo27.paintbox.ui.area


data class Insets(val top: Float, val bottom: Float, val left: Float, val right: Float) {
    companion object {
        val ZERO: Insets = Insets(0f, 0f, 0f, 0f)
    }
    
    constructor(all: Float) : this(all, all, all, all)
}
