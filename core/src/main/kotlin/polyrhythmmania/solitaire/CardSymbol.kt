package polyrhythmmania.solitaire


enum class CardSymbol(
        val scaleOrder: Int, val textSymbol: String, val spriteID: String,
        val semitone: Int = 0, val offsetX: Int = 0, val offsetY: Int = 0
) {
    NUM_7(6, "7", "7", 11),
    NUM_6(5, "6", "6", 9),
    NUM_5(4, "5", "5", 7),
    NUM_4(3, "4", "4", 5),
    NUM_3(2, "3", "3", 4),
    NUM_2(1, "2", "2", 2),
    NUM_1(0, "1", "1", 0),

    WIDGET_HALF(999, "Wh", "widget", offsetX = -1, offsetY = -1), ROD(999, "R", "rod"),

    SPARE(9999, "SP", "special", semitone = 12)
    ;
    
    companion object {
        val SCALE_CARDS: List<CardSymbol> = listOf(NUM_7, NUM_6, NUM_5, NUM_4, NUM_3, NUM_2, NUM_1)
    }
    
    fun isWidgetLike(): Boolean = scaleOrder == 999
    
    fun isNumeric(): Boolean = scaleOrder in 0..6
    
    fun getNoteSFXID(): String {
        return when (this) {
            NUM_7 -> "sfx_note_B3"
            NUM_6 -> "sfx_note_A3"
            NUM_5 -> "sfx_note_G3"
            NUM_4 -> "sfx_note_F3"
            NUM_3 -> "sfx_note_E3"
            NUM_2 -> "sfx_note_D3"
            NUM_1 -> "sfx_note_C3"
            SPARE -> "sfx_note_C4"
            else -> "sfx_note_C3"
        }
    }
}