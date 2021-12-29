package polyrhythmmania.solitaire


enum class CardSymbol(val scaleOrder: Int, val textSymbol: String) {
    C(6, "7"), D(5, "6"), E(4, "5"), F(3, "4"), G(2, "3"), A(1, "2"), B(0, "1"),
    
    WIDGET_HALF(999, "Wh"), ROD(999, "R"),
    ;
    
    companion object {
        val VALUES: List<CardSymbol> = values().toList()
    }
    
    fun isWidgetLike(): Boolean = scaleOrder == 999
}