package paintbox.ui.area

import paintbox.binding.FloatVar


data class Bounds(override val x: FloatVar, override val y: FloatVar,
                  override val width: FloatVar, override val height: FloatVar)
    : ReadOnlyBounds {

    constructor(x: Float, y: Float, width: Float, height: Float)
            : this(FloatVar(x), FloatVar(y), FloatVar(width), FloatVar(height))

}