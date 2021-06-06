package paintbox.util

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion

import com.badlogic.gdx.graphics.g2d.Batch


object TexturedQuad {
    
    private val vertices = FloatArray(25)
    private val WHITE_FLOAT_BITS: Float = Color.WHITE_FLOAT_BITS

    /**
     * Renders a quad, with vertices in order from bottom left, bottom right, top right, top left.
     */
    fun renderQuad(batch: Batch, tex: Texture, x1: Float, y1: Float, x2: Float, y2: Float,
                   x3: Float, y3: Float, x4: Float, y4: Float, u1: Float, v1: Float, u2: Float, v2: Float) {
        var id = 0

        // x, y (1)
        vertices[id++] = x1
        vertices[id++] = y1
        vertices[id++] = WHITE_FLOAT_BITS
        vertices[id++] = u1
        vertices[id++] = v2

        // x + w, y (2)
        vertices[id++] = x2
        vertices[id++] = y2
        vertices[id++] = WHITE_FLOAT_BITS
        vertices[id++] = u2
        vertices[id++] = v2

        // x + w, y + h (3)
        vertices[id++] = x3
        vertices[id++] = y3
        vertices[id++] = WHITE_FLOAT_BITS
        vertices[id++] = u2
        vertices[id++] = v1

        // x, y + h (4)
        vertices[id++] = x4
        vertices[id++] = y4
        vertices[id++] = WHITE_FLOAT_BITS
        vertices[id++] = u1
        vertices[id++] = v1

        // x, y (1)
        vertices[id++] = x1
        vertices[id++] = y1
        vertices[id++] = WHITE_FLOAT_BITS
        vertices[id++] = u1
        vertices[id] = v2
        batch.draw(tex, vertices, 0, vertices.size)
        batch.flush()
    }

    fun renderQuad(batch: Batch, reg: TextureRegion, x1: Float, y1: Float, x2: Float,
                   y2: Float, x3: Float, y3: Float, x4: Float, y4: Float) {
        renderQuad(batch, reg.texture, x1, y1, x2, y2, x3, y3, x4, y4, reg.u, reg.v, reg.u2, reg.v2)
    }
}