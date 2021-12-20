package polyrhythmmania.statistics

import paintbox.binding.IntVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var


fun interface StatFormatter {
    
    companion object {
        val NO_FORMAT: StatFormatter = StatFormatter { value -> Var { "$value" }}
    }
    
    fun format(value: IntVar): ReadOnlyVar<String>
    
}