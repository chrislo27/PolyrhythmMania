package paintbox.ui.area

import com.badlogic.gdx.math.Rectangle
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyFloatVar
import paintbox.binding.ReadOnlyVar


interface ReadOnlyBounds {
    
    val x: ReadOnlyFloatVar
    val y: ReadOnlyFloatVar
    val width: ReadOnlyFloatVar
    val height: ReadOnlyFloatVar
    
    fun toRectangle(): Rectangle {
        return Rectangle(this.x.get(), this.y.get(),
                this.width.get(), this.height.get())
    }
    
    /**
     * Returns true if the x/y point is within this bounds locally.
     */
    fun containsPointLocal(x: Float, y: Float): Boolean {
        val thisX = this.x.get()
        val thisY = this.y.get()
        val width = this.width.get()
        val height = this.height.get()
        return x >= thisX && x <= thisX + width && y >= thisY && y <= thisY + height
    }
}