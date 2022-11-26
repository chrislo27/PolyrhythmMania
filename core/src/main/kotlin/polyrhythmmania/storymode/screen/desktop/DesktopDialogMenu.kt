package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.graphics.Color
import paintbox.ui.RenderAlign
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.screen.desktop.DesktopUI.Companion.UI_SCALE
import polyrhythmmania.ui.PRManiaSkins


class DesktopDialogMenu(desktopUI: DesktopUI) : DesktopDialog(desktopUI) {
    
    init {
        val robotoRegularMarkup = desktopUI.robotoRegularMarkup
        
        this.mainPane.apply {
            this.bounds.height.set(70f * UI_SCALE)
        }
        this.mainPane += VBox().apply {
            this.spacing.set(4f * UI_SCALE)

            this += TextLabel(StoryL10N.getVar("desktop.menu"), font = main.fontMainMenuHeading).apply {
                this.bounds.height.set(10f * UI_SCALE)
                this.textColor.set(Color.WHITE)
                this.renderAlign.set(RenderAlign.center)
                this.doLineWrapping.set(true)
                this.margin.set(Insets(1f, 1f, 4f, 4f))
            }
            this += RectElement(Color().grey(0.8f)).apply {
                this.bounds.height.set(0.5f * UI_SCALE)
            }

            this += Button(Localization.getVar("common.back")).apply {
                this.skinID.set(PRManiaSkins.BUTTON_SKIN_STORY_LIGHT)
                this.markup.set(robotoRegularMarkup)
                this.bounds.height.set(16f * UI_SCALE)
                this.setOnAction {
                    attemptClose()
                }
            }
            this += Button(StoryL10N.getVar("desktop.menu.quitToTitle")).apply {
                this.skinID.set(PRManiaSkins.BUTTON_SKIN_STORY_LIGHT)
                this.markup.set(robotoRegularMarkup)
                this.bounds.height.set(16f * UI_SCALE)
                this.setOnAction {
                    desktopUI.controller.playSFX(DesktopController.SFXType.PAUSE_EXIT)
                    main.screen = desktopUI.rootScreen.prevScreen // TODO transition
                }
            }
        }
    }

    override fun onCloseDialog() {
        desktopUI.controller.playSFX(DesktopController.SFXType.PAUSE_EXIT)
    }
}
