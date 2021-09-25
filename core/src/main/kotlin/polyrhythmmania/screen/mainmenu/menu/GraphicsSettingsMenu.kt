package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import paintbox.binding.Var
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.CheckBox
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.control.Slider
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.WindowSize
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.Settings
import polyrhythmmania.ui.PRManiaSkins
import polyrhythmmania.world.render.ForceTexturePack
import kotlin.math.roundToInt


class GraphicsSettingsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    private val settings: Settings = menuCol.main.settings

    init {
        this.setSize(MMMenu.WIDTH_MEDIUM)
        this.titleText.bind { Localization.getVar("mainMenu.videoSettings.title").use() }
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
            val (flipPane, flipCheck) = createCheckboxOption({ Localization.getVar("mainMenu.graphicsSettings.mainMenuFlipAnimation").use() })
            flipCheck.selectedState.set(main.settings.mainMenuFlipAnimation.getOrCompute())
            flipCheck.onCheckChanged = {
                main.settings.mainMenuFlipAnimation.set(it)
            }
            vbox += flipPane

            val (forceTPPane, forceTPCombobox) = createComboboxOption(ForceTexturePack.VALUES, main.settings.forceTexturePack.getOrCompute(),
                    { Localization.getVar("mainMenu.graphicsSettings.forceTexturePack").use() },
                    percentageContent = 0.4f, itemToString = { choice ->
                Localization.getValue("mainMenu.graphicsSettings.forceTexturePack.${
                    when (choice) {
                        ForceTexturePack.NO_FORCE -> "noForce"
                        ForceTexturePack.FORCE_GBA -> "forceGBA"
                        ForceTexturePack.FORCE_HD -> "forceHD"
                    }
                }")
            })
            forceTPCombobox.selectedItem.addListener { 
                main.settings.forceTexturePack.set(it.getOrCompute())
            }
            forceTPPane.label.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.graphicsSettings.forceTexturePack.tooltip")))
            vbox += forceTPPane
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