package polyrhythmmania.solitaire


enum class CardSymbol(val scaleOrder: Int, val textSymbol: String, val semitone: Int = 0) {
    C(6, "7", 11), D(5, "6", 9), E(4, "5", 7), F(3, "4", 5), G(2, "3", 4), A(1, "2", 2), B(0, "1", 0),
    
    WIDGET_HALF(999, "Wh"), ROD(999, "R"),
    ;
    
    companion object {
        val VALUES: List<CardSymbol> = values().toList()
    }
    
    fun isWidgetLike(): Boolean = scaleOrder == 999
}