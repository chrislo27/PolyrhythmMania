package io.github.chrislo27.paintbox.font

import com.badlogic.gdx.utils.Align


enum class TextAlign {
    
    LEFT, RIGHT, CENTRE;
    
    companion object {
        fun fromInt(alignInt: Int): TextAlign {
            return when {
                Align.isLeft(alignInt) -> LEFT
                Align.isRight(alignInt) -> RIGHT
                else -> CENTRE
            }
        }
    }
    
}