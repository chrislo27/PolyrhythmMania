package polyrhythmmania.world.tileset

import com.badlogic.gdx.graphics.Color
import paintbox.binding.BooleanVar
import paintbox.binding.Var

/**
 * A [ColorMapping] is a string ID-mapped setting of a color. Used for adjusting the tileset palette.
 */
open class ColorMapping(val id: String, val tilesetGetter: (Tileset) -> Var<Color>,
                        val canAdjustAlpha: Boolean = false, val fallbackIDs: List<String> = emptyList(),
                        val color: Var<Color> = Var(Color(1f, 1f, 1f, 1f)),
                        val enabled: BooleanVar = BooleanVar(true),
                        val defaultEnabledStateIfWasOlderVersion: Boolean = true) {
    
    open fun copyFrom(tileset: Tileset) {
        val varr = tilesetGetter(tileset)
        this.color.set(varr.getOrCompute().cpy())
    }

    open fun applyTo(tileset: Tileset) {
        val varr = tilesetGetter(tileset)
        varr.set(this.color.getOrCompute().cpy())
    }
}
