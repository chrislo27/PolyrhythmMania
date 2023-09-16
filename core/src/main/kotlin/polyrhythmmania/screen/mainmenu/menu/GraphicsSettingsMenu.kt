package polyrhythmmania.screen.mainmenu.menu

import paintbox.binding.Var
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.Slider
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.ui.PRManiaSkins
import polyrhythmmania.world.render.ForceSignLanguage
import polyrhythmmania.world.render.ForceTexturePack
import polyrhythmmania.world.render.ForceTilesetPalette
import kotlin.math.roundToInt


class GraphicsSettingsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    private val settings: Settings = menuCol.main.settings
    
    private val subtitleOpacitySlider: Slider = Slider().apply {
        this.bindWidthToParent(multiplier = 0.85f)
        this.minimum.set(0f)
        this.maximum.set(100f)
        this.tickUnit.set(10f)
        this.setValue(settings.subtitleOpacity.getOrCompute().toFloat())
        this.value.addListener { v ->
            settings.subtitleOpacity.set(v.getOrCompute().toInt())
        }
    }

    init {
        this.setSize(MMMenu.WIDTH_MEDIUM)
        this.titleText.bind { Localization.getVar("mainMenu.graphicsSettings.title").use() }
        this.contentPane.bounds.height.set(300f)

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

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
            this.padding.set(Insets(4f, 0f, 2f, 2f))
            this.bounds.height.set(40f)
        }

        contentPane.addChild(scrollPane)
        contentPane.addChild(hbox)
        
        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
            this.bindHeightToParent(-40f)
            this.margin.set(Insets(0f, 0f, 0f, 4f))
        }
        

        vbox.temporarilyDisableLayouts {
            val (flipPane, flipCheck) = createCheckboxOption({ Localization.getVar("mainMenu.graphicsSettings.mainMenuFlipAnimation").use() })
            flipCheck.selectedState.set(settings.mainMenuFlipAnimation.getOrCompute())
            flipCheck.onCheckChanged = {
                settings.mainMenuFlipAnimation.set(it)
            }
            vbox += flipPane
            
            val (achvNotifPane, achvNotifCheck) = createCheckboxOption({ Localization.getVar("mainMenu.graphicsSettings.achievementNotifs").use() })
            achvNotifCheck.selectedState.set(settings.achievementNotifications.getOrCompute())
            achvNotifCheck.onCheckChanged = {
                settings.achievementNotifications.set(it)
            }
            vbox += achvNotifPane

            val (forceTexPackPane, forceTexPackCombobox) = createComboboxOption(ForceTexturePack.entries, settings.forceTexturePack.getOrCompute(),
                    { Localization.getVar("mainMenu.graphicsSettings.forceTexturePack").use() },
                    percentageContent = 0.4f, itemToString = { choice ->
                Localization.getValue("mainMenu.graphicsSettings.forceTexturePack.${
                    when (choice) {
                        ForceTexturePack.NO_FORCE -> "noForce"
                        ForceTexturePack.FORCE_GBA -> "forceGBA"
                        ForceTexturePack.FORCE_HD -> "forceHD"
                        ForceTexturePack.FORCE_ARCADE -> "forceArcade"
                    }
                }")
            })
            forceTexPackCombobox.setScaleXY(0.75f)
            forceTexPackCombobox.selectedItem.addListener { 
                settings.forceTexturePack.set(it.getOrCompute())
            }
            forceTexPackPane.label.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.graphicsSettings.forceTexturePack.tooltip")))
            vbox += forceTexPackPane

            val (forcePalettePane, forcePaletteCombobox) = createComboboxOption(ForceTilesetPalette.entries, settings.forceTilesetPalette.getOrCompute(),
                    { Localization.getVar("mainMenu.graphicsSettings.forceTilesetPalette").use() },
                    percentageContent = 0.4f, itemToString = { choice ->
                Localization.getValue("mainMenu.graphicsSettings.forceTilesetPalette.${
                    when (choice) {
                        ForceTilesetPalette.NO_FORCE -> "noForce"
                        ForceTilesetPalette.FORCE_PR1 -> "redGreen"
                        ForceTilesetPalette.FORCE_PR2 -> "redBlue"
                        ForceTilesetPalette.ORANGE_BLUE -> "orangeBlue"
                    }
                }")
            })
            forcePaletteCombobox.setScaleXY(0.75f)
            forcePaletteCombobox.selectedItem.addListener { 
                settings.forceTilesetPalette.set(it.getOrCompute())
            }
            forcePalettePane.label.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.graphicsSettings.forceTilesetPalette.tooltip")))
            vbox += forcePalettePane

            val (reducedMotionPane, reducedMotionCheck) = createCheckboxOption({ Localization.getVar("mainMenu.graphicsSettings.reducedMotion").use() })
            reducedMotionCheck.selectedState.set(settings.reducedMotion.getOrCompute())
            reducedMotionCheck.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.graphicsSettings.reducedMotion.tooltip")))
            reducedMotionCheck.onCheckChanged = {
                settings.reducedMotion.set(it)
            }
            vbox += reducedMotionPane

            val (disableSpotlightsPane, disableSpotlightsCheck) = createCheckboxOption({ Localization.getVar("mainMenu.graphicsSettings.disableSpotlights").use() })
            disableSpotlightsCheck.selectedState.set(settings.disableSpotlights.getOrCompute())
            disableSpotlightsCheck.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.graphicsSettings.disableSpotlights.tooltip")))
            disableSpotlightsCheck.onCheckChanged = {
                settings.disableSpotlights.set(it)
            }
            vbox += disableSpotlightsPane
            
            vbox += createSliderPane(subtitleOpacitySlider, percentageContent = 0.6f) {
                Localization.getVar("mainMenu.graphicsSettings.subtitleOpacity", Var { 
                    listOf(subtitleOpacitySlider.value.use().roundToInt().toString())
                }).use()
            }.apply { 
                this.label.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.graphicsSettings.subtitleOpacity.tooltip")))
            }
            
            val (forceSignLanguagePane, forceSignLanguageCombobox) = createComboboxOption(ForceSignLanguage.entries, settings.forceSignLanguage.getOrCompute(),
                { Localization.getVar("mainMenu.graphicsSettings.forceSignLanguage").use() },
                percentageContent = 0.4f, itemToString = { choice ->
                    Localization.getValue("mainMenu.graphicsSettings.forceSignLanguage.${
                        when (choice) {
                            ForceSignLanguage.NO_FORCE -> "noForce"
                            ForceSignLanguage.FORCE_JAPANESE -> "forceJapanese"
                            ForceSignLanguage.FORCE_ENGLISH -> "forceEnglish"
                        }
                    }")
                })
            forceSignLanguageCombobox.setScaleXY(0.75f)
            forceSignLanguageCombobox.selectedItem.addListener {
                settings.forceSignLanguage.set(it.getOrCompute())
            }
            forceSignLanguagePane.label.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.graphicsSettings.forceSignLanguage.tooltip")))
            vbox += forceSignLanguagePane
        }

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
        }
        vbox.sizeHeightToChildren(100f)
        scrollPane.setContent(vbox)
        
    }

}