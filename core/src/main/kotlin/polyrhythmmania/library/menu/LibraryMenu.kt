package polyrhythmmania.library.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.TinyFDWrapper
import paintbox.util.gdxutils.openFileExplorer
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.screen.mainmenu.menu.LoadSavedLevelMenu
import polyrhythmmania.screen.mainmenu.menu.MenuCollection
import polyrhythmmania.screen.mainmenu.menu.StandardMenu
import polyrhythmmania.ui.PRManiaSkins
import java.io.File
import kotlin.concurrent.thread


class LibraryMenu(menuCol: MenuCollection) : StandardMenu(menuCol)  {

    init {
        this.setSize(WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.library.title").use() }
        this.showLogo.set(false)
        this.contentPane.bounds.height.set(520f)

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
            vbox += createLongButton { Localization.getVar("mainMenu.play.playSavedLevel").use() }.apply {
                this.setOnAction {
                    val loadMenu = LoadSavedLevelMenu(menuCol)
                    menuCol.addMenu(loadMenu)
                    menuCol.pushNextMenu(loadMenu)
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
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.library.sortAndFilter").use() }).apply {
                this.bounds.width.set(170f)
                this.setOnAction {
                    // TODO
                }
            }
            hbox += createSmallButton(binding = {""}).apply {
                this.bindWidthToSelfHeight()
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_open"]))
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.library.openLibraryLocation.tooltip")))
                this.setOnAction {
                    Gdx.net.openFileExplorer(main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_LIBRARY_VIEW) ?: run {
                        PRMania.DEFAULT_LEVELS_FOLDER.also { it.mkdirs() }
                    })
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.library.changeLibraryLocation").use() }).apply {
                this.bounds.width.set(160f)
                this.setOnAction {
                    // TODO
                    main.restoreForExternalDialog { completionCallback ->
                        thread(isDaemon = true) {
                            TinyFDWrapper.selectFolder("title", File("~")) { file: File? ->
                                completionCallback()
                                println("Selected: $file")
                            }
                        }
                    }
                }
            }
        }
    }
}