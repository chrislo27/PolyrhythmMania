package io.github.chrislo27.paintbox.ui

import com.badlogic.gdx.math.Rectangle
import io.github.chrislo27.paintbox.util.ReadOnlyVar
import io.github.chrislo27.paintbox.util.Var


class UIBounds(val element: UIElement) {
    
    val x: Var<Float> = Var(0f)
    val y: Var<Float> = Var(0f)
    val width: Var<Float> = Var {
        use(element.parent)?.bounds?.width?.use() ?: 0f
    }
    val height: Var<Float> = Var {
        use(element.parent)?.bounds?.height?.use() ?: 0f
    }
    
//    val globalX: ReadOnlyVar<Float> = Var {
//        (element.parent.use()?.bounds?.globalX?.use() ?: 0f) + x.use()
//    }
//    val globalY: ReadOnlyVar<Float> = Var {
//        (element.parent.use()?.bounds?.globalY?.use() ?: 0f) + y.use()
//    }
    
    fun setAll(x: Float? = null, y: Float? = null, width: Float? = null, height: Float? = null): UIBounds {
        if (x != null) this.x.set(x)
        if (y != null) this.x.set(y)
        if (width != null) this.width.set(width)
        if (height != null) this.height.set(height)
        return this
    }
    
    fun bindAll(x: (Var.Context.() -> Float)? = null, y: (Var.Context.() -> Float)? = null,
                width: (Var.Context.() -> Float)? = null, height: (Var.Context.() -> Float)? = null): UIBounds {
        if (x != null) this.x.bind(x)
        if (y != null) this.x.bind(y)
        if (width != null) this.width.bind(width)
        if (height != null) this.height.bind(height)
        return this
    }

    /**
     * Returns true if the x/y point is within this UIBounds locally. Does not account for parent offsets.
     */
    fun containsPointLocal(x: Float, y: Float): Boolean {
        val thisX = this.x.getOrCompute()
        val thisY = this.y.getOrCompute()
        return x >= thisX && x <= thisX + this.width.getOrCompute() && y >= thisY && y <= thisY + this.height.getOrCompute()
    }

//    /**
//     * Returns true if the x/y point is within this UIBounds, accounting for parental offsets.
//     */
//    fun containsPointGlobal(x: Float, y: Float): Boolean {
//        val thisX = this.globalX.getOrCompute()
//        val thisY = this.globalY.getOrCompute()
//        return x >= thisX && x <= thisX + this.width.getOrCompute() && y >= thisY && y <= thisY + this.height.getOrCompute()
//    }
    
    fun toLocalRectangle(): Rectangle =
            Rectangle(this.x.getOrCompute(), this.y.getOrCompute(), this.width.getOrCompute(), this.height.getOrCompute())
    
//    fun toGlobalRectangle(): Rectangle =
//            Rectangle(this.globalX.getOrCompute(), this.globalY.getOrCompute(), this.width.getOrCompute(), this.height.getOrCompute())

    fun toLocalString(): String {
        return "[${x.getOrCompute()}, ${y.getOrCompute()}, ${width.getOrCompute()}, ${height.getOrCompute()}]"
    }
//    fun toGlobalString(): String {
//        return "[${globalX.getOrCompute()}, ${globalY.getOrCompute()}, ${width.getOrCompute()}, ${height.getOrCompute()}]"
//    }
    
    override fun toString(): String = toLocalString()
}
