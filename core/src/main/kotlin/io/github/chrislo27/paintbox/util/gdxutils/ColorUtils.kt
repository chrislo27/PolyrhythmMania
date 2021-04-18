package io.github.chrislo27.paintbox.util.gdxutils

import com.badlogic.gdx.graphics.Color


fun Color.grey(rgb: Float, a: Float = 1f): Color = this.set(rgb, rgb, rgb, a)
