package paintbox.util.gdxutils

import com.badlogic.gdx.graphics.Color


fun Color.grey(rgb: Float, a: Float = 1f): Color = this.set(rgb, rgb, rgb, a)

fun Color.set(r: Int, g: Int, b: Int, a: Int = 255): Color {
    this.set(r / 255f, g / 255f, b / 255f, a / 255f)
    return this
}
