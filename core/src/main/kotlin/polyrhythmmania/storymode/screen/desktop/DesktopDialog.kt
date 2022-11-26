package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.graphics.Color
import paintbox.ui.Anchor
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.element.RectElement
import paintbox.util.gdxutils.grey
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.screen.desktop.DesktopUI.Companion.UI_SCALE
import polyrhythmmania.ui.DialogPane


open class DesktopDialog(val desktopUI: DesktopUI) : DialogPane() {
    
    protected val main: PRManiaGame = desktopUI.main
    protected val mainPane: UIElement
    
    init {
        val backOverlay = RectElement(Color().grey(0.1f, 0.8f))
        this.addChild(backOverlay)

        mainPane = DesktopStyledPane().apply {
            Anchor.Centre.configure(this)
            this.textureToUse = DesktopStyledPane.DEFAULT_TEXTURE_TO_USE_DARK
            this.bounds.width.set(90f * UI_SCALE)
            this.bounds.height.set(100f * UI_SCALE)
            this.padding.set(Insets(8f * UI_SCALE, 6f * UI_SCALE, 6f * UI_SCALE, 6f * UI_SCALE))
        }
        backOverlay.addChild(mainPane)
    }
    
    
    fun attemptClose() {
        if (canCloseDialog()) {
            onCloseDialog()
            desktopUI.dialogHandler.closeDialog()
            afterDialogClosed()
        }
    }

    /**
     * Returns false if we cannot close the dialog.
     */
    protected open fun canCloseDialog(): Boolean = true

    /**
     * Overridden by subclasses to do close cleanup.
     */
    protected open fun onCloseDialog() {
    }

    protected open fun afterDialogClosed() {
    }

    open fun canCloseWithEscKey(): Boolean = true
}
