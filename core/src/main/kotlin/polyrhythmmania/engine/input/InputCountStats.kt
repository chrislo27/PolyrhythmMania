package polyrhythmmania.engine.input

import polyrhythmmania.statistics.GlobalStats


/**
 * Holding class for counting input types. These numbers are batched and pushed to [GlobalStats] at the end
 * of an endless mode or normal Polyrhythm gameplay.
 */
class InputCountStats {
    
    var total: Int = 0
    var missed: Int = 0
    var aces: Int = 0
    var goods: Int = 0
    var barelies: Int = 0
    var early: Int = 0
    var late: Int = 0
    
    init {
        reset()
    }

    fun reset() {
        total = 0
        missed = 0
        aces = 0
        goods = 0
        barelies = 0
        early = 0
        late = 0
    }
    
    fun addToGlobalStats() {
        GlobalStats.inputsGottenTotal.increment(this.total)
        GlobalStats.inputsMissed.increment(this.missed)
        GlobalStats.inputsGottenAce.increment(this.aces)
        GlobalStats.inputsGottenGood.increment(this.goods)
        GlobalStats.inputsGottenBarely.increment(this.barelies)
        GlobalStats.inputsGottenEarly.increment(this.early)
        GlobalStats.inputsGottenLate.increment(this.late)
    }
}
