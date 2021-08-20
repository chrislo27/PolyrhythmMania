package paintbox.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.math.Rectangle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


object ScissorStack {
    
    private val stack: MutableList<Rectangle> = mutableListOf()

    /**
     * The scissor should be in screen coordinates with 0, 0 at the BOTTOM LEFT.
     */
    fun pushScissor(scissor: Rectangle, screenX: Int, screenY: Int): Boolean {
        scissor.normalize()
        if (stack.isEmpty()) {
            if (scissor.width < 1 || scissor.height < 1) return false
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
        } else {
            if (!(scissor.x + scissor.y + scissor.width + scissor.height).isFinite()) return false
            
            // Merge scissors
            val parent = stack.last()
            val minX = max(parent.x, scissor.x)
            val maxX = min(parent.x + parent.width, scissor.x + scissor.width)
            if (maxX - minX < 1) return false

            val minY = max(parent.y, scissor.y)
            val maxY = min(parent.y + parent.height, scissor.y + scissor.height)
            if (maxY - minY < 1) return false

            scissor.x = minX
            scissor.y = minY
            scissor.width = maxX - minX
            scissor.height = max(1f, maxY - minY)
        }
        stack += scissor
        HdpiUtils.glScissor(scissor.x.toInt() + screenX, scissor.y.toInt() + screenY,
                scissor.width.toInt(), scissor.height.toInt())
        return true
    }
    
    fun popScissor(): Rectangle? {
        val last = stack.removeLastOrNull() ?: return null
        if (stack.isEmpty()) {
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
        } else {
            val next = stack.last()
            HdpiUtils.glScissor(next.x.toInt(), next.y.toInt(), next.width.toInt(), next.height.toInt())
        }
        return last
    }
    
    private fun Rectangle.normalize() {
        val rect = this
        rect.x = rect.x.roundToInt().toFloat()
        rect.y = rect.y.roundToInt().toFloat()
        rect.width = rect.width.roundToInt().toFloat()
        rect.height = rect.height.roundToInt().toFloat()
//        rect.y -= rect.height
        if (rect.width < 0) {
            rect.width = -rect.width
            rect.x -= rect.width
        }
        if (rect.height < 0) {
            rect.height = -rect.height
            rect.y -= rect.height
        }
    }
}