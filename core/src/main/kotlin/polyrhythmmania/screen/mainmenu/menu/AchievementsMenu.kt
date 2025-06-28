package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.binding.*
import paintbox.font.Markup
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.ComboBox
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.DecimalFormats
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.PRManiaLocalePicker
import polyrhythmmania.achievements.Achievement
import polyrhythmmania.achievements.AchievementCategory
import polyrhythmmania.achievements.Achievements
import polyrhythmmania.achievements.Fulfillment
import polyrhythmmania.achievements.ui.Toast
import polyrhythmmania.ui.PRManiaSkins
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class AchievementsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    companion object {
        private val SHOW_IDS_WHEN_DEBUG: Boolean = PRMania.isDevVersion
        private val SHOW_ICONS_WHEN_DEBUG: Boolean = PRMania.isDevVersion
    }
    
    private sealed class ViewType {
        data object AllByCategory : ViewType()
        data object AllTogether : ViewType()
        class Category(val category: AchievementCategory) : ViewType()
    }
    
    private enum class SortType(val localization: String) {
        DEFAULT("mainMenu.achievements.sort.default"),
        NAME_ASC("mainMenu.achievements.sort.name.asc"),
        NAME_DESC("mainMenu.achievements.sort.name.desc"),
        INCOMPLETE("mainMenu.achievements.sort.incomplete"),
        UNLOCKED_LATEST("mainMenu.achievements.sort.unlocked_latest"),
        UNLOCKED_EARLIEST("mainMenu.achievements.sort.unlocked_earliest"),
    }

    private val totalProgressLabel: TextLabel
//    private val panePerCategory: Map<AchievementCategory, UIElement>
    
    init {
        this.setSize(MMMenu.WIDTH_LARGE)
        this.titleText.bind { Localization.getVar("mainMenu.achievements.title").use() }
        this.contentPane.bounds.height.set(520f)
        this.showLogo.set(false)

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)
            
            // Give vbar a min length
            this.minThumbSize.set(50f)
            this.vBar.unitIncrement.set(40f)
            this.vBar.blockIncrement.set(90f)
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
            this.spacing.set(4f)
            this.margin.set(Insets(0f, 0f, 0f, 16f))
        }
        
        val percentFormat = DecimalFormats["#0.#"]
        val headingMarkup = Markup.createWithSingleFont(main.fontMainMenuHeading)
        val descMarkup = Markup.createWithBoldItalic(font, null, main.fontMainMenuItalic, main.fontMainMenuItalic, additionalMappings = mapOf(
                "prmania_icons" to main.fontIcons,
                "rodin" to main.fontMainMenuRodin,
                "thin" to main.fontMainMenuThin,
        ))
        val statProgressColor = "9FD677"
        val completedTextureReg = TextureRegion(AssetRegistry.get<Texture>("achievements_completed_mark"))
//        panePerCategory = linkedMapOf()
        
        totalProgressLabel = TextLabel(binding = {
            Localization.getVar("achievement.totalProgress", Var {
                val map = Achievements.fulfillmentMap.use()
                val numGotten = map.size
                val numTotal = Achievements.achievementIDMap.size
                val percentageWhole = (100f * numGotten / numTotal).coerceIn(0f, 100f)
                listOf(percentFormat.format(percentageWhole), numGotten, numTotal)
            }).use()
        }, font = main.fontMainMenuHeading).apply {
            this.textColor.set(CreditsMenu.HEADING_TEXT_COLOR)
            this.bounds.height.set(48f)
            this.padding.set(Insets(8f))
            this.setScaleXY(0.75f)
            this.renderAlign.set(Align.center)
        }
        
        fun createAchievementElement(achievement: Achievement, compact: Boolean): UIElement {
            val achievementEarned = BooleanVar { Achievements.fulfillmentMap.use()[achievement] != null }
            val entire = ActionablePane().apply {
                this.setOnAltAction {
                    if (Paintbox.debugMode.get() && PRMania.isDevVersion) {
                        main.achievementsUIOverlay.enqueueToast(Toast(achievement, Achievements.fulfillmentMap.getOrCompute()[achievement] ?: Fulfillment(Instant.now())))
                    }
                }

                this += ImageIcon(null, renderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO).apply {
                    Anchor.TopLeft.configure(this)
                    this.bindWidthToSelfHeight()
                    this.textureRegion.bind {
                        val iconID = if (achievementEarned.use() || (SHOW_ICONS_WHEN_DEBUG && Paintbox.debugMode.use())) achievement.getIconID() else "locked"
                        TextureRegion(AssetRegistry.get<PackedSheet>("achievements_icon")[iconID])
                    }
                }
                this += ImageIcon(completedTextureReg, renderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO).apply {
                    Anchor.TopRight.configure(this)
                    this.bindWidthToSelfHeight()
                    this.visible.bind { achievementEarned.use() }
                    this.tooltipElement.set(createTooltip(Localization.getVar("achievement.unlockedTooltip", Var {
                        listOf(ZonedDateTime.ofInstant(Achievements.fulfillmentMap.use()[achievement]?.gotAt ?: Instant.EPOCH, ZoneId.systemDefault()).format(DateTimeFormatter.RFC_1123_DATE_TIME))
                    })))
                }
                this += Pane().apply {
                    Anchor.TopCentre.configure(this)
                    this.bindWidthToParent(adjustBinding = {
                        -(((parent.use()?.contentZone?.height?.use() ?: 0f) + 8f) * 2)
                    })

                    val statProgress: ReadOnlyVar<String> = if (achievement is Achievement.StatTriggered && achievement.showProgress) {
                        val formatter = achievement.stat.formatter
                        val formattedValue = formatter.format(achievement.stat.value)
                        val formattedThreshold = formatter.format(IntVar(achievement.threshold))
                        Localization.getVar("achievement.statProgress", Var {
                            listOf(formattedValue.use(), formattedThreshold.use())
                        })
                    } else Var("")
                    val achDesc = Var.bind {
                        val stillHidden = achievement.isHidden && !achievementEarned.use()
                        if (stillHidden) {
                            "[i]${Localization.getVar("achievement.hidden.desc").use()}[]"
                        } else "${if (achievement.isHidden) "${Localization.getVar("achievement.hidden.desc").use()} " else ""}${achievement.getLocalizedDesc().use()}"
                    }
                    this += TextLabel(binding = {
                        val stillHidden = achievement.isHidden && !achievementEarned.use()
                        val desc = achDesc.use()
                        val statProgressText = if (!stillHidden) statProgress.use() else ""
                        "[color=#${achievement.rank.color.toString()} scale=1.0 lineheight=0.75]${achievement.getLocalizedName().use()} [color=#$statProgressColor scale=0.75] ${statProgressText}[] ${if (SHOW_IDS_WHEN_DEBUG && Paintbox.debugMode.use()) "[i color=GRAY scale=0.75]${achievement.id}[]" else ""}${if (!compact) "\n" +
                                "[][color=LIGHT_GRAY scale=0.75 lineheight=0.9]${desc}[]" else ""}"
                    }).apply {
                        Anchor.TopLeft.configure(this)
                        this.setScaleXY(1f)
                        this.renderAlign.set(Align.left)
                        this.padding.set(Insets.ZERO)
                        this.markup.set(descMarkup)
                        this.doLineWrapping.set(true)
                        if (compact) {
                            this.tooltipElement.set(createTooltip(achDesc).apply { 
                                this.doLineWrapping.set(true)
                                this.maxWidth.set(800f)
                            })
                        }
                    }
                }
            }
            return RectElement(Color.DARK_GRAY).apply {
                this.bounds.height.set(if (!compact) 72f else 36f)
                this.padding.set(Insets(6f))
                this += entire
            }
        }

        val achievementPanes: Map<Achievement, UIElement> by lazy { Achievements.achievementIDMap.values.associateWith { createAchievementElement(it, false) } }
        val achievementPanesCompact: Map<Achievement, UIElement> by lazy { Achievements.achievementIDMap.values.associateWith { createAchievementElement(it, true) } }
        val categoryHeadings: Map<AchievementCategory, UIElement> by lazy { 
            AchievementCategory.entries.associateWith { category ->
                val achievementsInCategory = Achievements.achievementIDMap.values.filter { it.category == category }
                TextLabel(binding = {
                    Localization.getVar("achievement.categoryProgress", Var {
                        val map = Achievements.fulfillmentMap.use()
                        val numGotten = map.keys.count { it.category == category }
                        val numTotal = achievementsInCategory.size
                        val percentageWhole = (100f * numGotten / numTotal).coerceIn(0f, 100f)
                        listOf(Localization.getVar(category.toLocalizationID()).use(), percentFormat.format(percentageWhole), numGotten, numTotal)
                    }).use()
                }).apply {
                    this.textColor.set(Color().grey(0.35f))
                    this.bounds.height.set(56f)
                    this.padding.set(Insets(16f, 8f, 0f, 32f))
                    this.renderAlign.set(Align.bottomLeft)
                    this.setScaleXY(0.75f)
                    this.markup.set(headingMarkup)
                }
            }
        }
        
        val viewingCategory = Var<ViewType>(ViewType.AllByCategory)
        val currentSort = Var<SortType>(SortType.DEFAULT)
        val compactMode = BooleanVar(false)
        
        fun updateCategory() {
            vbox.children.getOrCompute().forEach(vbox::removeChild)
            vbox.temporarilyDisableLayouts {
                vbox += totalProgressLabel

                val achievementUIElements = if (compactMode.get()) achievementPanesCompact else achievementPanes
                val cat = viewingCategory.getOrCompute()
                val sort = currentSort.getOrCompute()
                val isCategorical = cat != ViewType.AllTogether
                
                val filteredAchievements: MutableList<Achievement> = when (cat) {
                    ViewType.AllByCategory, ViewType.AllTogether -> {
                        Achievements.achievementIDMap.values
                    }
                    is ViewType.Category -> {
                        Achievements.achievementIDMap.values.filter { a -> a.category == cat.category }
                    }
                }.toMutableList()

                val fulfillments = Achievements.fulfillmentMap.getOrCompute()
                when (sort) {
                    SortType.DEFAULT -> {
                        // Do nothing
                    }
                    SortType.NAME_ASC -> filteredAchievements.sortBy {
                        it.getLocalizedName().getOrCompute().lowercase(Locale.ROOT)
                    }
                    SortType.NAME_DESC -> filteredAchievements.sortByDescending {
                        it.getLocalizedName().getOrCompute().lowercase(Locale.ROOT)
                    }
                    SortType.INCOMPLETE -> filteredAchievements.sortBy {
                        // Booleans sorted by false first. We want null fulfillments first, so invert the == null condition
                        fulfillments[it] != null
                    }
                    SortType.UNLOCKED_LATEST, SortType.UNLOCKED_EARLIEST -> {
                        val unlocked = filteredAchievements.filter { fulfillments[it] != null }.toMutableList()
                        val locked = filteredAchievements.filter { fulfillments[it] == null }
                        
                        unlocked.sortBy { fulfillments.getValue(it).gotAt } // Earliest
                        if (sort == SortType.UNLOCKED_LATEST) {
                            unlocked.reverse() // Now latest
                        }
                        
                        filteredAchievements.clear()
                        filteredAchievements.addAll(unlocked)
                        filteredAchievements.addAll(locked)
                    }
                }
                
                if (isCategorical) {
                    AchievementCategory.entries.forEach { category ->
                        val achInCat = filteredAchievements.filter { it.category == category }
                        if (achInCat.isNotEmpty()) {
                            vbox += categoryHeadings.getValue(category)
                            achInCat.forEach { ach ->
                                vbox += achievementUIElements.getValue(ach)
                            }
                        }
                    }
                } else {
                    filteredAchievements.forEach { ach ->
                        vbox += achievementUIElements.getValue(ach)
                    }
                }
            }
            
            vbox.sizeHeightToChildren(100f)
            scrollPane.setContent(vbox)
        }
        
        updateCategory()
        val updateCategoryTrigger = VarChangedListener<Any?> {
            updateCategory()
        } 
        viewingCategory.addListener(updateCategoryTrigger)
        currentSort.addListener(updateCategoryTrigger)
        PRManiaLocalePicker.currentLocale.addListener(updateCategoryTrigger)
        compactMode.addListener(updateCategoryTrigger)
        
        
        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }

            hbox += ImageIcon(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["help"])).apply {
                Anchor.BottomRight.configure(this)
                this.padding.set(Insets(2f))
                this.bounds.width.set(36f)
                this.tint.set(Color.BLACK)
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.achievements.help.tooltip")))
            }

            hbox += TextLabel(Localization.getVar("mainMenu.achievements.filter"), font = main.fontMainMenuMain).apply {
                this.bounds.width.set(80f)
                this.renderAlign.set(Align.right)
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
                this.setScaleXY(0.75f)
                this.margin.set(Insets(0f, 0f, 8f, 0f))
            }
            val viewTypeList = listOf(ViewType.AllByCategory, ViewType.AllTogether) + AchievementCategory.entries.map(ViewType::Category)
            hbox += ComboBox<ViewType>(viewTypeList, viewingCategory.getOrCompute(), font = font).apply {
                this.bounds.width.set(220f)
                this.setScaleXY(0.75f)
                this.itemStringConverter.bind {
                    StringConverter { view ->
                        when (view) {
                            ViewType.AllByCategory -> Localization.getVar("mainMenu.achievements.viewAll.category").use()
                            ViewType.AllTogether -> Localization.getVar("mainMenu.achievements.viewAll.together").use()
                            is ViewType.Category -> Localization.getVar(view.category.toLocalizationID()).use()
                        }
                    }
                }
                this.onItemSelected = { newItem ->
                    viewingCategory.set(newItem)
                }
            }
            
            hbox += TextLabel(Localization.getVar("mainMenu.achievements.sort"), font = main.fontMainMenuMain).apply {
                this.bounds.width.set(80f)
                this.renderAlign.set(Align.right)
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
                this.setScaleXY(0.75f)
                this.margin.set(Insets(0f, 0f, 8f, 0f))
            }
            val sortTypeList = SortType.entries
            hbox += ComboBox<SortType>(sortTypeList, currentSort.getOrCompute(), font = font).apply {
                this.bounds.width.set(180f)
                this.setScaleXY(0.75f)
                this.itemStringConverter.bind {
                    StringConverter { sort ->
                        Localization.getVar(sort.localization).use()
                    }
                }
                this.onItemSelected = { newItem ->
                    currentSort.set(newItem)
                }
            }
            
            hbox += createSmallButton { "" }.apply {
                val borderSize = Insets(4f)
                this.bindWidthToSelfHeight()
                this.padding.set(Insets.ZERO)
                this += RectElement(Color(1f, 1f, 1f, 0f)).apply { // Indent effect
                    this.borderStyle.set(SolidBorder().apply {
                        this.color.set(Color.valueOf("2AE030").apply { 
                            this.a = 0.9f
                        })
                    })
                    this.border.bind {
                        if (compactMode.use()) borderSize else Insets.ZERO
                    }
                }
                
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.achievements.compactMode.tooltip")))
                this += RectElement(binding = {
                    if (compactMode.use()) Color(0f, 0f, 0f, 0.4f) else Color.CLEAR
                })
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("achievements_icon")["compact_mode"])).apply {
                    this.margin.set(borderSize)
                    this.tint.bind {
                        if (compactMode.use()) Color.WHITE else Color().grey(0.1f, 1f)
                    }
                }
                this.setOnAction { 
                    compactMode.invert()
                }
            }
        }
    }

}