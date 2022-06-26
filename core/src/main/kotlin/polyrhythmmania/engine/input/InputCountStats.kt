package polyrhythmmania.engine.input


/**
 * Holding class for counting input types.
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
}
