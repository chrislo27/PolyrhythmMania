package io.github.chrislo27.paintbox.font

import com.badlogic.gdx.utils.Disposable
import io.github.chrislo27.paintbox.PaintboxGame


open class FontCache(val game: PaintboxGame) : Disposable {

    val fonts: Map<String, PaintboxFont> = mutableMapOf()

    operator fun get(key: String): PaintboxFont {
        return fonts[key] ?: throw IllegalArgumentException("Font not found: $key")
    }

    operator fun set(key: String, font: PaintboxFont?) {
        if (font != null) {
            (fonts as MutableMap)[key] = font
        } else {
            val existing = fonts[key]
            if (existing != null) {
                fonts[key]!!.dispose()
                (fonts as MutableMap).remove(key)
            }
        }
    }

    fun resizeAll(width: Int, height: Int) {
        fonts.values.forEach {
            it.resize(width, height)
        }
    }

    override fun dispose() {
        fonts.values.forEach(Disposable::dispose)
    }
}