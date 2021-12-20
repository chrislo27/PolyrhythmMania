package polyrhythmmania.statistics

import paintbox.binding.IntVar


class Stat(val id: String, val formatter: StatFormatter, val value: IntVar) {
    
    constructor(id: String, formatter: StatFormatter, initialValue: Int)
            : this(id, formatter, IntVar(initialValue))
    
    fun getLocalizationID(): String = "statistics.name.$id"
    
}
