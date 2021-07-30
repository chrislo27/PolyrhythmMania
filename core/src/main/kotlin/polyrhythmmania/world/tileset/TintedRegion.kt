package polyrhythmmania.world.tileset

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var



open class TintedRegion(val region: TextureRegion, val spacing: Spacing = Spacing.ZERO) {
    
    data class Spacing(val spacing: Int, val normalWidth: Int, val normalHeight: Int) {
        companion object {
            val ZERO: Spacing = Spacing(0, 0, 0)
        }
    }
    
    open val color: ReadOnlyVar<Color> = Var(Color(1f, 1f, 1f, 1f))

    constructor(region: TextureRegion, initColor: Color, spacing: Spacing = Spacing.ZERO)
            : this(region, spacing) {
        @Suppress("LeakingThis")
        (color as? Var)?.set(Color(1f, 1f, 1f, 1f).set(initColor))
    }
    
    constructor(region: TextureRegion, spacing: Spacing = Spacing.ZERO, binding: Var.Context.() -> Color)
            : this(region, spacing) {
        @Suppress("LeakingThis")
        (color as? Var)?.bind(binding)
    }
}

class EditableTintedRegion(region: TextureRegion, spacing: Spacing = Spacing.ZERO)
    : TintedRegion(region, spacing) {

    override val color: Var<Color> = Var(Color(1f, 1f, 1f, 1f))

    constructor(region: TextureRegion, initColor: Color, spacing: Spacing = Spacing.ZERO)
            : this(region, spacing) {
        color.set(Color(1f, 1f, 1f, 1f).set(initColor))
    }
}
