package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.graphics.Color
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.control.Slider
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.ui.PRManiaSkins


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
        this.setSize(MMMenu.WIDTH_MEDIUM)
        this.titleText.bind { Localization.getVar("mainMenu.audioSettings.title").use() }
        this.contentPane.bounds.height.set(300f)
        
        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)

            this.vBar.unitIncrement.set(10f)
            this.vBar.blockIncrement.set(40f)
        }
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }

        contentPane.addChild(scrollPane)
        contentPane.addChild(hbox)


        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
            this.bindHeightToParent(-40f)
        }
        vbox.temporarilyDisableLayouts {
            vbox += createSliderPane(gameplayVolSlider, percentageContent = 0.6f) { Localization.getVar("mainMenu.audioSettings.gameplayVol").use() }
            vbox += createSliderPane(menuMusicVolSlider, percentageContent = 0.6f) { Localization.getVar("mainMenu.audioSettings.menuMusicVol").use() }
            vbox += createSliderPane(menuSfxVolSlider, percentageContent = 0.6f) { Localization.getVar("mainMenu.audioSettings.menuSfxVol").use() }
            vbox += createLongButton { Localization.getVar("mainMenu.audioSettings.goToCalibration").use() }.apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.calibrationSettingsMenu)
                }
            }
            vbox += createLongButton { Localization.getVar("mainMenu.audioSettings.advancedAudioSettings").use() }.apply { 
                this.setOnAction { 
                    menuCol.pushNextMenu(menuCol.advancedAudioMenu)
                }
            }
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
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.audioSettings.resetLevels").use() }).apply {
                this.bounds.width.set(300f)
                this.setOnAction {
                    listOf(gameplayVolSlider, menuMusicVolSlider, menuSfxVolSlider).forEach { 
                        it.setValue(50f)
                    }
                }
            }
        }
    }

}