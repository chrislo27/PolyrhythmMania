package io.github.chrislo27.paintbox.ui.area

import com.badlogic.gdx.math.Rectangle
import io.github.chrislo27.paintbox.util.ReadOnlyVar


interface ReadOnlyBounds {
    
    val x: ReadOnlyVar<Float>
    val y: ReadOnlyVar<Float>
    val width: ReadOnlyVar<Float>
    val height: ReadOnlyVar<Float>
    
    fun toRectangle(): Rectangle {
        return Rectangle(this.x.getOrCompute(), this.y.getOrCompute(), this.width.getOrCompute(), this.height.getOrCompute())
    }
    
    /**
     * Returns true if the x/y point is within this bounds locally.
     */
    fun containsPointLocal(x: Float, y: Float): Boolean {
        val thisX = this.x.getOrCompute()
        val thisY = this.y.getOrCompute()
        val width = this.width.getOrCompute()
        val height = this.height.getOrCompute()
        return x >= thisX && x <= thisX + width && y >= thisY && y <= thisY + height
    }
}