package polyrhythmmania.solitaire


data class Card(val suit: CardSuit, val symbol: CardSymbol) {
    
    companion object {
        val STANDARD_DECK: List<Card> = mutableListOf<Card>().apply {
            this += listOf(CardSuit.A, CardSuit.B, CardSuit.C).flatMap { suit ->
                listOf(CardSymbol.C, CardSymbol.D, CardSymbol.E, CardSymbol.F, CardSymbol.G, CardSymbol.A, 
                        CardSymbol.B).map { sym -> Card(suit, sym) }
            }
            repeat(4) {
                repeat(2) {
                    this += Card(CardSuit.WIDGET, CardSymbol.WIDGET_HALF)
                }
                this += Card(CardSuit.WIDGET, CardSymbol.ROD)
            }
        }
    }
    
}
