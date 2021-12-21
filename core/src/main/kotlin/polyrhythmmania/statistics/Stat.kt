package polyrhythmmania.statistics

import paintbox.binding.IntVar
import paintbox.binding.ReadOnlyIntVar


class Stat(
        val id: String, val formatter: StatFormatter,
        val initialValue: Int = 0,
        val resetValue: Int = initialValue
) {

    private val _value: IntVar = IntVar(initialValue)
    val value: ReadOnlyIntVar = _value
    
    val triggers: MutableList<StatTrigger> = mutableListOf()
    
    fun getLocalizationID(): String = "statistics.name.$id"

    /**
     * Increments this stat by the given amount, and runs any [triggers]. If amount is non-positive, nothing changes.
     */
    fun increment(amount: Int = 1): Int {
        val oldValue = _value.get()
        if (amount <= 0) return oldValue
        val newValue = _value.incrementAndGetBy(amount)
        triggers.forEach { it.onIncremented(this, oldValue, newValue) }
        return newValue
    }

    /**
     * Sets the value of the stat to the given amount WITHOUT running any [triggers]. Used for persistence and resets.
     */
    fun setValue(newAmount: Int) {
        _value.set(newAmount)
    }
    
}
