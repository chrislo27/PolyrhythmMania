package paintbox.ui.control

import paintbox.binding.BooleanVar
import paintbox.binding.Var


interface Toggle {
    
    val selectedState: BooleanVar

    /**
     * The [ToggleGroup] that this [Toggle] belongs to.
     * 
     * This should not be set. In order to register a [Toggle] to a [ToggleGroup], use [ToggleGroup.addToggle].
     */
    val toggleGroup: Var<ToggleGroup?>
    
}