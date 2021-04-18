package polyrhythmmania.editor.pane

import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.control.ButtonSkin


class Toolbar : Pane() {
    
    init {
        this += Button("New").apply {
            this.bounds.width.set(32f)
            Anchor.TopLeft.configure(this, offsetX = 0f)
//            (this.skin as ButtonSkin).roundedRadius.set(0)
            (this.skin as ButtonSkin).let { skin ->
                skin.roundedCorners.remove(ButtonSkin.Corner.BOTTOM_RIGHT)
                skin.roundedCorners.remove(ButtonSkin.Corner.TOP_RIGHT)
            }
        }
        this += Button("Open").apply {
            this.bounds.width.set(32f)
            Anchor.TopLeft.configure(this, offsetX = 32f + 4f * 0)
//            (this.skin as ButtonSkin).roundedRadius.set(0)
            (this.skin as ButtonSkin).let { skin ->
                skin.roundedCorners.clear()
            }
        }
        this += Button("Save").apply {
            this.bounds.width.set(32f)
            Anchor.TopLeft.configure(this, offsetX = 32f * 2 + 4f * 2 * 0)
//            (this.skin as ButtonSkin).roundedRadius.set(0)
            (this.skin as ButtonSkin).let { skin ->
                skin.roundedCorners.remove(ButtonSkin.Corner.BOTTOM_LEFT)
                skin.roundedCorners.remove(ButtonSkin.Corner.TOP_LEFT)
            }
        }
    }
}