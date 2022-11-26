package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.math.Interpolation
import paintbox.binding.FloatVar
import paintbox.ui.SceneRoot
import paintbox.ui.UIElement
import paintbox.ui.animation.Animation
import polyrhythmmania.ui.DialogPane


class DesktopDialogHandler(val desktopUI: DesktopUI) {
    
    private val sceneRoot: SceneRoot = desktopUI.sceneRoot

    fun isDialogOpen(): Boolean = sceneRoot.isDialogActive()
    fun getActiveDialog(): UIElement? = sceneRoot.getCurrentRootDialog()

    fun closeDialog() {
        val root = sceneRoot
        root.hideRootContextMenu()
        root.hideRootDialog()
        root.hideDropdownContextMenu()
    }

    fun openDialog(dialog: DialogPane) {
        closeDialog()
        sceneRoot.showRootDialog(dialog)
        enqueueAnimation(dialog.opacity, 0f, 1f, 0.15f).apply {
            onStart = { dialog.visible.set(true) }
        }
    }

    private fun enqueueAnimation(animation: Animation, varr: FloatVar) {
        sceneRoot.animations.enqueueAnimation(animation, varr)
    }

    private fun enqueueAnimation(varr: FloatVar, start: Float, end: Float, duration: Float,
                                 interpolation: Interpolation = Interpolation.smoother): Animation {
        val animation = Animation(interpolation, duration, start, end)
        enqueueAnimation(animation, varr)
        return animation
    }
}
