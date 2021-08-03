package polyrhythmmania.world.tileset

import com.badlogic.gdx.graphics.Color
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var


open class TintedRegion(val regionID: String) {

    open val color: ReadOnlyVar<Color> = Var(Color(1f, 1f, 1f, 1f))

    constructor(regionID: String, initColor: Color)
            : this(regionID) {
        @Suppress("LeakingThis")
        (color as? Var)?.set(Color(1f, 1f, 1f, 1f).set(initColor))
    }

    constructor(regionID: String, binding: Var.Context.() -> Color)
            : this(regionID) {
        @Suppress("LeakingThis")
        (color as? Var)?.bind(binding)
    }

    constructor(regionID: String, toBindTo: ReadOnlyVar<Color>)
            : this(regionID, binding = { toBindTo.use() })

}

class EditableTintedRegion(regionID: String)
    : TintedRegion(regionID) {

    override val color: Var<Color> = Var(Color(1f, 1f, 1f, 1f))

    constructor(regionID: String, initColor: Color)
            : this(regionID) {
        color.set(Color(1f, 1f, 1f, 1f).set(initColor))
    }
}

open class TintedSubregion(val parent: TintedRegion, val u: Float, val v: Float, val u2: Float, val v2: Float)
    : TintedRegion(parent.regionID) {
    
    override val color: ReadOnlyVar<Color> = parent.color
    
}
