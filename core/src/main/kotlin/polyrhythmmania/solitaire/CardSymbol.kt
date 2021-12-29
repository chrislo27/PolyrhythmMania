package polyrhythmmania.solitaire


enum class CardSymbol(val scaleOrder: Int) {
    C(0), D(1), E(2), F(3), G(4), A(5), B(6),
    
    WIDGET_HALF(999), ROD(999),
    ;
    
    companion object {
        val VALUES: List<CardSymbol> = values().toList()
    }
}