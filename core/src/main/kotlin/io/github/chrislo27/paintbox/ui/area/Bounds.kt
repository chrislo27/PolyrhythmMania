package io.github.chrislo27.paintbox.ui.area

import io.github.chrislo27.paintbox.util.Var


data class Bounds(override val x: Var<Float>, override val y: Var<Float>,
                  override val width: Var<Float>, override val height: Var<Float>)
    : ReadOnlyBounds {

    constructor(x: Float, y: Float, width: Float, height: Float)
            : this(Var(x), Var(y), Var(width), Var(height))

}