package polyrhythmmania.screen.mainmenu.menu

import paintbox.Paintbox
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.Settings


class SettingsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    private val settings: Settings = menuCol.main.settings
    
    init {
        this.setSize(MMMenu.WIDTH_EXTRA_SMALL)
        this.titleText.bind { Localization.getVar("mainMenu.settings.title").use() }
        this.contentPane.bounds.height.set(250f)
        
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
            vbox += createLongButton { Localization.getVar("mainMenu.settings.audio").use() }.apply {
                this.setOnAction { 
                    menuCol.pushNextMenu(menuCol.audioSettingsMenu)
                }
            }
//            vbox += createLongButton { Localization.getVar("mainMenu.settings.video").use() }.apply {
//            }
            vbox += createLongButton { Localization.getVar("mainMenu.settings.input").use() }.apply { 
                this.disabled.set(true)
            }
        }

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                    settings.persist()
                    Paintbox.LOGGER.info("Settings persisted")
                }
            }
        }
    }
}