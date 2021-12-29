package polyrhythmmania.solitaire

import com.badlogic.gdx.graphics.Color


enum class CardSuit(val color: Color) {
    A(Color.RED), B(Color.GREEN), C(Color.BLUE),
    
    WIDGET(Color.BLACK),
    ;
    
    companion object {
        val VALUES: List<CardSuit> = values().toList()
    }
}