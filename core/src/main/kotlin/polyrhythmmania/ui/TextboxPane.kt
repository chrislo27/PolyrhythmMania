package polyrhythmmania.ui

import com.badlogic.gdx.graphics.Texture
import paintbox.registry.AssetRegistry


class TextboxPane : NinepatchPane() {
    
    companion object {
        val DEFAULT_TEXTURE_TO_USE: () -> Texture = { AssetRegistry["ui_rounded_textbox"] }
    }
    
    init {
        this.textureToUse = DEFAULT_TEXTURE_TO_USE
    }
}
