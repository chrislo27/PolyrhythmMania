package polyrhythmmania.solitaire


data class Card(val suit: CardSuit, val symbol: CardSymbol) {
    
    companion object {
        val STANDARD_DECK: List<Card> = mutableListOf<Card>().apply {
            this += listOf(CardSuit.A, CardSuit.B, CardSuit.C).flatMap { suit ->
                CardSymbol.SCALE_CARDS.map { sym ->
                    Card(suit, sym)
                } + mutableListOf<Card>().apply {
                    repeat(2) {
                        this += Card(suit, CardSymbol.WIDGET_HALF)
                    }
                    this += Card(suit, CardSymbol.ROD)
                }
            }
        }
    }
    
}
