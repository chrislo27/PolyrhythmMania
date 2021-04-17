package io.github.chrislo27.paintbox

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion


class PaintboxSpritesheet(val texture: Texture) {
    val roundedCorner: TextureRegion = TextureRegion(texture, 0, 0, 32, 32)
    val fill: TextureRegion = TextureRegion(texture, 36, 1, 2, 2)
    val logo128: TextureRegion = TextureRegion(texture, 0, 384, 128, 128)
}