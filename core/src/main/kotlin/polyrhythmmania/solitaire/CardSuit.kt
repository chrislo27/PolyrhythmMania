package polyrhythmmania.solitaire

import com.badlogic.gdx.graphics.Color
import paintbox.util.gdxutils.set


enum class CardSuit(val color: Color, val spriteID: String, val spriteIDSmall: String = spriteID + "_small") {
    A(Color().set(255, 56, 56), "suit_a"),
    B(Color().set(38, 204, 16), "suit_launcher"),
    C(Color().set(56, 129, 255), "suit_piston"),
    
    ;
    
    companion object {
        val VALUES: List<CardSuit> = values().toList()
    }
}