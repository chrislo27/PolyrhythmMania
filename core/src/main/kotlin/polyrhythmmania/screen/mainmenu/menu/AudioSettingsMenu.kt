package polyrhythmmania.screen.mainmenu.menu

import paintbox.binding.Var
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.Slider
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.Settings


class AudioSettingsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    private val settings: Settings = menuCol.main.settings
    
    val gameplayVolSlider: Slider = Slider().apply { 
        this.bindWidthToParent(multiplier = 0.85f)
        this.minimum.set(0f)
        this.maximum.set(100f)
        this.tickUnit.set(5f)
        this.setValue(settings.gameplayVolume.getOrCompute().toFloat())
        this.value.addListener { v ->
            settings.gameplayVolume.set(v.getOrCompute().toInt())
        }
    }
    val menuMusicVolSlider: Slider = Slider().apply { 
        this.bindWidthToParent(multiplier = 0.85f)
        this.minimum.set(0f)
        this.maximum.set(100f)
        this.tickUnit.set(5f)
        this.setValue(settings.menuMusicVolume.getOrCompute().toFloat())
        this.value.addListener { v ->
            settings.menuMusicVolume.set(v.getOrCompute().toInt())
        }
    }
    val menuSfxVolSlider: Slider = Slider().apply { 
        this.bindWidthToParent(multiplier = 0.85f)
        this.minimum.set(0f)
        this.maximum.set(100f)
        this.tickUnit.set(5f)
        this.setValue(settings.menuSfxVolume.getOrCompute().toFloat())
        this.value.addListener { v ->
            settings.menuSfxVolume.set(v.getOrCompute().toInt())
        }
    }

    init {
        this.setSize(MMMenu.WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.audioSettings.title").use() }
        this.contentPane.bounds.height.set(300f)
        
        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
            this.bindHeightToParent(-40f)
        }
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }

        contentPane.addChild(vbox)
        contentPane.addChild(hbox)

        vbox.temporarilyDisableLayouts {
            vbox += createSliderPane(gameplayVolSlider) { Localization.getVar("mainMenu.audioSettings.gameplayVol").use() }.apply {
            }
            vbox += createSliderPane(menuMusicVolSlider) { Localization.getVar("mainMenu.audioSettings.menuMusicVol").use() }.apply {
            }
            vbox += createSliderPane(menuSfxVolSlider) { Localization.getVar("mainMenu.audioSettings.menuSfxVol").use() }.apply {
            }
        }

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
        }
    }

    private fun createSliderPane(slider: Slider, labelText: Var.Context.() -> String): SettingsOptionPane {
        return createSettingsOption(labelText).apply { 
            this.content.addChild(slider)
            Anchor.CentreRight.configure(slider)
        }
    }

}