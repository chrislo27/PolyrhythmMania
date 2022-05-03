package polyrhythmmania.credits

import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import polyrhythmmania.Localization
import java.util.*


abstract class CreditsBase {
    
    abstract val credits: Map<ReadOnlyVar<String>, List<ReadOnlyVar<String>>>
    
    protected fun abcSorted(vararg things: String): List<String> = things.sortedBy { it.lowercase(Locale.ROOT) }
    protected fun List<String>.toVars(): List<ReadOnlyVar<String>> = this.map { Var(it) }
    
}
