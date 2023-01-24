package polyrhythmmania.world.tileset

import com.badlogic.gdx.graphics.Color
import paintbox.binding.ContextBinding
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var


/**
 * A [TintedRegion] holds the color data for a specific region. The region is accessed by the [regionID], so it doesn't
 * have to explicitly depend on a texture region due to changing texture packs.
 * 
 * The [color] is not directly changeable by default. For a mutable [Var] version, use [EditableTintedRegion].
 */
open class TintedRegion(val regionID: String) {

    open val color: ReadOnlyVar<Color> = Var(Color(1f, 1f, 1f, 1f))

    constructor(regionID: String, initColor: Color)
            : this(regionID) {
        @Suppress("LeakingThis")
        (color as? Var)?.set(Color(1f, 1f, 1f, 1f).set(initColor))
    }

    constructor(regionID: String, binding: ContextBinding<Color>)
            : this(regionID) {
        @Suppress("LeakingThis")
        (color as? Var)?.bind(binding)
    }

    constructor(regionID: String, toBindTo: ReadOnlyVar<Color>)
            : this(regionID, binding = { toBindTo.use() })

}

/**
 * Same as [TintedRegion] but [color] is a mutable [Var].
 */
class EditableTintedRegion(regionID: String)
    : TintedRegion(regionID) {

    override val color: Var<Color> = Var(Color(1f, 1f, 1f, 1f))

    constructor(regionID: String, initColor: Color)
            : this(regionID) {
        color.set(Color(1f, 1f, 1f, 1f).set(initColor))
    }
}

/**
 * Repreesnts a subregion of a [TintedRegion], using UV coordinates. The UV coordinates are relative to the 
 * [parent] region, not the texture!
 */
open class TintedSubregion(val parent: TintedRegion, val u: Float, val v: Float, val u2: Float, val v2: Float)
    : TintedRegion(parent.regionID) {
    
    override val color: ReadOnlyVar<Color> = parent.color
    
}
