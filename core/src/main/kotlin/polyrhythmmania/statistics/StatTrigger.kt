package polyrhythmmania.statistics


fun interface StatTrigger {
    
    fun onIncremented(stat: Stat, oldValue: Int, newValue: Int)
    
}