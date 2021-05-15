package io.github.chrislo27.paintbox.ui.control

import io.github.chrislo27.paintbox.binding.Var


interface Toggle {
    
    val selectedState: Var<Boolean>

    /**
     * The [ToggleGroup] that this [Toggle] belongs to.
     * 
     * This should not be set. In order to register a [Toggle] to a [ToggleGroup], use [ToggleGroup.addToggle].
     */
    val toggleGroup: Var<ToggleGroup?>
    
}