package polyrhythmmania.library.menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.container.LevelMetadata
import polyrhythmmania.library.LevelEntry
import polyrhythmmania.screen.mainmenu.menu.MMMenu
import polyrhythmmania.screen.mainmenu.menu.MenuCollection
import polyrhythmmania.screen.mainmenu.menu.StandardMenu
import polyrhythmmania.ui.PRManiaSkins
import java.util.*


class LibrarySortFilterMenu(menuCol: MenuCollection, val library: LibraryMenu,
                            private val fullLevelList: List<LevelEntryData>,
                            private val oldSettings: LibrarySortFilter)
    : StandardMenu(menuCol) {
    
    private val currentSettings: Var<LibrarySortFilter> = Var(oldSettings.copy())
    
    init {
        this.setSize(MMMenu.WIDTH_LARGE)
        this.titleText.bind { Localization.getVar("mainMenu.library.sortAndFilter").use() }
        this.showLogo.set(false)
        this.contentPane.bounds.height.set(520f)
        this.deleteWhenPopped.set(true)

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)

            this.vBar.unitIncrement.set(24f)
            this.vBar.blockIncrement.set(24f * 4)
        }
        contentPane += scrollPane
        val leftBottomPane = Pane().apply {
            Anchor.BottomLeft.configure(this)
            this.padding.set(Insets(4f, 0f, 2f, 2f))
            this.bounds.height.set(40f)
        }
        contentPane += leftBottomPane
        val leftBottomHbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
        }
        leftBottomPane += leftBottomHbox

        val vbox = VBox().apply {
            this.spacing.set(4f)
            this.bounds.height.set(100f)
            this.margin.set(Insets(0f, 32f, 0f, 2f))
        }
        
        vbox.temporarilyDisableLayouts {
            vbox += TextLabel(binding = { Localization.getVar("mainMenu.librarySortFilter.desc").use() }, font = main.fontMainMenuThin).apply {
                this.bounds.height.set(64f)
                this.margin.set(Insets(2f, 2f, 0f, 0f))
                this.renderAlign.set(Align.topLeft)
                this.doLineWrapping.set(true)
                this.setScaleXY(0.85f)
                this.textColor.set(Color.DARK_GRAY)
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
                        this.textLabel.padding.set(this.textLabel.padding.getOrCompute().copy(left = 2f))
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
                        this.textLabel.padding.set(this.textLabel.padding.getOrCompute().copy(left = 2f))
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
                    this.textLabel.padding.set(this.textLabel.padding.getOrCompute().copy(left = 2f))
                    this.onCheckChanged = {
                        currentSettings.set(currentSettings.getOrCompute().copy(legacyOnTop = it))
                    }
                }
            }
            val sortables = Sortable.entries
            vbox += Pane().apply {
                this.bounds.height.set(32f * (sortables.size / 3 + 1))
                val toggleGroup = ToggleGroup()
                sortables.forEachIndexed { index, sortable -> 
                    this += RadioButton(binding = {Localization.getVar(sortable.nameKey).use()}, font = main.fontMainMenuThin).apply { 
                        this.bounds.height.set(32f)
                        this.bindWidthToParent(multiplier = 0.333f, adjust = -4f)
                        this.bounds.x.bind { (parent.use()?.bounds?.width?.use() ?: 0f) * ((index % 3) / 3f) }
                        this.bounds.y.set(32f * (index / 3))
                        this.margin.set(Insets(0f, 0f, 0f, 4f))
                        this.textLabel.padding.set(this.textLabel.padding.getOrCompute().copy(left = 1f))
                        this.color.set(Color.DARK_GRAY)
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
            
            // ----------------------------------------------------------------------------------------------------

            // Filter half
            vbox += HBox().apply {
                this.bounds.height.set(64f + 8f)
                this.spacing.set(4f)
                this.margin.set(Insets(12f, 0f, 0f, 0f))
                this += TextLabel(binding = { Localization.getVar("mainMenu.librarySortFilter.filter").use() }, font = main.fontMainMenuHeading).apply {
                    this.bindWidthToParent(multiplier = 0.3f)
                }
            }
            fun <T : Filter> createFilter(getter: (LibrarySortFilter) -> T, setter: (LibrarySortFilter, T) -> LibrarySortFilter): UIElement {
                val oldFilter: T = getter(oldSettings)
                return VBox().apply {
                    this.margin.set(Insets(0f, 10f, 0f, 0f))
                    this.spacing.set(4f)
                    
                    val checkbox = CheckBox(binding = { Localization.getVar(oldFilter.filterable.nameKey).use() }, font = main.fontMainMenuThin).apply {
                        this.bounds.height.set(32f)
                        this.bindWidthToParent(multiplier = 0.45f)
                        this.selectedState.set(oldFilter.enabled)
                        this.textLabel.padding.set(this.textLabel.padding.getOrCompute().copy(left = 2f))
                        this.color.bind { 
                            if (checkedState.use()) Color(0f, 0.5f, 0f, 1f) else Color.DARK_GRAY.cpy()
                        }
                        this.textLabel.font.bind {
                            if (checkedState.use()) main.fontMainMenuMain else main.fontMainMenuThin
                        }
                        
                        this.onCheckChanged = { 
                            val lsf = currentSettings.getOrCompute()
                            val newFilter = getter(lsf).copyBase(it)
                            @Suppress("UNCHECKED_CAST")
                            currentSettings.set(setter(lsf, newFilter as T))
                        }
                    }
                    this += checkbox
                    fun UIElement.createFilterPane(filter: T) {
                        when (filter) {
                            is FilterOnStringList -> @Suppress("UNCHECKED_CAST") {
                                val modernList = fullLevelList.map { it.levelEntry }.filterIsInstance<LevelEntry.Modern>()
                                val list: List<String> = listOf("") + (when (filter.filterable) {
                                    Filterable.LEVEL_CREATOR -> modernList.map { it.levelMetadata.levelCreator }
                                    Filterable.SONG_NAME -> modernList.map { it.levelMetadata.songName }
                                    Filterable.SONG_ARTIST -> modernList.map { it.levelMetadata.songArtist }
                                    Filterable.ALBUM_NAME -> modernList.map { it.levelMetadata.albumName }
                                    Filterable.GENRE -> modernList.map { it.levelMetadata.genre }
                                    else -> emptyList()
                                }.sortedBy { it.lowercase(Locale.ROOT) }.toSet())
                                run {
                                    val lsf = currentSettings.getOrCompute()
                                    val newFilter = getter(lsf) as FilterOnStringList
                                    currentSettings.set(setter(lsf, newFilter.copy(list = list) as T))
                                }
                                this += ComboBox(list, filter.filterOn, font = main.fontMainMenuRodin).apply { 
                                    this.bounds.height.set(32f)
                                    this.bindWidthToParent(multiplier = 0.75f)
                                    
                                    this.onItemSelected = { item ->
                                        val lsf = currentSettings.getOrCompute()
                                        val newFilter = getter(lsf) as FilterOnStringList
                                        currentSettings.set(setter(lsf, newFilter.copy(filterOn = item) as T))
                                        if (!checkbox.checkedState.get()) {
                                            checkbox.checkedState.set(true)
                                        }
                                    }
                                }
                            }
                            is FilterInteger -> {
                                this += HBox().apply {
                                    this.bounds.height.set(32f)
                                    this.spacing.set(8f)

                                    this += ComboBox(FilterInteger.Op.entries, filter.op, font = main.fontMainMenuMain).apply {
                                        this.bounds.height.set(32f)
                                        this.bounds.width.set(64f)
                                        this.itemStringConverter.set { it.symbol }

                                        @Suppress("UNCHECKED_CAST")
                                        this.onItemSelected = { item ->
                                            val lsf = currentSettings.getOrCompute()
                                            val newFilter = getter(lsf) as FilterInteger
                                            currentSettings.set(setter(lsf, newFilter.copy(op = item) as T))
                                            
                                            if (!checkbox.checkedState.get()) {
                                                checkbox.checkedState.set(true)
                                            }
                                        }
                                    }
                                    if (filter.filterable == Filterable.DIFFICULTY) {
                                        this += ComboBox(LevelMetadata.LIMIT_DIFFICULTY.toList(), filter.right, font = main.fontMainMenuMain).apply {
                                            this.bounds.height.set(32f)
                                            this.bounds.width.set(150f)
                                            this.itemStringConverter.set {
                                                if (it <= 0) Localization.getValue("editor.dialog.levelMetadata.noDifficulty") else "$it"
                                            }

                                            @Suppress("UNCHECKED_CAST")
                                            this.onItemSelected = { item ->
                                                val lsf = currentSettings.getOrCompute()
                                                val newFilter = getter(lsf) as FilterInteger
                                                currentSettings.set(setter(lsf, newFilter.copy(right = item) as T))
                                                
                                                if (!checkbox.checkedState.get()) {
                                                    checkbox.checkedState.set(true)
                                                }
                                            }
                                        }
                                    } else if (filter.filterable == Filterable.ALBUM_YEAR) {
                                        this += RectElement(Color.BLACK).apply {
                                            this.bounds.width.set(75f)
                                            this.padding.set(Insets(1f, 1f, 2f, 2f))
                                            this.border.set(Insets(1f))
                                            this.borderStyle.set(SolidBorder(Color.WHITE))
                                            this += TextField(main.fontMainMenuRodin).apply {
                                                this.textColor.set(Color(1f, 1f, 1f, 1f))
                                                this.characterLimit.set(4) // XXXX
                                                this.inputFilter.set {
                                                    it in '0'..'9'
                                                }
                                                this.text.set((oldFilter as FilterInteger).right.takeUnless { it == 0 }?.toString() ?: "")
                                                this.text.addListener { t ->
                                                    val newText = t.getOrCompute()
                                                    if (this.hasFocus.get()) {
                                                        val newYear: Int = newText.toIntOrNull()?.takeIf { it in LevelMetadata.LIMIT_YEAR } ?: 0
                                                        
                                                        val lsf = currentSettings.getOrCompute()
                                                        val newFilter = getter(lsf) as FilterInteger
                                                        @Suppress("UNCHECKED_CAST")
                                                        currentSettings.set(setter(lsf, newFilter.copy(right = newYear) as T))
                                                        
                                                        this.text.set(newYear.takeUnless { it == 0 }?.toString() ?: "")
                                                        
                                                        if (!checkbox.checkedState.get()) {
                                                            checkbox.checkedState.set(true)
                                                        }
                                                    }
                                                }
                                                this.setOnRightClick {
                                                    requestFocus()
                                                    text.set("")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is FilterBoolean -> {
                                this += ComboBox(listOf(true, false), filter.targetValue, font = main.fontMainMenuMain).apply {
                                    this.bounds.height.set(32f)
                                    this.bounds.width.set(150f)
                                    this.itemStringConverter.set {
                                        if (it) Localization.getValue("mainMenu.librarySortFilter.filter.boolean.true") else Localization.getValue("mainMenu.librarySortFilter.filter.boolean.false")
                                    }

                                    @Suppress("UNCHECKED_CAST")
                                    this.onItemSelected = { item ->
                                        val lsf = currentSettings.getOrCompute()
                                        val newFilter = getter(lsf) as FilterBoolean
                                        currentSettings.set(setter(lsf, newFilter.copy(targetValue = item) as T))

                                        if (!checkbox.checkedState.get()) {
                                            checkbox.checkedState.set(true)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    this += VBox().apply { 
//                        this.opacity.bind { if (checkbox.checkedState.useB()) 1f else 0.25f }
                        this.margin.set(Insets(0f, 0f, 32f, 0f))
                        this.createFilterPane(oldFilter)
                        this.sizeHeightToChildren(32f)
                    }
                    
                    this.sizeHeightToChildren(32f)
                }
            }
            vbox += createFilter({ it.filterLevelCreator }, { lsf, filter -> lsf.copy(filterLevelCreator = filter) })
            vbox += createFilter({ it.filterSongName }, { lsf, filter -> lsf.copy(filterSongName = filter) })
            vbox += createFilter({ it.filterSongArtist }, { lsf, filter -> lsf.copy(filterSongArtist = filter) })
            vbox += createFilter({ it.filterAlbumName }, { lsf, filter -> lsf.copy(filterAlbumName = filter) })
            vbox += createFilter({ it.filterAlbumYear }, { lsf, filter -> lsf.copy(filterAlbumYear = filter) })
            vbox += createFilter({ it.filterGenre }, { lsf, filter -> lsf.copy(filterGenre = filter) })
            vbox += createFilter({ it.filterDifficulty }, { lsf, filter -> lsf.copy(filterDifficulty = filter) })
            vbox += createFilter({ it.filterFlashingLightsWarning }, { lsf, filter -> lsf.copy(filterFlashingLightsWarning = filter) })
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
                this.bounds.width.set(300f)
                this.setOnAction {
                    menuCol.popLastMenu(instant = true, playSound = true, backOut = false)
                    library.sortFilter.set(currentSettings.getOrCompute())
                    library.filterAndSortLevelList()
                }
            }
            leftBottomHbox += createSmallButton(binding = { Localization.getVar("mainMenu.librarySortFilter.resetDefault").use() }).apply {
                this.bounds.width.set(220f)
                this.text
                this.setOnAction {
                    menuCol.popLastMenu(instant = true, playSound = false, backOut = false)
                    library.goToFilterMenu(LibrarySortFilter.DEFAULT, playSound = true)
                }
            }
        }
    }
    
}