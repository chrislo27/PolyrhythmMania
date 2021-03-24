package io.github.chrislo27.paintbox.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Pool


/**
 * A convenience singleton for implementing temporary [com.badlogic.gdx.graphics.Color]s in a stack method.
 */
object ColorStack  {
    
    private val pool: ColorPool = ColorPool()
    private val stack: ArrayDeque<Color> = ArrayDeque()
    val numInStack: Int
        get() = stack.size

    /**
     * Returns a [Color], set to white (1f, 1f, 1f, 1f).
     */
    fun getAndPush(): Color {
        val obtained = pool.obtain()
        obtained.set(1f, 1f, 1f, 1f)
        stack.add(obtained)
        return obtained
    }

    /**
     * Pops the last [Color] off the stack. If there is nothing on the stack, null is returned.
     * The color that is returned will not be reset but may mutate or be reused later from
     * another invocation of [getAndPush].
     */
    fun pop(): Color? {
        if (stack.isEmpty()) return null
        val last = stack.removeLast()
        pool.free(last)
        return last
    }

    private class ColorPool : Pool<Color>() {
        override fun newObject(): Color {
            return Color(1f, 1f, 1f, 1f)
        }

        override fun free(color: Color?) {
            // Intentional: don't do a reset. The popped colour is returned so the information may be required temporarily
        }
    }
}