package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.BooleanVar
import paintbox.binding.IntVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.Markup
import paintbox.font.TextRun
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageIcon
import paintbox.ui.ImageRenderingMode
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.DecimalFormats
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.achievements.Achievement
import polyrhythmmania.achievements.AchievementCategory
import polyrhythmmania.achievements.Achievements
import polyrhythmmania.achievements.AchievementsL10N
import polyrhythmmania.ui.PRManiaSkins
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt


class AchievementsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    init {
        this.setSize(MMMenu.WIDTH_LARGE)
        this.titleText.bind { Localization.getVar("mainMenu.achievements.title").use() }
        this.contentPane.bounds.height.set(520f)
        this.showLogo.set(false)

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)
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
            this.bounds.height.set(300f)
            this.spacing.set(4f)
            this.margin.set(Insets(0f, 0f, 0f, 16f))
        }


        vbox.temporarilyDisableLayouts {
            val percentFormat = DecimalFormats["#0.#"]
            
            vbox += TextLabel(binding = { 
                AchievementsL10N.getVar("achievement.progress", Var {
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
            
            val headingMarkup = Markup(mapOf(), TextRun(main.fontMainMenuHeading, ""), Markup.FontStyles.ALL_USING_DEFAULT_FONT)
            val descMarkup = Markup(mapOf(
                    Markup.FONT_NAME_ITALIC to main.fontMainMenuItalic,
                    Markup.FONT_NAME_BOLDITALIC to main.fontMainMenuItalic,
                    "prmania_icons" to main.fontIcons,
                    "rodin" to main.fontMainMenuRodin,
                    "thin" to main.fontMainMenuThin
            ), TextRun(font, ""), Markup.FontStyles.ALL_USING_BOLD_ITALIC)
            val statProgressColor = "9FD677"
            val completedTextureReg = TextureRegion(AssetRegistry.get<Texture>("achievements_completed_mark"))
            AchievementCategory.VALUES.forEach { category ->
                val achievementsInCategory = Achievements.achievementIDMap.values.filter { it.category == category }
                
                vbox += TextLabel(binding = { 
                    AchievementsL10N.getVar("achievement.categoryProgress", Var {
                        val map = Achievements.fulfillmentMap.use()
                        val numGotten = map.keys.count { it.category == category }
                        val numTotal = achievementsInCategory.size
                        val percentageWhole = (100f * numGotten / numTotal).coerceIn(0f, 100f)
                        listOf(AchievementsL10N.getVar("achievement.category.${category.id}").use(), percentFormat.format(percentageWhole), numGotten, numTotal)
                    }).use() 
                }).apply { 
                    this.textColor.set(Color().grey(0.35f))
                    this.bounds.height.set(56f)
                    this.padding.set(Insets(16f, 8f, 0f, 32f))
                    this.renderAlign.set(Align.bottomLeft)
                    this.setScaleXY(0.75f)
                    this.markup.set(headingMarkup)
                }
                
                achievementsInCategory.forEach { achievement ->
                    val achievementEarned = BooleanVar { Achievements.fulfillmentMap.use()[achievement] != null }    
                    val entire = Pane().apply {
                        this += ImageIcon(null, renderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO).apply {
                            Anchor.TopLeft.configure(this)
                            this.bindWidthToSelfHeight()
                            this.textureRegion.set(TextureRegion(AssetRegistry.get<Texture>("tileset_missing_tex"))) // FIXME
                        }
                        this += ImageIcon(completedTextureReg, renderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO).apply {
                            Anchor.TopRight.configure(this)
                            this.bindWidthToSelfHeight()
                            this.visible.bind { achievementEarned.use() }
                            this.tooltipElement.set(createTooltip(AchievementsL10N.getVar("achievement.unlockedTooltip", Var {
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
                                AchievementsL10N.getVar("achievement.statProgress", Var {
                                    listOf(formattedValue.use(), formattedThreshold.use())
                                })
                            } else Var("")
                            this += TextLabel(binding = {
                                if (achievement.isHidden && !achievementEarned.use()) {
                                    "[i color=#9f9f9f]${AchievementsL10N.getVar("achievement.hidden.name").use()}[]"
                                } else {
                                    "[color=#${achievement.quality.color.toString()} scale=1.0 lineheight=0.75]${achievement.getLocalizedName().use()} [color=#$statProgressColor scale=0.75] ${statProgress.use()}[]\n[][color=LIGHT_GRAY scale=0.75 lineheight=0.9]${if (achievement.isHidden) "${AchievementsL10N.getVar("achievement.hidden.desc").use()} " else ""}${achievement.getLocalizedDesc().use()}[]"
                                }
                            }).apply {
                                Anchor.TopLeft.configure(this)
                                this.setScaleXY(1f)
                                this.renderAlign.set(Align.left)
                                this.padding.set(Insets.ZERO)
                                this.markup.set(descMarkup)
                                this.doLineWrapping.set(true)
                            }
                        }
                    }
                    vbox += RectElement(Color.DARK_GRAY).apply { 
                        this.bounds.height.set(72f)
                        this.padding.set(Insets(6f))
                        this += entire
                    }
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
        }
    }

}