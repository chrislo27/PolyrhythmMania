package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import paintbox.binding.Var
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.Slider
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.screen.CalibrationScreen
import polyrhythmmania.ui.PRManiaSkins


class CalibrationSettingsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    private val settings: Settings = menuCol.main.settings

    val manualOffsetSlider: Slider = Slider().apply {
        this.minimum.set(-500f)
        this.maximum.set(500f)
        this.tickUnit.set(1f)
        this.setValue(settings.calibrationAudioOffsetMs.getOrCompute().toFloat())
        this.value.addListener { v ->
            settings.calibrationAudioOffsetMs.set(v.getOrCompute().toInt())
        }
    }
    
    init {
        this.setSize(MMMenu.WIDTH_MEDIUM)
        this.titleText.bind { Localization.getVar("mainMenu.calibration").use() }
        this.contentPane.bounds.height.set(300f)

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)
        }
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(4f, 0f, 2f, 2f))
            this.bounds.height.set(40f)
        }
        contentPane.addChild(scrollPane)
        contentPane.addChild(hbox)

        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.height.set(300f)
            this.spacing.set(0f)
        }

        vbox.temporarilyDisableLayouts {
            vbox += createSettingsOption({
                Localization.getVar("mainMenu.calibration.audioOffset", Var.bind {listOf(manualOffsetSlider.value.use().toInt())}).use()
            }, percentageContent = 1f, twoRowsTall = true).apply {
                val slider = manualOffsetSlider
                this.content.addChild(slider)
                Anchor.BottomCentre.configure(slider)
                this.label.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.calibration.audioOffset.tooltip")))
            }

            vbox += createLongButton { Localization.getVar("mainMenu.calibration.test").use() }.apply {
                this.setOnAction {
                    menuCol.playMenuSound("sfx_menu_select")
                    Gdx.app.postRunnable {
                        val playScreen = CalibrationScreen(main, main.settings.inputCalibration.getOrCompute(), main.settings.inputKeymapKeyboard.getOrCompute().buttonA)
                        main.screen = TransitionScreen(main, main.screen, playScreen,
                                FadeToOpaque(0.25f, Color(0f, 0f, 0f, 1f)), FadeToTransparent(0.25f, Color(0f, 0f, 0f, 1f))).apply {
                            this.onEntryEnd = {
                                playScreen.prepareShow()
                            }
                        }
                    }
                }
            }

            val (disableInputSFXPane, disableInputSFXCheck) = createCheckboxOption({ Localization.getVar("mainMenu.calibration.disableInputSounds").use() })
            disableInputSFXCheck.checkedState.set(settings.calibrationDisableInputSFX.getOrCompute())
            disableInputSFXCheck.onCheckChanged = { newState ->
                settings.calibrationDisableInputSFX.set(newState)
            }
            disableInputSFXCheck.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.calibration.disableInputSounds.tooltip")))
            vbox += disableInputSFXPane
        }
        vbox.sizeHeightToChildren(100f)
        scrollPane.setContent(vbox)

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.calibration.reset").use() }).apply {
                this.bounds.width.set(250f)
                this.setOnAction {
                    manualOffsetSlider.setValue(0f)
                }
            }
        }
    }

}