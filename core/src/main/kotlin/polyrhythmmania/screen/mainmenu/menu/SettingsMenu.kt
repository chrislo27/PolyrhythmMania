package polyrhythmmania.screen.mainmenu.menu

import paintbox.Paintbox
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollPane
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.ui.PRManiaSkins


class SettingsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    private val settings: Settings = menuCol.main.settings
    
    init {
        this.setSize(0.35f)
        this.titleText.bind { Localization.getVar("mainMenu.settings.title").use() }
        this.contentPane.bounds.height.set(320f)
        
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
        }
        vbox.temporarilyDisableLayouts {
            vbox += createLongButton { Localization.getVar("mainMenu.settings.graphics").use() }.apply {
                this.setOnAction { 
                    menuCol.pushNextMenu(menuCol.graphicsSettingsMenu)
                }
            }
            vbox += createLongButton { Localization.getVar("mainMenu.settings.audio").use() }.apply {
                this.setOnAction { 
                    val audioSettingsMenu = AudioSettingsMenu(menuCol)
                    menuCol.addMenu(audioSettingsMenu)
                    menuCol.pushNextMenu(audioSettingsMenu)
                }
            }
            vbox += createLongButton { Localization.getVar("mainMenu.settings.video").use() }.apply {
                this.setOnAction {
                    val menu = menuCol.videoSettingsMenu
                    menu.prepareShow()
                    menuCol.pushNextMenu(menu)
                }
            }
            vbox += createLongButton { Localization.getVar("mainMenu.settings.input").use() }.apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.inputSettingsMenu)
                }
            }
            vbox += createLongButton { Localization.getVar("mainMenu.settings.data").use() }.apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.dataSettingsMenu)
                }
            }
            vbox += createLongButton { Localization.getVar("mainMenu.settings.language").use() }.apply {
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.languageMenu)
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
                    settings.persist()
                    Paintbox.LOGGER.info("Settings persisted")
                }
            }
        }
    }
}