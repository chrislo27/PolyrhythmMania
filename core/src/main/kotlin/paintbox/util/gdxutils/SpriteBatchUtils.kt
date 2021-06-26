package paintbox.util.gdxutils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import paintbox.PaintboxGame

fun SpriteBatch.fillRect(x: Float, y: Float, width: Float, height: Float) {
    this.draw(PaintboxGame.fillTexture, x, y, width, height)
}

fun SpriteBatch.fillRect(rect: Rectangle) {
    this.fillRect(rect.x, rect.y, rect.width, rect.height)
}

fun SpriteBatch.drawRect(x: Float, y: Float, width: Float, height: Float, lineX: Float, lineY: Float) {
    this.draw(PaintboxGame.fillTexture, x, y, width, lineY.coerceAtMost(height))
    this.draw(PaintboxGame.fillTexture, x, y + height, width, -(lineY.coerceAtMost(height)))
    this.draw(PaintboxGame.fillTexture, x, y + lineY, lineX.coerceAtMost(width), height - (lineY * 2).coerceAtMost(height))
    this.draw(PaintboxGame.fillTexture, x + width, y + lineY, -(lineX.coerceAtMost(width)), height - (lineY * 2).coerceAtMost(height))
}

fun SpriteBatch.drawRect(x: Float, y: Float, width: Float, height: Float, line: Float) {
    this.drawRect(x, y, width, height, line, line)
}

fun SpriteBatch.drawRect(rect: Rectangle, lineX: Float, lineY: Float) {
    this.drawRect(rect.x, rect.y, rect.width, rect.height, lineX, lineY)
}

fun SpriteBatch.drawRect(rect: Rectangle, line: Float) {
    this.drawRect(rect, line, line)
}

fun SpriteBatch.fillRoundedRect(x: Float, y: Float, w: Float, h: Float, cornerRad: Float) {
    var roundedRad = cornerRad
    if (roundedRad > w / 2f) {
        roundedRad = (w / 2f)
    }
    if (roundedRad > h / 2f) {
        roundedRad = (h / 2f)
    }
    if (roundedRad <= 0) {
        this.fillRect(x, y, w, h)
    } else {
        val spritesheet = PaintboxGame.paintboxSpritesheet
        val fill = spritesheet.fill
        val roundedRect = spritesheet.roundedCorner
        this.draw(fill, x + roundedRad, y + roundedRad, w - roundedRad * 2, h - roundedRad * 2) // Middle
        this.draw(fill, x, y + roundedRad, (roundedRad), h - roundedRad * 2) // Left
        this.draw(fill, x + w - roundedRad, y + roundedRad, (roundedRad), h - roundedRad * 2) // Right
        this.draw(fill, x + roundedRad, y, w - roundedRad * 2, (roundedRad)) // Bottom
        this.draw(fill, x + roundedRad, y + h - roundedRad, w - roundedRad * 2, (roundedRad)) // Top
        this.draw(roundedRect, x, y + h - roundedRad, (roundedRad), (roundedRad)) // TL
        this.draw(roundedRect, x, y + roundedRad, (roundedRad), (-roundedRad)) // BL
        this.draw(roundedRect, x + w, y + h - roundedRad, (-roundedRad), (roundedRad)) // TR
        this.draw(roundedRect, x + w, y + roundedRad, (-roundedRad), (-roundedRad)) // BR
    }
}

private val quadVerts: FloatArray = FloatArray(20)

/**
 * bottom left, bottom right, top right, top left
 */
fun SpriteBatch.drawQuad(x1: Float, y1: Float, color1: Color,
                         x2: Float, y2: Float, color2: Color,
                         x3: Float, y3: Float, color3: Color,
                         x4: Float, y4: Float, color4: Color,
                         texture: Texture = PaintboxGame.fillTexture) {
    this.drawQuad(x1, y1, color1.toFloatBits(),
                  x2, y2, color2.toFloatBits(),
                  x3, y3, color3.toFloatBits(),
                  x4, y4, color4.toFloatBits(),
                  texture)
}

/**
 * bottom left, bottom right, top right, top left
 */
fun SpriteBatch.drawQuad(x1: Float, y1: Float, color1: Float,
                         x2: Float, y2: Float, color2: Float,
                         x3: Float, y3: Float, color3: Float,
                         x4: Float, y4: Float, color4: Float,
                         texture: Texture = PaintboxGame.fillTexture,
                         blU: Float = 0f, blV: Float = 0f, brU: Float = 1f, brV: Float = 0f,
                         trU: Float = 1f, trV: Float = 1f, tlU: Float = 0f, tlV: Float = 1f) {
    var idx = 0

    quadVerts[idx++] = x1
    quadVerts[idx++] = y1
    quadVerts[idx++] = color1
    quadVerts[idx++] = blU
    quadVerts[idx++] = blV

    quadVerts[idx++] = x2
    quadVerts[idx++] = y2
    quadVerts[idx++] = color2
    quadVerts[idx++] = brU
    quadVerts[idx++] = brV

    quadVerts[idx++] = x3
    quadVerts[idx++] = y3
    quadVerts[idx++] = color3
    quadVerts[idx++] = trU
    quadVerts[idx++] = trV

    quadVerts[idx++] = x4
    quadVerts[idx++] = y4
    quadVerts[idx++] = color4
    quadVerts[idx++] = tlU
    quadVerts[idx] = tlV

    this.draw(texture, quadVerts, 0, 20)
}

inline fun SpriteBatch.batchCall(projection: Matrix4 = this.projectionMatrix, drawFunction: SpriteBatch.() -> Unit) {
    val oldProjection = this.projectionMatrix
    val oldColor = this.packedColor

    this.begin()
    this.drawFunction()
    this.end()

    this.projectionMatrix = oldProjection
    this.packedColor = oldColor
}

/**
 * The same as [SpriteBatch.draw(texture, x, y, width, height, u, v, u2, v2)] but [u] and [v] are in the top left, and
 * [u2] and [v2] are in the bottom right. The original [SpriteBatch] draw function has the origin in the bottom left.
 * 
 * The uv order for this function is more akin to [TextureRegion]'s internal uv representation.
 */
fun SpriteBatch.drawUV(tex: Texture, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float) {
    this.draw(tex, x, y, width, height, u, v2, u2, v)
}
