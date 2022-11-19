package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.graphics.Texture
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.ui.NinepatchPane


class DesktopStyledPane : NinepatchPane() {
    companion object {
        val DEFAULT_TEXTURE_TO_USE: () -> Texture = { StoryAssets["desk_ui_pane"] }
    }
    
    init {
        this.textureToUse = DEFAULT_TEXTURE_TO_USE
        this.cornerSize.set(8 * DesktopUI.UI_SCALE)
    }
}