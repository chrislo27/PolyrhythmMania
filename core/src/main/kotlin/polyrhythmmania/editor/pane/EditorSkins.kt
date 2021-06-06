package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.ui.control.Button
import paintbox.ui.control.ButtonSkin
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.SkinFactory


object EditorSkins {
    
    val BUTTON: String = "Button_noCorners"
    val BUTTON_NO_SKIN: String = "Button_noSkin"
    
    init {
        DefaultSkins.register(BUTTON, SkinFactory { element: Button ->
            ButtonSkin(element).apply { 
                this.roundedCorners.clear()
                this.roundedRadius.set(0)
            }
        })
        DefaultSkins.register(BUTTON_NO_SKIN, SkinFactory { element: Button ->
            ButtonSkinNoOp(element)
        })
    }
    
    private class ButtonSkinNoOp(element: Button) : ButtonSkin(element) {
        init {
            this.roundedRadius.set(0)
            this.roundedCorners.clear()
        }

        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        }

        override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        }
    }
    
}