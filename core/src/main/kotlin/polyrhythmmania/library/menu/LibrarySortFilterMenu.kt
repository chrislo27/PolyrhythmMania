package polyrhythmmania.library.menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.control.*
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.screen.mainmenu.menu.MMMenu
import polyrhythmmania.screen.mainmenu.menu.MenuCollection
import polyrhythmmania.screen.mainmenu.menu.StandardMenu
import polyrhythmmania.ui.PRManiaSkins


class LibrarySortFilterMenu(menuCol: MenuCollection, val library: LibraryMenu)
    : StandardMenu(menuCol) {
    
    private val oldSettings: LibrarySortFilter = library.sortFilter.getOrCompute()
    
    private var currentSettings: Var<LibrarySortFilter> = Var(oldSettings.copy())
    
    init {
        this.setSize(MMMenu.WIDTH_LARGE)
        this.titleText.bind { Localization.getVar("mainMenu.library.sortAndFilter").use() }
        this.showLogo.set(false)
        this.contentPane.bounds.height.set(520f)
        this.deleteWhenPopped.set(true)

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)

            this.vBar.unitIncrement.set(48f)
            this.vBar.blockIncrement.set(48f * 4)
        }
        contentPane += scrollPane
        val leftBottomHbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }
        contentPane += leftBottomHbox

        val vbox = VBox().apply {
            this.spacing.set(4f)
            this.bounds.height.set(100f)
            this.margin.set(Insets(0f, 0f, 0f, 2f))
        }
        
        vbox.temporarilyDisableLayouts {
            vbox += TextLabel(binding = { Localization.getVar("mainMenu.librarySortFilter.desc").use() }, font = main.fontMainMenuThin).apply {
                this.bounds.height.set(72f)
                this.margin.set(Insets(2f, 2f, 0f, 0f))
                this.renderAlign.set(Align.topLeft)
                this.doLineWrapping.set(true)
            }
            
            // Sort half
            vbox += HBox().apply {
                this.bounds.height.set(64f)
                this.spacing.set(4f)
                this += TextLabel(binding = { Localization.getVar("mainMenu.librarySortFilter.sort").use() }, font = main.fontMainMenuHeading).apply {
                    this.bindWidthToParent(multiplier = 0.25f)
                }
                this += TextLabel(binding = { Localization.getVar("mainMenu.librarySortFilter.sort.sortOrder").use() }, font = main.fontMainMenuMain).apply {
                    this.bindWidthToParent(multiplier = 0.15f)
                    this.renderAlign.set(Align.right)
                    this.margin.set(Insets(0f, 0f, 16f, 8f))
                }
                val sortToggleGroup = ToggleGroup()
                this += Pane().apply {
                    this.bindWidthToParent(multiplier = 0.25f)
                    this += RadioButton(binding = { Localization.getVar("mainMenu.librarySortFilter.sort.sortAscending").use() }, font = main.fontMainMenuThin).apply {
                        Anchor.TopLeft.configure(this)
                        sortToggleGroup.addToggle(this)
                        this.bindHeightToParent(multiplier = 0.5f)
                        this.onSelected = {
                            currentSettings.set(currentSettings.getOrCompute().copy(sortDescending = false))
                        }
                        if (!oldSettings.sortDescending) {
                            this.selectedState.set(true)
                        }
                    }
                    this += RadioButton(binding = { Localization.getVar("mainMenu.librarySortFilter.sort.sortDescending").use() }, font = main.fontMainMenuThin).apply {
                        Anchor.BottomLeft.configure(this)
                        sortToggleGroup.addToggle(this)
                        this.bindHeightToParent(multiplier = 0.5f)
                        this.onSelected = {
                            currentSettings.set(currentSettings.getOrCompute().copy(sortDescending = true))
                        }
                        if (oldSettings.sortDescending) {
                            this.selectedState.set(true)
                        }
                    }
                }
                this += CheckBox(binding = { Localization.getVar("mainMenu.librarySortFilter.sort.legacyOnTop").use() }, font = main.fontMainMenuThin).apply {
                    Anchor.CentreLeft.configure(this)
                    this.bounds.height.set(32f)
                    this.bindWidthToParent(multiplier = 0.325f)
                    this.selectedState.set(oldSettings.legacyOnTop)
                    this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.librarySortFilter.sort.legacyOnTop.tooltip")))
                    this.onCheckChanged = {
                        currentSettings.set(currentSettings.getOrCompute().copy(legacyOnTop = it))
                    }
                }
            }
            val sortables = Sortable.VALUES
            vbox += Pane().apply {
                this.bounds.height.set(32f * (sortables.size / 3 + 1))
                val toggleGroup = ToggleGroup()
                sortables.forEachIndexed { index, sortable -> 
                    this += RadioButton(binding = {Localization.getVar(sortable.nameKey).use()}, font = main.fontMainMenuThin).apply { 
                        this.bounds.height.set(32f)
                        this.bindWidthToParent(multiplier = 0.333f, adjust = -4f)
                        this.bounds.x.bind { (parent.use()?.bounds?.width?.useF() ?: 0f) * ((index % 3) / 3f) }
                        this.bounds.y.set(32f * (index / 3))
                        this.margin.set(Insets(0f, 0f, 0f, 4f))
                        toggleGroup.addToggle(this)
                        if (oldSettings.sortOn == sortable) {
                            this.selectedState.set(true)
                        }
                        this.onSelected = {
                            currentSettings.set(currentSettings.getOrCompute().copy(sortOn = sortable))
                        }
                    }
                }
            }

            // Filter half
            vbox += HBox().apply {
                this.bounds.height.set(64f + 8f)
                this.spacing.set(4f)
                this.margin.set(Insets(8f, 0f, 0f, 0f))
                this += TextLabel(binding = { Localization.getVar("mainMenu.librarySortFilter.filter").use() }, font = main.fontMainMenuHeading).apply {
                    this.bindWidthToParent(multiplier = 0.3f)
                }
            }
        }
        vbox.sizeHeightToChildren(100f)
        scrollPane.setContent(vbox)

        leftBottomHbox.temporarilyDisableLayouts {
            leftBottomHbox += createSmallButton(binding = { Localization.getVar("common.cancel").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu(instant = true, playSound = true, backOut = true)
                }
            }
            leftBottomHbox += createSmallButton(binding = { Localization.getVar("mainMenu.librarySortFilter.apply").use() }).apply {
                this.bounds.width.set(200f)
                this.setOnAction {
                    menuCol.popLastMenu(instant = true, playSound = true, backOut = false)
                    library.sortFilter.set(currentSettings.getOrCompute())
                    library.filterAndSortLevelList()
                }
            }
            leftBottomHbox += createSmallButton(binding = { Localization.getVar("mainMenu.librarySortFilter.resetDefault").use() }).apply {
                this.bounds.width.set(250f)
                this.setOnAction {
                    menuCol.popLastMenu(instant = true, playSound = true, backOut = false)
                    library.sortFilter.set(LibrarySortFilter.DEFAULT)
                    library.filterAndSortLevelList()
                }
            }
        }
    }
    
}