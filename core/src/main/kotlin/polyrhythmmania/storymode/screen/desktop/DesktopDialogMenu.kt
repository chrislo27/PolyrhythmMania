package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import paintbox.binding.BooleanVar
import paintbox.binding.FloatVar
import paintbox.binding.Var
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import paintbox.ui.RenderAlign
import paintbox.ui.UIElement
import paintbox.ui.animation.Animation
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.Slider
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.screen.desktop.DesktopUI.Companion.UI_SCALE
import polyrhythmmania.ui.PRManiaSkins


class DesktopDialogMenu(desktopUI: DesktopUI) : DesktopDialog(desktopUI) {
    
    companion object {
        private const val MAIN_PANE_HEIGHT_COLLAPSED: Float = 78f
        private const val MAIN_PANE_HEIGHT_FULL: Float = 150f
    }
    
    private val resetVolumesToggle: BooleanVar = BooleanVar(false)
    
    private val mainPaneHeightUnits: FloatVar = FloatVar(MAIN_PANE_HEIGHT_COLLAPSED)
    
    init {
        val settings = main.settings
        val robotoRegularMarkup = desktopUI.robotoRegularMarkup
        val robotoBoldMarkup = desktopUI.robotoBoldMarkup
        
        this.mainPane.apply {
            this.bounds.height.bind { mainPaneHeightUnits.use() * UI_SCALE }
        }
        this.mainPane += VBox().apply {
            fun separator(): UIElement = RectElement(Color().grey(0.8f)).apply {
                this.bounds.height.set(0.5f * UI_SCALE)
            }
            
            this.doClipping.set(true)
            this.spacing.set(3f * UI_SCALE)

            this += TextLabel(StoryL10N.getVar("desktop.menu"), font = main.fontMainMenuHeading).apply {
                this.bounds.height.set(10f * UI_SCALE)
                this.textColor.set(Color.WHITE)
                this.renderAlign.set(RenderAlign.center)
                this.doLineWrapping.set(true)
                this.margin.set(Insets(1f, 1f, 4f, 4f))
            }
            
            this += separator()

            this += Button(Localization.getVar("common.close")).apply {
                this.skinID.set(PRManiaSkins.BUTTON_SKIN_STORY_LIGHT)
                this.markup.set(robotoRegularMarkup)
                this.bounds.height.set(14f * UI_SCALE)
                this.setOnAction {
                    attemptClose()
                }
            }
            this += Button(StoryL10N.getVar("desktop.menu.quitToTitle")).apply {
                this.skinID.set(PRManiaSkins.BUTTON_SKIN_STORY_LIGHT)
                this.markup.set(robotoRegularMarkup)
                this.bounds.height.set(14f * UI_SCALE)
                this.setOnAction {
                    desktopUI.controller.playSFX(DesktopController.SFXType.PAUSE_EXIT)
                    removeBandpassFromMusic()

                    val storySession = desktopUI.storySession
                    storySession.attemptSave()
                    storySession.stopUsingSavefile()
                    storySession.musicHandler.transitionToTitleMix()
                    
                    val nextScreen = desktopUI.rootScreen.prevScreen()
                    main.screen = TransitionScreen(main, main.screen, nextScreen,
                            FadeToOpaque(0.25f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                }
            }

            this += separator()
            
            // Audio settings
            fun addAudioSettings() {
                this += TextLabel(StoryL10N.getVar("desktop.menu.settings.audio"), font = main.fontMainMenuHeading).apply {
                    this.bounds.height.set(7f * UI_SCALE)
                    this.textColor.set(Color.WHITE)
                    this.renderAlign.set(RenderAlign.center)
                    this.margin.set(Insets(1f, 1f, 4f, 4f))
                    this.setScaleXY(0.625f)
                }
                fun createVolumeSlider(nameKey: String, setting: Var<Int>, resetValue: Int) {
                    this += VBox().apply {
                        this.bounds.height.set(13f * UI_SCALE)
                        this.spacing.set(0.5f * UI_SCALE)

                        this.temporarilyDisableLayouts {
                            val slider = Slider().apply {
                                this.bounds.height.set(6f * UI_SCALE)
                                this.minimum.set(0f)
                                this.maximum.set(100f)
                                this.tickUnit.set(5f)
                                this.setValue(setting.getOrCompute().toFloat())
                                this.value.addListener { v ->
                                    setting.set(v.getOrCompute().toInt())
                                }
                                resetVolumesToggle.addListener {
                                    this.setValue(resetValue.toFloat())
                                }
                            }
                            this += TextLabel(StoryL10N.getVar("desktop.menu.settings.audio.settingLabel", Var.bind {
                                listOf(Localization.getVar(nameKey).use(), slider.value.use().toInt())
                            })).apply {
                                this.markup.set(robotoRegularMarkup)
                                this.bounds.height.set(6f * UI_SCALE)
                                this.textColor.set(Color.WHITE)
                                this.renderAlign.set(RenderAlign.left)
                                this.margin.set(Insets(1f, 1f, 4f, 4f))
                            }
                            this += slider
                        }
                    }
                }
                createVolumeSlider("mainMenu.audioSettings.masterVol", settings.masterVolumeSetting, Settings.DEFAULT_MASTER_VOLUME)
                createVolumeSlider("mainMenu.audioSettings.gameplayVol", settings.gameplayVolumeSetting, Settings.DEFAULT_SPECIFIC_VOLUME)
                createVolumeSlider("mainMenu.audioSettings.menuMusicVol", settings.menuMusicVolumeSetting, Settings.DEFAULT_SPECIFIC_VOLUME)
                createVolumeSlider("mainMenu.audioSettings.menuSfxVol", settings.menuSfxVolumeSetting, Settings.DEFAULT_SPECIFIC_VOLUME)
                this += Button(Localization.getVar("mainMenu.audioSettings.resetLevels")).apply {
                    this.skinID.set(PRManiaSkins.BUTTON_SKIN_STORY_LIGHT)
                    this.markup.set(robotoRegularMarkup)
                    this.bounds.height.set(8f * UI_SCALE)
                    this.setScaleXY(0.9f)
                    this.setOnAction {
                        resetVolumesToggle.invert()
                        desktopUI.controller.playSFX(DesktopController.SFXType.CLICK_INBOX_ITEM)
                    }
                }
            }
            
            val vbox = this
            this += Button(StoryL10N.getVar("desktop.menu.settings.audio")).apply {
                this.skinID.set(PRManiaSkins.BUTTON_SKIN_STORY_LIGHT)
                this.markup.set(robotoRegularMarkup)
                this.bounds.height.set(10f * UI_SCALE)
                this.setOnAction {
                    desktopUI.controller.playSFX(DesktopController.SFXType.PAUSE_ENTER)
                    removeBandpassFromMusic() // Music should not be muffled while these settings are open. These settings never close, though
                    vbox.removeChild(this)
                    vbox.temporarilyDisableLayouts {
                        addAudioSettings()
                    }
                    desktopUI.sceneRoot.animations.enqueueAnimation(
                            Animation(Interpolation.pow5Out, 0.25f, MAIN_PANE_HEIGHT_COLLAPSED, MAIN_PANE_HEIGHT_FULL),
                            mainPaneHeightUnits)
                }
            }
        }
    }

    override fun onCloseDialog() {
        desktopUI.controller.playSFX(DesktopController.SFXType.PAUSE_EXIT)
        removeBandpassFromMusic()
    }
    
    private fun removeBandpassFromMusic() {
        desktopUI.storySession.musicHandler.transitionToBandpass(false)
    }
}
