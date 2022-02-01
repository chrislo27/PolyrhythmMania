package polyrhythmmania.solitaire

import paintbox.binding.BooleanVar
import paintbox.binding.FloatVar


class CardStack(val cardList: MutableList<Card>) {
    
    val x: FloatVar = FloatVar(0f)
    val y: FloatVar = FloatVar(0f)
    val flippedOver: BooleanVar = BooleanVar(false)
    
}