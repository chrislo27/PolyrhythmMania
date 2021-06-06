package paintbox.ui.control

import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.binding.VarChangedListener


/**
 * A [ToggleGroup] manages [Toggle] objects and ensures that only one [Toggle] can be selected at a time.
 *
 * To register a [Toggle], use the [addToggle] function (DON'T simply set the [Toggle.toggleGroup] var).
 */
class ToggleGroup {

    private val linkedToggles: MutableMap<Toggle, Metadata> = mutableMapOf()

    private val _activeToggle: Var<Toggle?> = Var(null)
    val activeToggle: ReadOnlyVar<Toggle?> get() = _activeToggle

    fun addToggle(toggle: Toggle) {
        val toggleCurrentGroup = toggle.toggleGroup.getOrCompute()
        if (toggleCurrentGroup == this && toggle in linkedToggles) return

        toggleCurrentGroup?.removeToggle(toggle)
        toggle.toggleGroup.set(this)
        if (toggle !in linkedToggles) {
            val listener = VarChangedListener<Boolean> { property ->
                update(property, toggle)
            }
            linkedToggles[toggle] = Metadata(toggle, listener)
            toggle.selectedState.addListener(listener)

            val currentActive = activeToggle.getOrCompute()
            if (toggle.selectedState.getOrCompute()) {
                if (currentActive == null || currentActive !== toggle) {
                    _activeToggle.set(toggle)
                }
            }
        }
    }

    fun removeToggle(toggle: Toggle) {
        val metadata = linkedToggles.remove(toggle)
        if (metadata != null) toggle.selectedState.removeListener(metadata.listener)
        toggle.toggleGroup.set(null)
        if (activeToggle.getOrCompute() === toggle) {
            _activeToggle.set(null)
        }
    }

    private fun update(property: ReadOnlyVar<Boolean>, toggle: Toggle) {
        if (property.getOrCompute()) {
            _activeToggle.set(toggle)
            // Update the other Toggles to be false (unless the property is the same)
            linkedToggles.keys.toList().forEach { t ->
                if (t !== toggle) {
                    val prop = t.selectedState
                    if (prop !== property) {
                        prop.set(false)
                    }
                }
            }
        }
    }

    private data class Metadata(val toggle: Toggle, val listener: VarChangedListener<Boolean>)

}